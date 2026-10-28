package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.EntidadNoProcesableException;
import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.TestCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoServicioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoServicioService pagoServicioService;

    @InjectMocks
    private PagoServicioController pagoServicioController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoServicioController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("GET /pagos - Información del servicio retorna HTTP 200")
    void obtenerInformacionRetorna200() throws Exception {
        mockMvc.perform(get("/pagos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.mensaje", is("API de Pagos de Servicios activa")));
    }

    @Test
    @DisplayName("Happy Path: POST /pagos - Recarga telefónica válida (bandera=true) retorna HTTP 201 Created")
    void procesarRecargaTelefonicaExitosa() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .idConcepto(501L)
                .idDistribuidor(83)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin(TestCredentials.testPin())
                .build();

        PagoServicioResponseDto response = PagoServicioResponseDto.builder()
                .folioTransaccion("TX-abc-123")
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoActual(new BigDecimal("400.00"))
                .upe("1234567890")
                .tipoOperacion("RECARGA_TELEFONICA")
                .destinoPago("5512345678")
                .estatus("APLICADO")
                .fechaOperacion(LocalDateTime.now())
                .build();

        when(pagoServicioService.procesarPago(any(PagoServicioRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.folioTransaccion", is("TX-abc-123")))
                .andExpect(jsonPath("$.datos.saldoActual", is(400.00)))
                .andExpect(jsonPath("$.datos.tipoOperacion", is("RECARGA_TELEFONICA")));
    }

    @Test
    @DisplayName("Happy Path: POST /pagos - Pago de servicio con referencia válido (bandera=false) retorna HTTP 201 Created")
    void procesarPagoServicioReferenciaExitoso() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(2L)
                .idProducto(202L)
                .precio(new BigDecimal("250.50"))
                .upe("9876543210")
                .bandera(false)
                .referencia("CFE-987654321")
                .pin(TestCredentials.testPin())
                .build();

        PagoServicioResponseDto response = PagoServicioResponseDto.builder()
                .folioTransaccion("TX-def-456")
                .numCuenta("1234567890123456")
                .idServicio(2L)
                .idProducto(202L)
                .monto(new BigDecimal("250.50"))
                .saldoAnterior(new BigDecimal("1000.00"))
                .saldoActual(new BigDecimal("749.50"))
                .upe("9876543210")
                .tipoOperacion("PAGO_SERVICIO_REFERENCIA")
                .destinoPago("CFE-987654321")
                .estatus("APLICADO")
                .fechaOperacion(LocalDateTime.now())
                .build();

        when(pagoServicioService.procesarPago(any(PagoServicioRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.folioTransaccion", is("TX-def-456")))
                .andExpect(jsonPath("$.datos.destinoPago", is("CFE-987654321")));
    }

    @Test
    @DisplayName("Validación: POST /pagos con monto cero o negativo retorna HTTP 400 Bad Request")
    void montoInvalidoRetorna400() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(BigDecimal.ZERO) // Monto inválido
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin(TestCredentials.testPin())
                .build();

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.monto").isNotEmpty());
    }

    @Test
    @DisplayName("Validación: POST /pagos con UPE inválido (menor a 10 dígitos) retorna HTTP 400 Bad Request")
    void upeInvalidoRetorna400() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("50.00"))
                .upe("123")
                .bandera(true)
                .telefono("5512345678")
                .pin(TestCredentials.testPin())
                .build();

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.upe").isNotEmpty());
    }

    @Test
    @DisplayName("Seguridad: POST /pagos con cuenta no encontrada retorna HTTP 404 Not Found")
    void cuentaNoEncontradaRetorna404() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("9999999999999999")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("50.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin(TestCredentials.testPin())
                .build();

        when(pagoServicioService.procesarPago(any(PagoServicioRequestDto.class)))
                .thenThrow(new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número 9999999999999999"));

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)));
    }

    @Test
    @DisplayName("Integridad: POST /pagos con producto no catalogado retorna HTTP 422 Unprocessable Entity")
    void productoNoProcesableRetorna422() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(999L)
                .idProducto(888L)
                .monto(new BigDecimal("50.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin(TestCredentials.testPin())
                .build();

        when(pagoServicioService.procesarPago(any(PagoServicioRequestDto.class)))
                .thenThrow(new EntidadNoProcesableException("El servicio con ID 999 no existe en el catálogo activo"));

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo", is(422)))
                .andExpect(jsonPath("$.mensaje", containsString("no existe en el catálogo activo")));
    }

    @Test
    @DisplayName("Robustez ante Inyección: Intento de XSS o caracteres maliciosos en teléfono retorna HTTP 400 Bad Request")
    void inyeccionMaliciosaEnTelefonoRetorna400() throws Exception {
        String payloadConInyeccion = "{"
                + "\"numCuenta\":\"1234567890123456\","
                + "\"idServicio\":1,"
                + "\"idProducto\":101,"
                + "\"monto\":100.00,"
                + "\"upe\":\"1234567890\","
                + "\"bandera\":true,"
                + "\"telefono\":\"<script>alert(1)</script>\","
                + "\"pin\":\"" + TestCredentials.testPin() + "\""
                + "}";

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConInyeccion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.telefono").isNotEmpty());
    }

    @Test
    @DisplayName("Happy Path: POST /pagos/conciliar con UPE registrado retorna HTTP 200 OK")
    void conciliarTransaccionExitosaRetorna200() throws Exception {
        PagoConciliacionRequestDto request = PagoConciliacionRequestDto.builder()
                .upe("1234567890")
                .build();

        PagoServicioResponseDto response = PagoServicioResponseDto.builder()
                .upe("1234567890")
                .folioTransaccion("GP-TX-888")
                .estatus("APLICADO")
                .build();

        when(pagoServicioService.consultarOConciliarPago(any(PagoConciliacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/pagos/conciliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.upe", is("1234567890")))
                .andExpect(jsonPath("$.datos.estatus", is("APLICADO")));
    }
}
