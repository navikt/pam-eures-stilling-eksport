package no.nav.pam.euresstillingeksport.infrastruktur

import no.nav.pam.euresstillingeksport.kafka.KafkaHealthService
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Exception

@RestController
@RequestMapping("/internal")
class StatusController(@Autowired private val repo: StillingRepository,
                       @Autowired private val kafkaHealthService: KafkaHealthService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @RequestMapping("/isAlive")
    fun isAlive(): ResponseEntity<String> {
        try {
            repo.findStillingsannonserByIds(listOf("finnes_ikke"))
        } catch (e: Exception) {
            LOG.info("Failed to connect to database", e)
            return ResponseEntity("Not OK", HttpStatus.SERVICE_UNAVAILABLE)
        }

        return if (kafkaHealthService.isHealthy())
            ResponseEntity("OK", HttpStatus.OK)
        else
            ResponseEntity("Not OK", HttpStatus.SERVICE_UNAVAILABLE)
    }

    @RequestMapping("/isReady")
    fun isReady(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)

    @RequestMapping("/diediedie")
    fun diediedie(): ResponseEntity<String> {
        kafkaHealthService.addUnhealthyVote()
        LOG.info("Mottok selvmordsønske")
        return ResponseEntity("OK", HttpStatus.OK)
    }

}
