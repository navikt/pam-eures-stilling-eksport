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
class StatusController(@Autowired private val repo: StillingRepository) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @RequestMapping("/isAlive")
    public fun isAlive(@Autowired topicBridgeHealthService: KafkaHealthService): ResponseEntity<String> {
        try {
            repo.findStillingsannonserByIds(listOf("finnes_ikke"))
        } catch (e: Exception) {
            LOG.info("Failed to connect to database", e)
            return ResponseEntity("Not OK", HttpStatus.SERVICE_UNAVAILABLE)
        }

        if (topicBridgeHealthService.isHealthy())
            ResponseEntity("OK", HttpStatus.OK)
        else
            ResponseEntity("Not OK", HttpStatus.SERVICE_UNAVAILABLE)


        return ResponseEntity("OK", HttpStatus.OK)
    }

    @RequestMapping("/isReady")
    public fun isReady(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)

    @RequestMapping("/diediedie")
    fun diediedie(@Autowired topicBridgeHealthService: KafkaHealthService): ResponseEntity<String> {
        topicBridgeHealthService.addUnhealthyVote()
        LOG.info("Mottok selvmordsønske")
        return ResponseEntity("OK", HttpStatus.OK)
    }

}
