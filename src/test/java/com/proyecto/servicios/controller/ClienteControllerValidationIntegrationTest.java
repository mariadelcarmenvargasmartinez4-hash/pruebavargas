package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteConsultaFiltroRecord;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.security.SecurityService;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.TestCredentials;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
public class ClienteControllerValidationIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ClienteController clienteController;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private ClienteRegistroRequestDto crearDtoBase() {
        return ClienteRegistroRequestDto.builder()
                .nombre("Ana")
                .segundoNombre(null)
                .apellidoPaterno("Gomez")
                .apellidoMaterno("Martinez")
                .fechaNacimiento(LocalDate.of(1995, 8, 22))
                .curp("GOMA950822MDFRTN03")
                .rfc("GOMA9508223B4")
                .sexo("FEM")
                .nacionalidad("MEX")
                .estadoCivil("CASADO")
                .correoElectronico("ana.gomez@example.com")
                .telefonoMovil("5598765432")
                .calle("Insurgentes Sur")
                .numeroExterior("456")
                .colonia("Del Valle")
                .municipio("Benito Juarez")
                .estado("Ciudad de Mexico")
                .codigoPostal("03100")
                .pais("Mexico")
                .ocupacion("Analista Financiero")
                .empresa("Global Bank SA")
                .ingresoMensual(new BigDecimal("42000.00"))
                .tipoCuenta("AHORRO")
                .password(TestCredentials.validPass())
                .build();
    }

    @Test
    @DisplayName("Happy Path: POST /clientes registra cliente exitosamente retornando HTTP 201 Created")
    public void testRegistrarClienteExitosoRetorna201() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        ClienteRegistroResponseDto response = ClienteRegistroResponseDto.builder()
                .codigo(0)
                .nombreCompleto("Ana Gomez Martinez")
                .curp("GOMA950822MDFRTN03")
                .build();

        when(clienteService.registrarCliente(any(ClienteRegistroRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCompleto", is("Ana Gomez Martinez")))
                .andExpect(jsonPath("$.curp", is("GOMA950822MDFRTN03")));
    }

    @Test
    @DisplayName("Happy Path: GET /clientes consulta paginada de clientes retorna HTTP 200 OK")
    public void testConsultarClientesPaginadosRetorna200() throws Exception {
        ClienteRegistroResponseDto cliente = ClienteRegistroResponseDto.builder()
                .nombreCompleto("Ana Gomez Martinez")
                .curp("GOMA950822MDFRTN03")
                .build();

        PaginaResponseRecord<ClienteRegistroResponseDto> pagina = new PaginaResponseRecord<>(
                List.of(cliente), 0, 10, 1L, 1, true, true);

        when(clienteService.buscarClientesPaginados(any(ClienteConsultaFiltroRecord.class))).thenReturn(pagina);

        mockMvc.perform(get("/clientes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.totalElementos", is(1)));
    }

    @Test
    @DisplayName("Happy Path: GET /clientes/{id} retorna cliente y HTTP 200 OK")
    public void testObtenerClientePorIdRetorna200() throws Exception {
        ClienteRegistroResponseDto response = ClienteRegistroResponseDto.builder()
                .nombreCompleto("Ana Gomez Martinez")
                .curp("GOMA950822MDFRTN03")
                .build();

        when(clienteService.obtenerClientePorId(10L)).thenReturn(response);

        mockMvc.perform(get("/clientes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto", is("Ana Gomez Martinez")));
    }

    @Test
    @DisplayName("Happy Path: PUT /clientes/{id} actualiza información y retorna HTTP 200 OK")
    public void testActualizarClienteRetorna200() throws Exception {
        ClienteActualizacionRequestDto request = ClienteActualizacionRequestDto.builder()
                .correoElectronico("nuevo.correo@example.com")
                .telefonoMovil("5511223344")
                .calle("Av. Insurgentes 789")
                .numeroExterior("10")
                .colonia("Condesa")
                .municipio("Cuauhtemoc")
                .estado("CDMX")
                .codigoPostal("06140")
                .pais("Mexico")
                .build();

        ClienteRegistroResponseDto response = ClienteRegistroResponseDto.builder()
                .nombreCompleto("Ana Gomez Martinez")
                .build();

        when(clienteService.actualizarCliente(eq(10L), any(ClienteActualizacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/clientes/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto", is("Ana Gomez Martinez")));
    }

    @Test
    @DisplayName("Happy Path: DELETE /clientes/{id} da de baja lógica y retorna HTTP 204 No Content")
    public void testDesactivarClienteRetorna204() throws Exception {
        doNothing().when(clienteService).desactivarCliente(10L);

        mockMvc.perform(delete("/clientes/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Happy Path: POST /clientes/{id}/reactivar reactiva cliente y retorna HTTP 200 OK")
    public void testReactivarClienteRetorna200() throws Exception {
        ClienteRegistroResponseDto response = ClienteRegistroResponseDto.builder()
                .nombreCompleto("Ana Gomez Martinez")
                .build();

        when(clienteService.reactivarCliente(10L)).thenReturn(response);

        mockMvc.perform(post("/clientes/10/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto", is("Ana Gomez Martinez")));
    }

    @Test
    @DisplayName("Validación: Rechazar nombre con espacios al inicio retorna HTTP 400")
    public void testRechazarNombreConEspaciosAlInicio() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setNombre(" Ana");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("no puede iniciar con espacios")))
                .andExpect(jsonPath("$.errores.nombre", containsString("no puede iniciar con espacios")));
    }

    @Test
    @DisplayName("Validación: Rechazar nombre con espacios al final retorna HTTP 400")
    public void testRechazarNombreConEspaciosAlFinal() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setNombre("Ana   ");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("no puede terminar con espacios")))
                .andExpect(jsonPath("$.errores.nombre", containsString("no puede terminar con espacios")));
    }

    @Test
    @DisplayName("Validación: Parámetro de ruta con letras donde va un ID numérico retorna HTTP 400 Bad Request")
    public void testIdNoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/clientes/letras-en-lugar-de-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", containsString("es inválido")));
    }

    @Test
    @DisplayName("Integridad: CURP duplicada retorna HTTP 409 Conflict")
    public void testCurpDuplicadaRetorna409() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();

        when(clienteService.registrarCliente(any(ClienteRegistroRequestDto.class)))
                .thenThrow(new CurpDuplicadaException("La CURP 'GOMA950822MDFRTN03' ya se encuentra registrada en el sistema"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", containsString("CURP")));
    }

    @Test
    @DisplayName("Integridad: RFC duplicado retorna HTTP 409 Conflict")
    public void testRfcDuplicadoRetorna409() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();

        when(clienteService.registrarCliente(any(ClienteRegistroRequestDto.class)))
                .thenThrow(new RfcDuplicadoException("El RFC 'GOMA9508223B4' ya se encuentra registrado en el sistema"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", containsString("RFC")));
    }

    @Test
    @DisplayName("Seguridad: Cliente no existente retorna HTTP 404 Not Found")
    public void testClienteNoExistenteRetorna404() throws Exception {
        when(clienteService.obtenerClientePorId(999L))
                .thenThrow(new ClienteNoEncontradoException("No se encontró el cliente con ID 999"));

        mockMvc.perform(get("/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", containsString("No se encontró el cliente")));
    }

    @Test
    @DisplayName("Seguridad (Anti-IDOR): Acceso no autorizado a otro cliente retorna HTTP 403 Forbidden")
    public void testAccesoOtroClienteRetorna403() throws Exception {
        doThrow(new AccessDeniedException("Acceso denegado: No tiene permisos para consultar este cliente"))
                .when(securityService).validarAccesoCliente(anyLong());

        mockMvc.perform(get("/clientes/888"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is(403)))
                .andExpect(jsonPath("$.mensaje", containsString("Acceso denegado")));
    }

    @Test
    @DisplayName("Robustez ante Inyección: Intento de SQL injection en CURP es bloqueado con HTTP 400")
    public void testInyeccionSqlEnCurpRetorna400() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setCurp("' OR '1'='1");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.curp").isNotEmpty());
    }
}
