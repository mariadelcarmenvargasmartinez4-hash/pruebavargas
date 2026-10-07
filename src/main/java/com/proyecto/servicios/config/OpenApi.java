package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Configuracion de Swagger OpenAPI
@Configuration
public class OpenApi {

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("API Registro de Clientes y Cuentas Bancarias")
                        .version("1.0")
                        .description("API REST para registro de clientes, catálogos, cuentas y autenticación"))
                .servers(List.of(
                        new Server().url("/").description("Servidor actual (Render / Local)")
                ));
    }
}
