package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ClienteControllerValidationIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
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
                .password("NuevaClaveSuperSegura2026!")
                .build();
    }

    @Test
    public void testRechazarNombreConEspaciosAlInicio() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setNombre(" Ana");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre", containsString("no puede iniciar con espacios")));
    }

    @Test
    public void testRechazarNombreConEspaciosAlFinal() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setNombre("Ana   ");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre", containsString("no puede terminar con espacios")));
    }

    @Test
    public void testRechazarSegundoNombreConEspaciosAlInicio() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setSegundoNombre(" Carlos");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.segundoNombre", containsString("no puede iniciar con espacios")));
    }

    @Test
    public void testRechazarSegundoNombreConMuchosEspaciosAlFinal() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setSegundoNombre("Carlos     ");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.segundoNombre", containsString("no puede terminar con espacios")));
    }

    @Test
    public void testRechazarSegundoNombreMenosDe2Caracteres() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setSegundoNombre("C");

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.segundoNombre", containsString("debe tener al menos 2 caracteres")));
    }

    @Test
    public void testRechazarSegundoNombreMasDe50Caracteres() throws Exception {
        ClienteRegistroRequestDto dto = crearDtoBase();
        dto.setSegundoNombre("C".repeat(55));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.segundoNombre", containsString("no puede exceder 50 caracteres")));
    }
}
