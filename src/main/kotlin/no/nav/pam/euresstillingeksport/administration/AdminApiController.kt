package no.nav.pam.euresstillingeksport.administration

import no.nav.pam.euresstillingeksport.model.StillingService
import no.nav.pam.euresstillingeksport.repository.AnnonseStatistikk
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping(AdminApiController.BASE_PATH)
class AdminApiController(private val stillingService: StillingService) {

    companion object {
        private const val BASE_PATH = "/internal/admin"
    }

    @GetMapping("/statistikk")
    fun statistikk() : ResponseEntity<List<AnnonseStatistikk>> {
        val statistikk = stillingService.hentStatistikk(null)
        return ResponseEntity.ok(statistikk)
    }

    @GetMapping("/statistikk/{fraOgMed}")
    fun statistikk(@PathVariable("fraOgMed") fraOgMed: String) : ResponseEntity<List<AnnonseStatistikk>> {
        val fom = LocalDateTime.parse(fraOgMed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val statistikk = stillingService.hentStatistikk(fom)
        return ResponseEntity.ok(statistikk)
    }

}
