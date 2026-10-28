package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.model.producto.ProductoItemDto;
import com.proyecto.servicios.model.producto.ProductoResponseDto;
import com.proyecto.servicios.service.ProductoService;
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

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Happy Path: GET /productos retorna lista de productos y HTTP 200 OK")
    void obtenerProductosRetorna200() throws Exception {
        ProductoItemDto item = ProductoItemDto.builder()
                .idProducto(101)
                .servicio("TELCEL")
                .producto("Recarga 100")
                .precio("100.00")
                .build();

        ProductoResponseDto response = new ProductoResponseDto();
        response.setCodigo(200);
        response.setMensaje("Exito");
        response.setProductos(List.of(item));

        when(productoService.obtenerListaProductos()).thenReturn(response);

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.productos[0].idProducto", is(101)))
                .andExpect(jsonPath("$.productos[0].servicio", is("TELCEL")));
    }

    @Test
    @DisplayName("Robustez: POST /productos no permitido retorna HTTP 405 Method Not Allowed")
    void postProductosRetorna405() throws Exception {
        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo", is(405)));
    }
}
