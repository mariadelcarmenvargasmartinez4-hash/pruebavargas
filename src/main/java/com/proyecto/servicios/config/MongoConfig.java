package com.proyecto.servicios.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

// Configuracion de repositorios MongoDB para coexistir con JPA
@Configuration
@EnableMongoRepositories(basePackages = "com.proyecto.servicios.repositorys.mongo")
public class MongoConfig {
}
