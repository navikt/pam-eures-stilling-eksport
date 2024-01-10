package no.nav.pam.euresstillingeksport.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
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
    override fun doFilter(req: HttpServletRequest?, res: HttpServletResponse?, chain: FilterChain?) {
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
