package no.nav.pam.euresstillingeksport.administration

import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.euresapi.convertToPositionOpening
import no.nav.pam.euresstillingeksport.model.StillingService
import no.nav.pam.euresstillingeksport.repository.AnnonseStatistikk
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/internal/admin")
class AdminApiController(@Autowired private val feedClient: AdFeedClient,
                         @Autowired private val feedLeser: AdFeedClient.FeedLeser,
                         @Autowired private val stillingService: StillingService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdminApiController::class.java)
    }


    @GetMapping("/rekjor/{uuid}")
    fun rekjor(@PathVariable("uuid") uuid: String): ResponseEntity<String> {
        LOG.info("Henter inn annonse med id {} fra pam-ad på nytt.", uuid)
        val ad = feedClient.getAd(uuid)

        // Det er litt dårlig karma å sjekke forretningslogikk her som kun burde ligge i stillingService,
        // men det er verd det for å kunne gi en ordentlig feilmelding (alternativet er "sjekk loggen")
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

    @GetMapping("/statistikk")
    fun statistikk() : ResponseEntity<List<AnnonseStatistikk>> {
        val statistikk = stillingService.hentStatistikk(null)
        return ResponseEntity(statistikk, HttpStatus.OK)
    }

    @GetMapping("/statistikk/{fraOgMed}")
    fun statistikk(@PathVariable("fraOgMed") fraOgMed: String) : ResponseEntity<List<AnnonseStatistikk>> {
        val fom = LocalDateTime.parse(fraOgMed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val statistikk = stillingService.hentStatistikk(fom)
        return ResponseEntity(statistikk, HttpStatus.OK)
    }

    @GetMapping("/feedpeker")
    fun feedpeker() : ResponseEntity<String> {
        val sistlest = feedLeser.feedpeker()
        return ResponseEntity(sistlest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), HttpStatus.OK)
    }

    @PutMapping("/feedpeker/{sistLest}")
    fun feedpeker(@PathVariable("sistLest") sistLest: String,
                  @RequestParam("wipeDb", required = false, defaultValue = "false") wipeDb: Boolean) : ResponseEntity<String> {
        val ldt = LocalDateTime.parse(sistLest, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        feedLeser.feedpeker(ldt, wipeDb)
        return ResponseEntity("", HttpStatus.OK)
    }

}
