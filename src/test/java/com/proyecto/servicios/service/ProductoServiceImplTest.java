package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.enums.RespuestaCatalogoEnum;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.producto.GetProductListXmlResponse;
import com.proyecto.servicios.model.producto.MensajeRespuestaDto;
import com.proyecto.servicios.model.producto.ProductoItemDto;
import com.proyecto.servicios.model.producto.ProductoResponseDto;
import com.proyecto.servicios.model.producto.ProductosWrapperDto;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import com.proyecto.servicios.util.ProductoXmlParser;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas unitarias completas para ProductoServiceImpl
@ExtendWith(MockitoExtension.class)
public class ProductoServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private ProductoXmlParser productoXmlParser;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private static final String TOKEN_PRUEBA = "mi_token_secreto_123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productoService, "token", TOKEN_PRUEBA);
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(productoService, "password", "distribuidor-pass");
        ReflectionTestUtils.setField(productoService, "gestoPagoAuthClient", gestoPagoAuthClient);
    }

    @Test
    void testSincronizarProductosDesdeServicio_Exitoso() throws Exception {
        String xmlSimulado = "<RESPONSE>...</RESPONSE>";
        ProductoItemDto item = ProductoItemDto.builder()
                .idProducto(101)
                .producto("Producto Prueba")
                .precio("50.0")
                .build();

        GetProductListXmlResponse xmlResponse = new GetProductListXmlResponse();
        xmlResponse.setMensaje(new MensajeRespuestaDto("01", "Operacion realizada con exito"));
        xmlResponse.setProductosWrapper(new ProductosWrapperDto(List.of(item)));

        when(gestoPagoProductClient.getProductList(eq("Bearer " + TOKEN_PRUEBA))).thenReturn(xmlSimulado);
        when(productoXmlParser.parse(xmlSimulado)).thenReturn(xmlResponse);
        when(productoMongoRepository.findByIdProducto(101)).thenReturn(Optional.empty());

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.EXITO.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.EXITO.getMensaje(), resultado.getMensaje());
        assertEquals(1, resultado.getProductos().size());
        verify(gestoPagoProductClient).getProductList("Bearer " + TOKEN_PRUEBA);
        verify(productoMongoRepository).saveAll(anyList());
    }

    @Test
    void testObtenerListaProductos_DesdeMongoDB() {
        ProductoDocument doc = ProductoDocument.builder()
                .idProducto(202)
                .producto("Producto Guardado en Mongo")
                .precio("120.0")
                .build();

        when(productoMongoRepository.findAll()).thenReturn(List.of(doc));

        ProductoResponseDto resultado = productoService.obtenerListaProductos();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.EXITO.getCodigo(), resultado.getCodigo());
        assertEquals(1, resultado.getProductos().size());
        assertEquals(202, resultado.getProductos().get(0).getIdProducto());
    }

    @Test
    void testObtenerListaProductos_MongoVacio_SincronizaConServicio() throws Exception {
        when(productoMongoRepository.findAll()).thenReturn(Collections.emptyList());

        String xmlSimulado = "<RESPONSE>...</RESPONSE>";
        GetProductListXmlResponse xmlResponse = new GetProductListXmlResponse();
        xmlResponse.setMensaje(new MensajeRespuestaDto("01", "Exito"));
        xmlResponse.setProductosWrapper(new ProductosWrapperDto(Collections.emptyList()));

        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(xmlSimulado);
        when(productoXmlParser.parse(xmlSimulado)).thenReturn(xmlResponse);

        ProductoResponseDto resultado = productoService.obtenerListaProductos();
        assertNotNull(resultado);
    }

    @Test
    void testObtenerListaProductos_MongoFalla_RetornaErrorInterno() {
        when(productoMongoRepository.findAll()).thenThrow(new RuntimeException("Error Mongo"));

        ProductoResponseDto resultado = productoService.obtenerListaProductos();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_INTERNO.getCodigo(), resultado.getCodigo());
    }

    @Test
    void testSincronizarProductos_ErrorGenericoFeign() {
        Request request = Request.create(Request.HttpMethod.GET, "/url", new HashMap<>(), null, new RequestTemplate());
        FeignException.BadRequest ex400 = new FeignException.BadRequest("Bad Request", request, null, null);
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(ex400);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(400, resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_PROVEEDOR.getMensaje(), resultado.getMensaje());
    }

    @Test
    void testSincronizarProductos_ErrorAutenticacion401() {
        Request request = Request.create(Request.HttpMethod.GET, "/url", new HashMap<>(), null, new RequestTemplate());
        FeignException.Unauthorized ex401 = new FeignException.Unauthorized("Unauthorized", request, null, null);
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(ex401);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_AUTORIZACION.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_AUTORIZACION.getMensaje(), resultado.getMensaje());
        assertTrue(resultado.getProductos().isEmpty());
    }

    @Test
    void testSincronizarProductos_ErrorTimeout() {
        RetryableException ex = Mockito.mock(RetryableException.class);
        when(ex.getMessage()).thenReturn("Connection timed out");
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(ex);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_TIMEOUT.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_TIMEOUT.getMensaje(), resultado.getMensaje());
        assertTrue(resultado.getProductos().isEmpty());
    }

    @Test
    void testSincronizarProductos_ErrorParseoXml() throws Exception {
        String xmlInvalido = "xml_corrupto";
        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(xmlInvalido);
        when(productoXmlParser.parse(xmlInvalido)).thenThrow(new JAXBException("Error de sintaxis XML"));

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_FORMATO_XML.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_FORMATO_XML.getMensaje(), resultado.getMensaje());
        assertTrue(resultado.getProductos().isEmpty());
    }

    @Test
    void testSincronizarProductos_ErrorInesperado() {
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(new RuntimeException("Falla fatal"));

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_INTERNO.getCodigo(), resultado.getCodigo());
    }

    @Test
    void testSincronizarProductos_Forbidden403_RenuevaTokenYReintentaExitoso() throws Exception {
        Request req = Request.create(Request.HttpMethod.GET, "/url", new HashMap<>(), null, new RequestTemplate());
        FeignException.Forbidden ex403 = new FeignException.Forbidden("Forbidden", req, null, null);

        String xmlSimulado = "<RESPONSE>...</RESPONSE>";
        ProductoItemDto item = ProductoItemDto.builder()
                .idProducto(202)
                .producto("Producto Renovado")
                .precio("100.0")
                .build();
        GetProductListXmlResponse xmlResponse = new GetProductListXmlResponse();
        xmlResponse.setMensaje(new MensajeRespuestaDto("01", "Operacion exitosa"));
        xmlResponse.setProductosWrapper(new ProductosWrapperDto(List.of(item)));

        when(gestoPagoProductClient.getProductList(anyString()))
                .thenThrow(ex403)
                .thenReturn(xmlSimulado);

        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("nuevo_token_999");
        when(gestoPagoAuthClient.authenticate(any(), any(), any())).thenReturn(authResponse);
        when(productoXmlParser.parse(xmlSimulado)).thenReturn(xmlResponse);
        when(productoMongoRepository.findByIdProducto(202)).thenReturn(Optional.empty());

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.EXITO.getCodigo(), resultado.getCodigo());
        assertEquals(1, resultado.getProductos().size());
        verify(gestoPagoAuthClient).authenticate(any(), any(), any());
    }

    @Test
    void testSincronizarProductos_ConTokenYaConBearer() throws Exception {
        ReflectionTestUtils.setField(productoService, "token", "Bearer ya_tiene_bearer");
        String xmlSimulado = "<RESPONSE></RESPONSE>";
        GetProductListXmlResponse xmlResponse = new GetProductListXmlResponse();
        xmlResponse.setMensaje(new MensajeRespuestaDto("01", "Ok"));
        xmlResponse.setProductosWrapper(new ProductosWrapperDto(Collections.emptyList()));

        when(gestoPagoProductClient.getProductList("Bearer ya_tiene_bearer")).thenReturn(xmlSimulado);
        when(productoXmlParser.parse(xmlSimulado)).thenReturn(xmlResponse);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.EXITO.getCodigo(), resultado.getCodigo());
    }
}
