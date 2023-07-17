package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.micrometer.core.instrument.Tag
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
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
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
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
import java.net.URL
import java.util.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.server.observation.ServerRequestObservationConvention
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
    fun transactionManager(@Autowired dataSource: DataSource): PlatformTransactionManager =
            DataSourceTransactionManager(dataSource)

    @Bean
    open fun lockProvider(@Autowired dataSource: DataSource, @Autowired transactionManager: PlatformTransactionManager) =
            JdbcTemplateLockProvider(JdbcTemplate(dataSource), transactionManager)

    /*
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

    private class testing : ServerRequestObservationConvention {
        val tester : ServerRequestObservationConvention =
    }
*/
    @Bean
    fun denyInternalFilter() : FilterRegistrationBean<HttpFilter> {
        val reg = FilterRegistrationBean<HttpFilter>();
        reg.filter = DenyEksternFilter()
        reg.setName("DenyAccessToInternalFromEkstern")
        reg.addUrlPatterns("/actuator/*", "/internal/*")
        reg.order = 1
        return reg;
    }
}

class DenyEksternFilter : HttpFilter() {
    override protected fun doFilter(req: HttpServletRequest?, res: HttpServletResponse?, chain: FilterChain?) {
        if (req != null &&
                (req.getHeader("host").contains(".ekstern.", true) ||
                    req.getHeader("host").contains("eures-eksport-gcp.nav.no", true))
                        ) {
            res!!.status = HttpServletResponse.SC_FORBIDDEN
            return
        }
        super.doFilter(req, res, chain)
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
