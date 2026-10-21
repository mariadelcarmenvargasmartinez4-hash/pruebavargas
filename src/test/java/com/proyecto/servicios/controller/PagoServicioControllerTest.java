package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.service.PagoServicioService;
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
    @DisplayName("POST /pagos - Recarga telefónica válida (bandera=true) retorna HTTP 201 Created")
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
                .pin("1234")
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
    @DisplayName("POST /pagos - Pago de servicio con referencia válido (bandera=false) retorna HTTP 201 Created")
    void procesarPagoServicioReferenciaExitoso() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(2L)
                .idProducto(202L)
                .precio(new BigDecimal("250.50"))
                .upe("9876543210")
                .bandera(false)
                .referencia("CFE-987654321")
                .pin("5678")
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
    @DisplayName("POST /pagos - UPE inválido (longitud diferente a 10 dígitos) detona HTTP 400 Bad Request")
    void upeInvalidoRetorna400() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("50.00"))
                .upe("123") // Menor a 10 dígitos
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.upe").isNotEmpty());
    }

    @Test
    @DisplayName("POST /pagos - Cuenta bancaria no encontrada retorna HTTP 404 Not Found")
    void cuentaNoEncontradaRetorna404() throws Exception {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("9999999999999999")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("50.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(pagoServicioService.procesarPago(any(PagoServicioRequestDto.class)))
                .thenThrow(new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número 9999999999999999"));

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)));
    }
}
