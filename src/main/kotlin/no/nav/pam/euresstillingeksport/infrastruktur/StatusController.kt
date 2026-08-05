package no.nav.pam.euresstillingeksport.infrastruktur

import no.nav.pam.euresstillingeksport.kafka.KafkaHealthService
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Exception

@RestController
@RequestMapping("/internal")
class StatusController(private val repo: StillingRepository,
                       private val kafkaHealthService: KafkaHealthService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @GetMapping("/isAlive")
    fun isAlive(): ResponseEntity<String> {
        try {
            repo.findStillingsannonserByIds(listOf("finnes_ikke"))
        } catch (e: Exception) {
            LOG.info("Failed to connect to database", e)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Not OK")
        }

        return if (kafkaHealthService.isHealthy())
            ResponseEntity.ok("OK")
        else
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Not OK")
    }

    @GetMapping("/isReady")
    fun isReady(): ResponseEntity<String> =
            ResponseEntity.ok("OK")
    
}
