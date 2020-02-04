package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.micrometer.core.instrument.Tag
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpRequest
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
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
    fun flywayConfig(@Value("\${dbnavn}") dbnavn: String,
                     @Value("\${spring.datasource.url}") jdbcUrl: String) : FlywayConfigurationCustomizer =
            FlywayConfigurationCustomizer { c ->
                if (jdbcUrl.toLowerCase().contains("jdbc:postgresql"))
                    c.initSql("SET ROLE \"${dbnavn}-admin\"")
            }

    @Bean
    fun transactionManager(@Autowired dataSource: DataSource): PlatformTransactionManager =
            DataSourceTransactionManager(dataSource)

    @Bean
    open fun lockProvider(@Autowired dataSource: DataSource, @Autowired transactionManager: PlatformTransactionManager) =
            JdbcTemplateLockProvider(JdbcTemplate(dataSource), transactionManager)

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

    @Bean
    fun restTemplateTagConfigurer(): RestTemplateExchangeTagsProvider? {
        return CustomRestTemplateExchangeTagsProvider()
    }

    private class CustomRestTemplateExchangeTagsProvider : RestTemplateExchangeTagsProvider {
        override fun getTags(urlTemplate: String?, request: HttpRequest, response: ClientHttpResponse): Iterable<Tag> {
            // we only use path for tags, because of hitting a limit of tags. The cardinality for uri might cause issue.
            return Arrays.asList(
                    RestTemplateExchangeTags.method(request),
                    RestTemplateExchangeTags.uri(request.uri.path),
                    RestTemplateExchangeTags.status(response),
                    RestTemplateExchangeTags.clientName(request))
        }
    }
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