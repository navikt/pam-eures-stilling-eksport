package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Tag
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import no.nav.pam.euresstillingeksport.infrastruktur.VaultClient
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpRequest
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.io.File
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
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
    companion object {
        private val LOG = LoggerFactory.getLogger(ApiConfiguration::class.java)
    }

    @Bean
    @Primary
    open fun objectMapper() =
            ObjectMapper().apply {
                registerModule(KotlinModule())
                registerModule(JavaTimeModule())
            }

    @Bean()
    @Profile("prod-sbs |dev-sbs")
    fun dataSource(@Value("\${spring.datasource.url}") url: String,
                   @Value("\${spring.datasource.hikari.minimum-idle}") minimumIdle: Int = 1,
                   @Value("\${spring.datasource.hikari.maximum-pool-size}") maxPoolSize: Int = 3,
                   @Autowired vaultClient: VaultClient
    ): DataSource {
        val auth = vaultClient.getVaultToken(role = "static-creds")
        val creds = vaultClient.getDbCredentials(vaultToken = auth)
        LOG.info("Got db credentials from vault. TTL: ${creds.ttl}")

        val dsb = HikariDataSource()
        dsb.jdbcUrl = url
        dsb.username = creds.username
        dsb.password = creds.password
        dsb.minimumIdle = minimumIdle
        dsb.maximumPoolSize = maxPoolSize
        return dsb
    }

    @Bean("safeElasticClientBuilder")
    fun safeElasticClientBuilder(@Value("\${internalad-search-api.url}") elasticsearchUrl: URL? = null): RestClientBuilder {
        return RestClient.builder(HttpHost.create(elasticsearchUrl.toString()))
                .setPathPrefix("/eures/internalad")
                .setRequestConfigCallback { requestConfigBuilder: RequestConfig.Builder ->
                    requestConfigBuilder
                            .setConnectionRequestTimeout(5000)
                            .setConnectTimeout(10000)
                            .setSocketTimeout(20000)
                }
                .setHttpClientConfigCallback { httpAsyncClientBuilder: HttpAsyncClientBuilder ->
                    httpAsyncClientBuilder // Fix SSL hostname verification for *.local domains:
                            .setSSLHostnameVerifier(DefaultHostnameVerifier())
                            .setMaxConnTotal(256)
                            .setMaxConnPerRoute(256)
                }
    }

    @Bean
    open fun restTemplate(@Autowired restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder.build()
    }

    @Bean
    fun flywayConfig(@Value("\${dbnavn}") dbnavn: String,
                     @Value("\${spring.datasource.url}") jdbcUrl: String): FlywayConfigurationCustomizer =
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

    @ExceptionHandler(value = [(Exception::class)])
    fun loggingExceptionHandler(e: Exception, wr: WebRequest): ResponseEntity<Any>? {
        LOG.info("Uhåndtert feil propagerte til webserver: {}", e.message, e)
        return handleException(e, wr)
    }
}
