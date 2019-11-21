package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.jdbc.SchemaManagement
import org.springframework.boot.jdbc.SchemaManagementProvider
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.sql.DataSource

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT45M")
@EnableRetry
class ApiConfiguration {
    @Bean
    @Primary
    open fun objectMapper() =
            ObjectMapper().apply {
                registerModule(KotlinModule())
                registerModule(JavaTimeModule())
            }

    @Bean
    open fun restTemplate(@Autowired restTemplateBuilder : RestTemplateBuilder,
                          @Value("\${spring.profiles.active}") profil: String) : RestTemplate {
        if ("dev" == profil)
            disableSSLChecksDefaultHttpClient()
        return restTemplateBuilder.build()
    }

    @Bean
    open fun lockProvider(@Autowired dataSource: DataSource) =
            JdbcTemplateLockProvider(dataSource)

    fun disableSSLChecksDefaultHttpClient() {
        val trustManager: X509TrustManager = object: X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> =
                    emptyArray()
        }

        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
    }

    /* Alt som er her av flywaykonfigurasjon kan fjernes når vi går over til postgresql
       Grunnen til at det er med er at vi må bruke gammel versjon av flyway for å støtte den
       gamle versjonen av oracle som er i prod....
     */
    @Bean
    @Primary
    fun flywayDefaultDdlModeProvider(flyways : ObjectProvider<Flyway>) =
            SchemaManagementProvider {
                SchemaManagement.MANAGED
            }
    @Bean(initMethod = "migrate")
    fun flyway(dataSource : DataSource) =
            Flyway().apply { setDataSource(dataSource) }
    @Bean
    fun flywayInitializer(@Autowired flyway : Flyway) =
            FlywayMigrationInitializer(flyway, null)

}
@Configuration
class FlywayInitializerJdbcOperationsDependencyConfiguration : EntityManagerFactoryDependsOnPostProcessor("flywayInitializer") {
}


@ControllerAdvice
class WebControllerErrorHandler : ResponseEntityExceptionHandler() {
    companion object {
        private val LOG = LoggerFactory.getLogger(WebControllerErrorHandler::class.java)
    }
    @ExceptionHandler(value=[(Exception::class)])
    fun loggingExceptionHandler(e: Exception, wr: WebRequest): ResponseEntity<Any>? {
        LOG.info("Uhåndtert feil propagerte til webserver: {}", e.message, e)
        return handleException(e, wr)
    }
}