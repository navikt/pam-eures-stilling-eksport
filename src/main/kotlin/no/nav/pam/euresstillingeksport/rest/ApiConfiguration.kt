package no.nav.pam.euresstillingeksport.rest

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.sql.DataSource

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT45M")
class ApiConfiguration {
    companion object {
        private val LOG = LoggerFactory.getLogger(ApiConfiguration::class.java)
    }

    @Bean
    @Primary
    fun objectMapper() =
            JsonMapper.builder()
                .addModule(
                    KotlinModule.Builder()
                        .disable(KotlinFeature.StrictNullChecks)
                        .enable(KotlinFeature.NullIsSameAsDefault)
                        .enable(KotlinFeature.KotlinPropertyNameAsImplicitName)
                        .build()
                )
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build()

    @Bean
    @DependsOnDatabaseInitialization
    fun lockProvider(dataSource: DataSource): LockProvider = JdbcTemplateLockProvider(dataSource)

    @Bean
    fun denyInternalFilter(): FilterRegistrationBean<HttpFilter> {
        val reg = FilterRegistrationBean<HttpFilter>()
        reg.setFilter(DenyEksternFilter())
        reg.setName("DenyAccessToInternalFromEkstern")
        reg.addUrlPatterns("/actuator/*", "/internal/*")
        reg.order = 1
        return reg
    }
}

class DenyEksternFilter : HttpFilter() {
    override fun doFilter(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val host = req.getHeader("host").orEmpty()
        if (host.contains(".ekstern.", true) || host.contains("eures-eksport-gcp.nav.no", true)) {
            res.status = HttpServletResponse.SC_FORBIDDEN
            return
        }
        chain.doFilter(req, res)
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
