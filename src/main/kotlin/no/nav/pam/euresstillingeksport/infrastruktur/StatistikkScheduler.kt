package no.nav.pam.euresstillingeksport.infrastruktur

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.pam.euresstillingeksport.model.StillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class StatistikkScheduler (@Autowired private val meterRegistry: MeterRegistry,
                           @Autowired private val stillingService: StillingService){
    private val statistikkMetrikker : Map<String, AtomicLong?> =
            listOf("DELETED", "STOPPED", "ACTIVE", "REJECTED", "INACTIVE")
                    .map {
                        it to meterRegistry.gauge("pam.eures.stilling.antall", Tags.of("status", it), AtomicLong(0))
                    }
                    .toMap()

    @Scheduled(cron = "0 */2 * * * *")
    fun genererStatistikkMetrikker() {
        val annonseStatistikk = stillingService.hentStatistikk(null)
        annonseStatistikk.forEach {
            statistikkMetrikker[it.status]?.set(it.antall)
        }
    }
}