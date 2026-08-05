package no.nav.pam.euresstillingeksport.infrastruktur

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.pam.euresstillingeksport.model.StillingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class StatistikkScheduler(private val meterRegistry: MeterRegistry,
                          private val stillingService: StillingService) {
    private val statistikkMetrikker : Map<String, AtomicLong?> =
            listOf("DELETED", "STOPPED", "ACTIVE", "REJECTED", "INACTIVE", "FlaggetAktiv")
                    .map {
                        it to meterRegistry.gauge("pam.eures.stilling.antall", Tags.of("status", it), AtomicLong(0))
                    }
                    .toMap()

    @Scheduled(cron = "0 */2 * * * *")
    @SchedulerLock(
        name = "StatistikkScheduler.genererStatistikkMetrikker",
        lockAtMostFor = "PT10M",
        lockAtLeastFor = "PT30S"
    )
    fun genererStatistikkMetrikker() {
        LockAssert.assertLocked()
        val annonseStatistikk = stillingService.hentStatistikk(null)
        annonseStatistikk.forEach {
            statistikkMetrikker[it.status]?.set(it.antall)
        }
    }
}