package no.nav.pam.euresstillingeksport.rest

import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.model.pam.convertToPositionOpening
import no.nav.pam.euresstillingeksport.service.StillingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class StatusController(@Autowired private val feedClient: AdFeedClient,
                       @Autowired private val stillingService: StillingService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @RequestMapping("/isAlive")
    public fun isAlive(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)

    @RequestMapping("/isReady")
    public fun isReady(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)

    @RequestMapping("/rekjor/{uuid}")
    public fun rekjor(uuid: String): ResponseEntity<String> {
        LOG.info("Henter inn annonse med id {} fra pam-ad på nytt.", uuid)
        val ad = feedClient.getAd(uuid)

        // Det er litt dårlig karma å sjekke forretningslogikk her som kun burde ligge i stillingService,
        // men det er verd det for å kunne i en ordentlig feilmelding (alternativet er "sjekk loggen")
        try {
            ad.convertToPositionOpening()
        } catch (e: Exception) {
            return ResponseEntity("Annonsen ble ikke importer pga av den ikke " +
                    "kan konverteres til Euresformat: ${e.message}", HttpStatus.BAD_REQUEST)
        }
        val antallModifiserteStillinger = stillingService.lagreStillinger(listOf(ad))

        if (antallModifiserteStillinger > 0) {
            return ResponseEntity("Annonsen ble oppdatert.", HttpStatus.OK)
        } else {
            return ResponseEntity("Annonsen ble ikke oppdatert i databasen. Sjekk loggene for å finne ut hvorfor.",
                    HttpStatus.BAD_REQUEST)
        }
    }
}
