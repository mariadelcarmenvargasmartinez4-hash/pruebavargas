package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

// Configuracion de limpieza automatica de cadenas en peticiones JSON para evitar espacios al inicio o fin
@Configuration
public class StringTrimConfig {

    @Bean
    public SimpleModule stringTrimModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StdScalarDeserializer<String>(String.class) {
            @Override
            public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                return value != null ? value.trim() : null;
            }
        });
        return module;
    }
}
