package no.nav.pam.euresstillingeksport.administration

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
class AdminApiController(@Autowired private val stillingService: StillingService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdminApiController::class.java)
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

}
