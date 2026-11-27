package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.enums.RespuestaCatalogoEnum;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.producto.GetProductListXmlResponse;
import com.proyecto.servicios.model.producto.ProductoItemDto;
import com.proyecto.servicios.model.producto.ProductoResponseDto;
import com.proyecto.servicios.model.producto.ProductosWrapperDto;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.util.ProductoXmlParser;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Implementación de negocio con sincronización programada y persistencia MongoDB declarativa sin 'if'
@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final GestoPagoProductClient gestoPagoProductClient;
    private final ProductoXmlParser productoXmlParser;
    private final ProductoMongoRepository productoMongoRepository;

    @Autowired(required = false)
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Autowired(required = false)
    private GestoPagoTokenService gestoPagoTokenService;

    private volatile List<ProductoItemDto> cacheProductosReales = Collections.emptyList();
    private volatile long mongoNoDisponibleHastaMs = 0L;

    @Value("${productos.api.token:}")
    private String token;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String password;

    public ProductoServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                               ProductoXmlParser productoXmlParser,
                               ProductoMongoRepository productoMongoRepository) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.productoXmlParser = productoXmlParser;
        this.productoMongoRepository = productoMongoRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void precargarCatalogoAlIniciar() {
        CompletableFuture.runAsync(() -> {
            try {
                ProductoResponseDto inicial = obtenerListaProductos();
                log.info("Catálogo inicial de GestoPago disponible: {} productos reales cargados",
                        inicial.getProductos() != null ? inicial.getProductos().size() : 0);
            } catch (Exception ex) {
                log.warn("Advertencia al precargar catálogo inicial de GestoPago: {}", ex.getMessage());
            }
        });
    }

    @Override
    @Scheduled(cron = "${productos.cron:0 0 6 * * ?}")
    public ProductoResponseDto sincronizarProductosDesdeServicio() {
        log.info("Iniciando sincronizacion de catalogo de productos reales con GestoPago");
        ProductoResponseDto responseDto = new ProductoResponseDto();

        try {
            String tokenEfectivo = resolverTokenEfectivo();
            String authHeader = formatearBearerToken(tokenEfectivo);
            String xmlResponse = gestoPagoProductClient.getProductList(authHeader);
            GetProductListXmlResponse parsedXml = productoXmlParser.parse(xmlResponse);
            if (parsedXml != null && parsedXml.getMensaje() != null
                    && parsedXml.getMensaje().getCodigo() != null
                    && !"01".equals(parsedXml.getMensaje().getCodigo().trim())) {
                log.warn("GestoPago getProductList respondió código {} ({})",
                        parsedXml.getMensaje().getCodigo(), parsedXml.getMensaje().getTexto());
                asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_PROVEEDOR);
                return responseDto;
            }
            List<ProductoItemDto> items = extraerProductos(parsedXml);

            actualizarCacheReal(items);
            guardarEnMongoTolerante(items);
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
            responseDto.setProductos(items);

            log.info("Sincronizacion completada con exito desde GestoPago. Total registros reales: {}", items.size());
            return responseDto;

        } catch (FeignException.Unauthorized e) {
            log.warn("Error de autorizacion (401) con GestoPago. Intentando renovar token...");
            Optional.ofNullable(renovarToken()).ifPresent(nuevoToken -> {
                try {
                    String xmlResponse = gestoPagoProductClient.getProductList(formatearBearerToken(nuevoToken));
                    GetProductListXmlResponse parsedXml = productoXmlParser.parse(xmlResponse);
                    List<ProductoItemDto> items = extraerProductos(parsedXml);
                    actualizarCacheReal(items);
                    guardarEnMongoTolerante(items);
                    asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
                    responseDto.setProductos(items);
                    log.info("Sincronizacion exitosa tras renovacion en 401. Total: {}", items.size());
                } catch (Exception ex) {
                    log.error("Fallo la llamada tras renovar token en 401: {}", ex.getMessage());
                }
            });
            if (responseDto.getProductos() == null || responseDto.getProductos().isEmpty()) {
                asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_AUTORIZACION);
            }

        } catch (FeignException.Forbidden e) {
            log.warn("Token no valido o expirado (403). Renovando token automaticamente con GestoPago...");
            Optional.ofNullable(renovarToken()).ifPresent(nuevoToken -> {
                try {
                    String xmlResponse = gestoPagoProductClient.getProductList(formatearBearerToken(nuevoToken));
                    GetProductListXmlResponse parsedXml = productoXmlParser.parse(xmlResponse);
                    List<ProductoItemDto> items = extraerProductos(parsedXml);
                    actualizarCacheReal(items);
                    guardarEnMongoTolerante(items);
                    asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
                    responseDto.setProductos(items);
                    log.info("Sincronizacion exitosa tras auto-renovacion de token. Total: {}", items.size());
                } catch (Exception ex) {
                    log.error("Fallo la llamada tras renovar token: {}", ex.getMessage());
                }
            });
            Optional.ofNullable(responseDto.getProductos())
                    .filter(Predicate.not(List::isEmpty))
                    .ifPresentOrElse(p -> {}, () -> asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_TOKEN_EXPIRADO));

        } catch (RetryableException e) {
            log.error("Timeout de conexion al sincronizar catalogo: {}", e.getMessage());
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_TIMEOUT);

        } catch (FeignException e) {
            log.error("Respuesta no exitosa del proveedor externo. Codigo HTTP: {}", e.status());
            responseDto.setCodigo(e.status() > 0 ? e.status() : RespuestaCatalogoEnum.ERROR_PROVEEDOR.getCodigo());
            responseDto.setMensaje(RespuestaCatalogoEnum.ERROR_PROVEEDOR.getMensaje());
            responseDto.setProductos(Collections.emptyList());

        } catch (JAXBException e) {
            log.error("Error al procesar el formato XML recibido de GestoPago: {}", e.getMessage());
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_FORMATO_XML);

        } catch (Exception e) {
            log.error("Error inesperado en sincronizacion de productos: {}", e.getMessage());
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_INTERNO);
        }

        return responseDto;
    }

    @Override
    public ProductoResponseDto obtenerListaProductos() {
        log.info("Consultando catalogo de productos de GestoPago");
        ProductoResponseDto responseDto = new ProductoResponseDto();

        if (System.currentTimeMillis() < mongoNoDisponibleHastaMs && !cacheProductosReales.isEmpty()) {
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
            responseDto.setProductos(cacheProductosReales);
            return responseDto;
        }

        try {
            List<ProductoDocument> documentos = consultarMongoConTiempoLimite();

            return Optional.of(documentos)
                    .filter(Predicate.not(List::isEmpty))
                    .map(docs -> {
                        List<ProductoItemDto> listaDto = mapearDocumentosADto(docs);
                        actualizarCacheReal(listaDto);
                        asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
                        responseDto.setProductos(listaDto);
                        log.info("Consulta a MongoDB completada. Total productos devueltos: {}", listaDto.size());
                        return responseDto;
                    })
                    .orElseGet(() -> {
                        log.info("Coleccion de MongoDB vacia. Ejecutando sincronizacion directa con GestoPago");
                        return sincronizarProductosDesdeServicio();
                    });

        } catch (Exception e) {
            mongoNoDisponibleHastaMs = System.currentTimeMillis() + 300_000L;
            log.warn("MongoDB no disponible temporalmente ({}). Consultando catálogo real directamente desde GestoPago...", e.getMessage());
            if (!cacheProductosReales.isEmpty()) {
                asignarRespuesta(responseDto, RespuestaCatalogoEnum.EXITO);
                responseDto.setProductos(cacheProductosReales);
                return responseDto;
            }
            ProductoResponseDto respaldoReal = sincronizarProductosDesdeServicio();
            if (respaldoReal != null && respaldoReal.getProductos() != null && !respaldoReal.getProductos().isEmpty()) {
                return respaldoReal;
            }
            asignarRespuesta(responseDto, RespuestaCatalogoEnum.ERROR_INTERNO);
            return responseDto;
        }
    }

    @Override
    public Optional<ProductoItemDto> buscarProductoRealPorId(Integer idProducto) {
        if (idProducto == null) {
            return Optional.empty();
        }
        List<ProductoItemDto> catalogo = cacheProductosReales;
        if (catalogo.isEmpty()) {
            ProductoResponseDto cargado = obtenerListaProductos();
            catalogo = cargado != null && cargado.getProductos() != null ? cargado.getProductos() : Collections.emptyList();
        }
        return catalogo.stream()
                .filter(p -> p.getIdProducto() != null && p.getIdProducto().equals(idProducto))
                .findFirst();
    }

    private List<ProductoDocument> consultarMongoConTiempoLimite() throws Exception {
        if (System.currentTimeMillis() < mongoNoDisponibleHastaMs) {
            throw new IllegalStateException("Enfriamiento temporal de conexión a MongoDB activo");
        }
        return CompletableFuture.supplyAsync(productoMongoRepository::findAll)
                .get(2000, TimeUnit.MILLISECONDS);
    }

    private String resolverTokenEfectivo() {
        if (this.token != null && !this.token.trim().isEmpty()) {
            return this.token.trim();
        }
        if (gestoPagoTokenService != null) {
            Optional<String> tokenActivo = gestoPagoTokenService
                    .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .filter(GestoPagoToken::esVigente)
                    .map(GestoPagoToken::getToken)
                    .filter(Predicate.not(String::isBlank));
            if (tokenActivo.isPresent()) {
                return tokenActivo.get();
            }
        }
        String renovado = renovarToken();
        if (renovado != null && !renovado.isBlank()) {
            return renovado;
        }
        return this.token;
    }

    private void actualizarCacheReal(List<ProductoItemDto> items) {
        if (items != null && !items.isEmpty()) {
            this.cacheProductosReales = List.copyOf(items);
        }
    }

    private void guardarEnMongoTolerante(List<ProductoItemDto> items) {
        if (System.currentTimeMillis() < mongoNoDisponibleHastaMs) {
            return;
        }
        try {
            CompletableFuture.runAsync(() -> guardarEnMongo(items)).get(2500, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            mongoNoDisponibleHastaMs = System.currentTimeMillis() + 300_000L;
            log.warn("No se pudo persistir copia del catálogo en MongoDB local ({}); se mantiene catálogo real en memoria.", ex.getMessage());
        }
    }

    private String renovarToken() {
        return Optional.ofNullable(gestoPagoAuthClient)
                .map(client -> {
                    try {
                        log.info("Solicitando nuevo token de autenticacion a GestoPago");
                        return client.authenticate(idDistribuidor, codigoDispositivo, password);
                    } catch (Exception e) {
                        log.error("Error al renovar token con GestoPago: {}", e.getMessage());
                        return null;
                    }
                })
                .map(GestoPagoAuthResponse::getToken)
                .map(nuevoToken -> {
                    this.token = nuevoToken;
                    return nuevoToken;
                })
                .orElse(null);
    }

    private void guardarEnMongo(List<ProductoItemDto> items) {
        Optional.ofNullable(items)
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(lista -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    List<ProductoDocument> documentos = lista.stream()
                            .map(item -> {
                                ProductoDocument doc = productoMongoRepository
                                        .findByIdProducto(item.getIdProducto())
                                        .orElseGet(ProductoDocument::new);
                                doc.setIdProducto(item.getIdProducto());
                                doc.setIdServicio(item.getIdServicio());
                                doc.setProducto(item.getProducto());
                                doc.setServicio(item.getServicio());
                                doc.setIdCatTipoServicio(item.getIdCatTipoServicio());
                                doc.setTipoFront(item.getTipoFront());
                                doc.setHasDigitoVerificador(item.getHasDigitoVerificador());
                                doc.setTipoReferencia(item.getTipoReferencia());
                                doc.setPrecio(item.getPrecio());
                                doc.setShowAyuda(item.getShowAyuda());
                                doc.setLegend(item.getLegend());
                                doc.setFechaSincronizacion(ahora);
                                return doc;
                            })
                            .collect(Collectors.toList());
                    productoMongoRepository.saveAll(documentos);
                });
    }

    private List<ProductoItemDto> mapearDocumentosADto(List<ProductoDocument> documentos) {
        return documentos.stream()
                .map(doc -> ProductoItemDto.builder()
                        .idProducto(doc.getIdProducto())
                        .idServicio(doc.getIdServicio())
                        .producto(doc.getProducto())
                        .servicio(doc.getServicio())
                        .idCatTipoServicio(doc.getIdCatTipoServicio())
                        .tipoFront(doc.getTipoFront())
                        .hasDigitoVerificador(doc.getHasDigitoVerificador())
                        .tipoReferencia(doc.getTipoReferencia())
                        .precio(doc.getPrecio())
                        .showAyuda(doc.getShowAyuda())
                        .legend(doc.getLegend())
                        .build())
                .collect(Collectors.toList());
    }

    private void asignarRespuesta(ProductoResponseDto dto, RespuestaCatalogoEnum enumRespuesta) {
        dto.setCodigo(enumRespuesta.getCodigo());
        dto.setMensaje(enumRespuesta.getMensaje());
        dto.setProductos(Optional.ofNullable(dto.getProductos()).orElseGet(Collections::emptyList));
    }

    private String formatearBearerToken(String rawToken) {
        String trimmed = Optional.ofNullable(rawToken)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new IllegalArgumentException("El token de autorizacion no esta configurado"));

        return trimmed.startsWith("Bearer ") ? trimmed : "Bearer " + trimmed;
    }

    private List<ProductoItemDto> extraerProductos(GetProductListXmlResponse xmlResponse) {
        return Optional.ofNullable(xmlResponse)
                .map(GetProductListXmlResponse::getProductosWrapper)
                .map(ProductosWrapperDto::getProductos)
                .orElseGet(Collections::emptyList);
    }
}
