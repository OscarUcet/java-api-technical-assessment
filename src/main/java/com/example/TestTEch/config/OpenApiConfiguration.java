package com.example.TestTEch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI countriesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Countries API")
                .version("1.0.0")
                .description("Importa países del Banco Mundial y consulta los datos almacenados. No requiere autenticación."));
    }
}
