package com.govinda777.execution.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Manager API (CAPE)")
                        .version("1.0.0")
                        .description("API do Cloud Account Provisioning Engine (CAPE) para provisionamento automatizado de contas multi-cloud (AWS, GCP, Azure), controle de centros de custo e consolidação de relatórios financeiros.")
                        .contact(new Contact()
                                .name("Equipe de Cloud Manager")
                                .email("suporte@cloudmanager.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
