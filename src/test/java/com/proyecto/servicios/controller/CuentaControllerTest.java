package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaConsultaFiltroRecord;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.security.SecurityService;
import com.proyecto.servicios.service.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CuentaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private CuentaController cuentaController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cuentaController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Happy Path: POST /cuentas crea cuenta exitosamente y retorna HTTP 201 Created")
    void crearCuentaExitosaRetorna201() throws Exception {
        CuentaCreacionRequestDto request = CuentaCreacionRequestDto.builder()
                .clienteId(1L)
                .tipoCuenta("AHORRO")
                .saldoInicial(new BigDecimal("1500.00"))
                .build();

        CuentaBancariaDto response = CuentaBancariaDto.builder()
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("AHORRO")
                .saldo(new BigDecimal("1500.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();

        when(cuentaService.crearCuenta(any(CuentaCreacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta", is("4815162342")))
                .andExpect(jsonPath("$.estatus", is("ACTIVA")));
    }

    @Test
    @DisplayName("Happy Path: GET /cuentas consulta dinámica paginada retorna HTTP 200 OK")
    void consultarCuentasPaginadasRetorna200() throws Exception {
        CuentaBancariaDto cuenta = CuentaBancariaDto.builder()
                .numeroCuenta("4815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        PaginaResponseRecord<CuentaBancariaDto> pagina = new PaginaResponseRecord<>(
                List.of(cuenta), 0, 10, 1L, 1, true, true);

        when(cuentaService.buscarCuentasPaginadas(any(CuentaConsultaFiltroRecord.class))).thenReturn(pagina);

        mockMvc.perform(get("/cuentas")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.totalElementos", is(1)))
                .andExpect(jsonPath("$.datos.contenido[0].numeroCuenta", is("4815162342")));
    }

    @Test
    @DisplayName("Happy Path: GET /cuentas/{numeroCuenta} retorna HTTP 200 OK")
    void consultarCuentaPorNumeroRetorna200() throws Exception {
        CuentaBancariaDto response = CuentaBancariaDto.builder()
                .numeroCuenta("4815162342")
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerCuentaPorNumero("4815162342")).thenReturn(response);

        mockMvc.perform(get("/cuentas/4815162342"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is("4815162342")));
    }

    @Test
    @DisplayName("Happy Path: GET /cuentas/{numeroCuenta}/saldo retorna saldo y HTTP 200 OK")
    void consultarSaldoRetorna200() throws Exception {
        SaldoCuentaDto response = SaldoCuentaDto.builder()
                .numeroCuenta("4815162342")
                .saldo(new BigDecimal("3500.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerSaldoCuenta("4815162342")).thenReturn(response);

        mockMvc.perform(get("/cuentas/4815162342/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is("4815162342")))
                .andExpect(jsonPath("$.saldo", is(3500.00)));
    }

    @Test
    @DisplayName("Happy Path: PUT /cuentas/{numeroCuenta} actualiza cuenta y retorna HTTP 200 OK")
    void actualizarCuentaRetorna200() throws Exception {
        CuentaActualizacionRequestDto request = CuentaActualizacionRequestDto.builder()
                .tipoCuenta("AHORRO")
                .estatus("ACTIVA")
                .saldo(new BigDecimal("5000.00"))
                .build();

        CuentaBancariaDto response = CuentaBancariaDto.builder()
                .numeroCuenta("4815162342")
                .tipoCuenta("AHORRO")
                .saldo(new BigDecimal("5000.00"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.actualizarCuenta(eq("4815162342"), any(CuentaActualizacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/cuentas/4815162342")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is("4815162342")))
                .andExpect(jsonPath("$.saldo", is(5000.00)));
    }

    @Test
    @DisplayName("Happy Path: DELETE /cuentas/{numeroCuenta} cancela cuenta y retorna HTTP 204 No Content")
    void desactivarCuentaRetorna204() throws Exception {
        doNothing().when(cuentaService).desactivarCuenta("4815162342");

        mockMvc.perform(delete("/cuentas/4815162342"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Validación: POST /cuentas con saldo negativo retorna HTTP 400 Bad Request")
    void crearCuentaSaldoNegativoRetorna400() throws Exception {
        CuentaCreacionRequestDto request = CuentaCreacionRequestDto.builder()
                .clienteId(1L)
                .tipoCuenta("AHORRO")
                .saldoInicial(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.saldoInicial").isNotEmpty());
    }

    @Test
    @DisplayName("Seguridad: GET /cuentas/{numeroCuenta} no existente retorna HTTP 404 Not Found")
    void cuentaNoExistenteRetorna404() throws Exception {
        when(cuentaService.obtenerCuentaPorNumero("0000000000"))
                .thenThrow(new CuentaNoEncontradaException("La cuenta 0000000000 no existe"));

        mockMvc.perform(get("/cuentas/0000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", containsString("no existe")));
    }

    @Test
    @DisplayName("Seguridad (Anti-IDOR): Acceso a cuenta ajena retorna HTTP 403 Forbidden")
    void accesoCuentaAjenaRetorna403() throws Exception {
        doThrow(new AccessDeniedException("Acceso denegado: No tiene permisos para manipular cuentas de otro titular"))
                .when(securityService).validarAccesoCuenta(anyString());

        mockMvc.perform(get("/cuentas/9999999999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is(403)))
                .andExpect(jsonPath("$.mensaje", containsString("Acceso denegado")));
    }
}
