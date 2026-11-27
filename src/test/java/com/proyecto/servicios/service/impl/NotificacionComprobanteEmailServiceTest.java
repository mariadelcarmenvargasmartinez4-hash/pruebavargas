package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.PagoRechazadoProveedorException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionComprobanteEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PagoServicioRepository pagoServicioRepository;

    @Mock
    private GestoPagoSendTxClient gestoPagoSendTxClient;

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    private NotificacionComprobanteEmailService emailService;
    private PagoServicioServiceImpl pagoServicioService;

    @BeforeEach
    void setUp() {
        emailService = new NotificacionComprobanteEmailService();
        emailService.setMailSender(mailSender);
        emailService.setMailFrom("notificaciones@gestopago.test");

        pagoServicioService = new PagoServicioServiceImpl(
                cuentaRepository,
                pagoServicioRepository,
                gestoPagoSendTxClient,
                gestoPagoAuthClient,
                gestoPagoTokenService,
                aesEncryptionUtil,
                passwordEncoder
        );
        ReflectionTestUtils.setField(pagoServicioService, "claveSecretaAes", "ClaveTestSeguraDe32BytesParaPruebas!");
        ReflectionTestUtils.setField(pagoServicioService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoServicioService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(pagoServicioService, "passwordDistribuidor", TestCredentials.distribuidorPass());
        ReflectionTestUtils.setField(pagoServicioService, "productoMongoRepository", productoMongoRepository);
        pagoServicioService.setNotificacionComprobanteEmailService(emailService);
    }

    @Test
    @DisplayName("Escenarios 1, 2 y 3: Pago aprobado genera comprobante con datos reales y envía correo HTML con asunto oficial")
    void pagoAprobadoGeneraComprobanteRealYEnviaCorreo() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ClienteEntity cliente = ClienteEntity.builder()
                .nombre("María")
                .apellidoPaterno("Navarro")
                .apellidoMaterno("López")
                .correoElectronico("cliente.prueba@correo.com")
                .build();
        CuentaEntity cuenta = PagoTestDataBuilder.cuentaParaPago(new BigDecimal("1000.00"));
        cuenta.setCliente(cliente);

        ProductoDocument prod = ProductoDocument.builder()
                .idProducto(133)
                .idServicio(5)
                .producto("Recarga $10")
                .servicio("AT&T")
                .precio("10.00")
                .build();

        PagoServicioRequestDto request = PagoTestDataBuilder.unPagoRequest()
                .idServicio(5L)
                .idProducto(133L)
                .monto(new BigDecimal("10.00"))
                .upe("A1EF40DB79")
                .bandera(true)
                .telefono("5512345678")
                .build();

        GestoPagoSendTxResponse respProveedor = GestoPagoSendTxResponse.builder()
                .codigo("01")
                .mensaje("Transacción Exitosa")
                .autorizacion("100020110")
                .idTxProveedor("952209114")
                .folioTransaccion("GP-100020110")
                .build();

        when(pagoServicioRepository.findByUpe("A1EF40DB79")).thenReturn(Optional.empty());
        when(productoMongoRepository.findByIdProducto(133)).thenReturn(Optional.of(prod));
        when(cuentaRepository.findByNumeroCuentaWithLock(request.getNumCuenta())).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches(TestCredentials.testPin(), TestCredentials.testPinHash())).thenReturn(true);
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(PagoTestDataBuilder.tokenActivo()));
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENC");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIG");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class))).thenReturn(respProveedor);
        when(pagoServicioRepository.save(any(PagoServicioEntity.class))).thenAnswer(inv -> {
            PagoServicioEntity ent = inv.getArgument(0);
            if (ent.getId() == null) ent.setId(1L);
            return ent;
        });

        PagoServicioResponseDto comprobante = pagoServicioService.procesarPago(request);

        assertEquals(1L, comprobante.getId());
        assertEquals("MXN", comprobante.getMoneda());
        assertEquals("Recarga $10", comprobante.getNombreProducto());
        assertEquals("AT&T", comprobante.getNombreServicio());
        assertEquals("100020110", comprobante.getNumAutorizacion());
        assertEquals("952209114", comprobante.getIdTxProveedor());
        assertEquals("800-437-8672", comprobante.getAtencionClientes());
        assertEquals("GestoPago Gateway 2.0", comprobante.getNombreGateway());
        assertEquals("APLICADO", comprobante.getEstatus());

        // Esperar breve envío asíncrono
        verify(mailSender, timeout(2000).times(1)).send(any(MimeMessage.class));
        assertEquals("Comprobante de pago - GestoPago", mimeMessage.getSubject());
    }

    @Test
    @DisplayName("Escenarios 4 y 5: Pago rechazado lanza PagoRechazadoProveedorException con comprobante rojo sin cargo y envía correo")
    void pagoRechazadoRetornaComprobanteRojoYEnviaNotificacion() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ClienteEntity cliente = ClienteEntity.builder()
                .nombre("Carlos")
                .apellidoPaterno("Ruiz")
                .apellidoMaterno("Soto")
                .correoElectronico("carlos.prueba@correo.com")
                .build();
        CuentaEntity cuenta = PagoTestDataBuilder.cuentaParaPago(new BigDecimal("500.00"));
        cuenta.setCliente(cliente);

        ProductoDocument prod = ProductoDocument.builder()
                .idProducto(2284)
                .idServicio(18)
                .producto("ABIB 100")
                .servicio("ABIB")
                .precio("100.00")
                .build();

        PagoServicioRequestDto request = PagoTestDataBuilder.unPagoRequest()
                .idServicio(18L)
                .idProducto(2284L)
                .monto(new BigDecimal("100.00"))
                .upe("74454274AB")
                .bandera(false)
                .referencia("5464984648")
                .build();

        GestoPagoSendTxResponse respRechazo = GestoPagoSendTxResponse.builder()
                .codigo("04")
                .mensaje("Referencia inválida en el proveedor")
                .autorizacion("-1")
                .idTxProveedor(null)
                .build();

        when(pagoServicioRepository.findByUpe("74454274AB")).thenReturn(Optional.empty());
        when(productoMongoRepository.findByIdProducto(2284)).thenReturn(Optional.of(prod));
        when(cuentaRepository.findByNumeroCuentaWithLock(request.getNumCuenta())).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches(TestCredentials.testPin(), TestCredentials.testPinHash())).thenReturn(true);
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(PagoTestDataBuilder.tokenActivo()));
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENC");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIG");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class))).thenReturn(respRechazo);
        when(pagoServicioRepository.save(any(PagoServicioEntity.class))).thenAnswer(inv -> {
            PagoServicioEntity ent = inv.getArgument(0);
            if (ent.getId() == null) ent.setId(26L);
            return ent;
        });

        PagoRechazadoProveedorException ex = assertThrows(
                PagoRechazadoProveedorException.class,
                () -> pagoServicioService.procesarPago(request)
        );

        PagoServicioResponseDto comp = ex.getComprobante();
        assertNotNull(comp);
        assertEquals(26L, comp.getId());
        assertEquals("RECHAZADA", comp.getEstatus());
        assertEquals("ABIB 100", comp.getNombreProducto());
        assertEquals("-1", comp.getNumAutorizacion());
        assertNull(comp.getIdTxProveedor()); // No inventa ID Tx Proveedor cuando no está disponible
        assertTrue(Boolean.TRUE.equals(comp.getConfirmadoSinCargo()));
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo()); // Sin descuento

        verify(mailSender, timeout(2000).times(1)).send(any(MimeMessage.class));
        assertEquals("Comprobante de pago - GestoPago", mimeMessage.getSubject());
    }

    @Test
    @DisplayName("Escenarios 6 y 7: Compra digital envía comprobante y reintento con misma UPE NO duplica correo")
    void compraDigitalEnviaCorreoYEvitaDuplicadosPorUpe() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertTrue(NotificacionComprobanteEmailService.detectarSiEsCompra("Certificado Amazon $200", "Amazon"));

        PagoServicioResponseDto compraDto = PagoServicioResponseDto.builder()
                .id(45L)
                .folioTransaccion("GP-AMZ-45")
                .idProducto(512L)
                .monto(new BigDecimal("200.00"))
                .moneda("MXN")
                .upe("AMZN2026XY")
                .nombreProducto("Certificado Amazon $200")
                .nombreServicio("Amazon")
                .numAutorizacion("88990011")
                .destinoPago("5512345678")
                .estatus("APLICADO")
                .mensaje("Compra aprobada")
                .fechaOperacion(LocalDateTime.now())
                .build();

        // Primera notificación
        emailService.notificarOperacionAsync("comprador@correo.com", "Ana Gómez", compraDto, true);
        // Intento duplicado con la misma UPE
        emailService.notificarOperacionAsync("comprador@correo.com", "Ana Gómez", compraDto, true);

        verify(mailSender, timeout(2000).times(1)).send(any(MimeMessage.class));
        assertTrue(emailService.yaFueNotificado("AMZN2026XY"));
    }

    @Test
    @DisplayName("Escenarios 4, 5 y 8: GlobalExceptionHandler retorna comprobante real en 'datos' y no afirma ausencia de cargo si proveedor no lo confirmó")
    void globalExceptionHandlerYCondicionSinCargoEnCorreo() throws Exception {
        var handler = new com.proyecto.servicios.config.GlobalExceptionHandler();
        PagoServicioResponseDto compSinConfirmar = PagoServicioResponseDto.builder()
                .id(30L)
                .folioTransaccion("GP-30")
                .idProducto(2284L)
                .monto(new BigDecimal("100.00"))
                .moneda("MXN")
                .upe("UPE998877A")
                .nombreProducto("ABIB 100")
                .nombreServicio("ABIB")
                .destinoPago("5464984648")
                .estatus("RECHAZADA")
                .confirmadoSinCargo(false)
                .mensaje("Fallo sin confirmación explícita")
                .fechaOperacion(LocalDateTime.now())
                .build();

        PagoRechazadoProveedorException ex = new PagoRechazadoProveedorException("Rechazado", compSinConfirmar);
        var respEntity = handler.handlePagoRechazadoProveedorException(ex);
        assertEquals(400, respEntity.getStatusCode().value());
        assertNotNull(respEntity.getBody());
        assertSame(compSinConfirmar, respEntity.getBody().getDatos());

        String htmlSinConfirmar = ReflectionTestUtils.invokeMethod(
                emailService, "construirHtmlComprobante", "Usuario Prueba", compSinConfirmar, false);
        assertNotNull(htmlSinConfirmar);
        assertTrue(htmlSinConfirmar.contains("#1A237E"));
        assertTrue(htmlSinConfirmar.contains("RECHAZADO"));
        assertFalse(htmlSinConfirmar.contains("No se aplicó ningún cargo a su cuenta"));

        compSinConfirmar.setConfirmadoSinCargo(true);
        String htmlConfirmado = ReflectionTestUtils.invokeMethod(
                emailService, "construirHtmlComprobante", "Usuario Prueba", compSinConfirmar, false);
        assertNotNull(htmlConfirmado);
        assertTrue(htmlConfirmado.contains("No se aplicó ningún cargo a su cuenta"));

        compSinConfirmar.setEstatus("EN_CONCILIACION");
        String htmlPendiente = ReflectionTestUtils.invokeMethod(
                emailService, "construirHtmlComprobante", "Usuario Prueba", compSinConfirmar, false);
        assertNotNull(htmlPendiente);
        assertTrue(htmlPendiente.contains("PENDIENTE / EN CONCILIACIÓN"));
    }

    @Test
    @DisplayName("Validación Backend: Rechaza compra cuando el producto seleccionado no existe en el catálogo real de GestoPago")
    void rechazaCompraSinProductoValidoEnCatalogoReal() {
        var productoServiceMock = mock(com.proyecto.servicios.service.ProductoService.class);
        ReflectionTestUtils.setField(pagoServicioService, "productoService", productoServiceMock);

        var catalogoReal = new com.proyecto.servicios.model.producto.ProductoResponseDto();
        catalogoReal.setProductos(java.util.List.of(
                com.proyecto.servicios.model.producto.ProductoItemDto.builder()
                        .idProducto(133)
                        .idServicio(5)
                        .producto("Recarga $10")
                        .servicio("AT&T")
                        .precio("10.00")
                        .build()
        ));
        when(productoServiceMock.buscarProductoRealPorId(99999)).thenReturn(Optional.empty());
        when(productoServiceMock.obtenerListaProductos()).thenReturn(catalogoReal);

        PagoServicioRequestDto reqInvalido = PagoTestDataBuilder.unPagoRequest()
                .idServicio(1L)
                .idProducto(99999L)
                .upe("UPE99999AB")
                .build();
        when(pagoServicioRepository.findByUpe("UPE99999AB")).thenReturn(Optional.empty());

        var ex = assertThrows(com.proyecto.servicios.exception.ReglaNegocioException.class,
                () -> pagoServicioService.procesarPago(reqInvalido));
        assertTrue(ex.getMessage().contains("no existe"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }
}
