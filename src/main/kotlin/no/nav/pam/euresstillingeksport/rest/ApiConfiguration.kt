package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.JSR310DateTimeDeserializerBase
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfiguration {
    @Bean
    @Primary
    open fun objectMapper() =
            ObjectMapper().apply {
                registerModule(KotlinModule())
                registerModule(JavaTimeModule())
            }

    @Bean
    open fun restTemplate(@Autowired restTemplateBuilder : RestTemplateBuilder) =
            restTemplateBuilder.build()
}