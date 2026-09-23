package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.enums.RespuestaCatalogoEnum;
import com.proyecto.servicios.model.producto.GetProductListXmlResponse;
import com.proyecto.servicios.model.producto.MensajeRespuestaDto;
import com.proyecto.servicios.model.producto.ProductoItemDto;
import com.proyecto.servicios.model.producto.ProductoResponseDto;
import com.proyecto.servicios.model.producto.ProductosWrapperDto;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import com.proyecto.servicios.util.ProductoXmlParser;
import feign.FeignException;
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

// Pruebas unitarias para ProductoServiceImpl con MongoDB y Enums
@ExtendWith(MockitoExtension.class)
public class ProductoServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

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
    }

    // Valida el flujo del Cron Task: consume XML y persiste en MongoDB
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
        assertEquals(RespuestaCatalogoEnum.SINCRONIZACION_EXITOSA.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.SINCRONIZACION_EXITOSA.getMensaje(), resultado.getMensaje());
        assertEquals(1, resultado.getProductos().size());
        verify(gestoPagoProductClient).getProductList("Bearer " + TOKEN_PRUEBA);
        verify(productoMongoRepository).saveAll(anyList());
    }

    // Valida la consulta al servicio que lee desde MongoDB y retorna JSON
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
        assertEquals(RespuestaCatalogoEnum.EXITO.getMensaje(), resultado.getMensaje());
        assertEquals(1, resultado.getProductos().size());
        assertEquals(202, resultado.getProductos().get(0).getIdProducto());
    }

    // Valida error 401 en la sincronizacion
    @Test
    void testSincronizarProductos_ErrorAutenticacion401() {
        FeignException.Unauthorized ex = Mockito.mock(FeignException.Unauthorized.class);
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(ex);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_AUTORIZACION.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_AUTORIZACION.getMensaje(), resultado.getMensaje());
        assertTrue(resultado.getProductos().isEmpty());
    }

    // Valida error 403 de token expirado
    @Test
    void testSincronizarProductos_ErrorTokenExpirado403() {
        FeignException.Forbidden ex = Mockito.mock(FeignException.Forbidden.class);
        when(gestoPagoProductClient.getProductList(anyString())).thenThrow(ex);

        ProductoResponseDto resultado = productoService.sincronizarProductosDesdeServicio();

        assertNotNull(resultado);
        assertEquals(RespuestaCatalogoEnum.ERROR_TOKEN_EXPIRADO.getCodigo(), resultado.getCodigo());
        assertEquals(RespuestaCatalogoEnum.ERROR_TOKEN_EXPIRADO.getMensaje(), resultado.getMensaje());
        assertTrue(resultado.getProductos().isEmpty());
    }

    // Valida timeout de red en la sincronizacion
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

    // Valida error de parseo XML
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
}
