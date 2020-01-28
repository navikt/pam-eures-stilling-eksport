package no.nav.pam.euresstillingeksport.euresapi

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.StillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class AdApiService(@Autowired private val stillingService: StillingService,
                   @Autowired private val meterRegistry: MeterRegistry) : ApiService {

    private val euresLagMeter = meterRegistry.gauge("eures.lag.hours", AtomicInteger(0))
    private val euresLastPollMeter = mapOf<String, AtomicInteger?>(
            Pair("Opprettet", meterRegistry.gauge("eures.last.poll", Tags.of("type", "Opprettet"), AtomicInteger(0))),
            Pair("Endret", meterRegistry.gauge("eures.last.poll", Tags.of("type", "Endret"), AtomicInteger(0))),
            Pair("Lukket", meterRegistry.gauge("eures.last.poll", Tags.of("type", "Lukket"), AtomicInteger(0))))

    /** Kun referanser til aktive stillingsannonser skal returneres */
    override fun getAll(): GetAllResponse {
        val stillingsannonser = stillingService.hentAlleAktiveStillinger()

        return GetAllResponse(stillingsannonser.map {
            Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                    Converters.localdatetimeToTimestamp(it.sistEndretTs),
                    it.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts) } ?: null,
                    it.id,
                    it.kilde,
                    EuresStatus.fromAdStatus(it.status))
        })
    }

    override fun getChanges(ts: Long): GetChangesResponse {
        val lagInHours = (System.currentTimeMillis() - ts)/3600000
        euresLagMeter?.set(lagInHours.toInt())

        val stillingsannonser = stillingService.hentAlleStillinger(ts)

        val opprettet = ArrayList<Stillingreferanse>()
        val endret = ArrayList<Stillingreferanse>()
        val lukket = ArrayList<Stillingreferanse>()

        stillingsannonser.forEach {
            val stillingreferanse = Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                    Converters.localdatetimeToTimestamp(it.sistEndretTs),
                    it.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts) } ?: null,
                    it.id,
                    it.kilde,
                    EuresStatus.fromAdStatus(it.status))
            if (it.lukketTs != null) {
                lukket.add(stillingreferanse)
            } else if (stillingreferanse.creationTimestamp == stillingreferanse.lastModificationTimestamp) {
                opprettet.add(stillingreferanse)
            } else {
                endret.add(stillingreferanse)
            }
        }
        euresLastPollMeter["Opprettet"]?.set(opprettet.size)
        euresLastPollMeter["Endret"]?.set(endret.size)
        euresLastPollMeter["Lukket"]?.set(lukket.size)

        return GetChangesResponse(opprettet, endret, lukket)
    }

    override fun getDetails(referanser : List<String>): GetDetailsResponse {
        val stillingsannonser = stillingService.hentStillingsannonser(referanser)

        return GetDetailsResponse(stillingsannonser.associateBy({ it.stillingsannonseMetadata.id },
                {
                    JvDetails(
                            reference = it.stillingsannonseMetadata.id,
                            source = it.stillingsannonseMetadata.kilde,
                            status = EuresStatus.fromAdStatus(it.stillingsannonseMetadata.status),
                            content = toXML(it.ad.convertToPositionOpening()),
                            creationTimestamp = Converters.localdatetimeToTimestamp(it.stillingsannonseMetadata.opprettetTs),
                            lastModificationTimestamp = Converters.localdatetimeToTimestamp(it.stillingsannonseMetadata.sistEndretTs),
                            closingTimestamp = it.stillingsannonseMetadata.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts) }
                                    ?: null
                    )
                }))
    }

    fun toXML(positionOpening: PositionOpening): String {
        return HrxmlSerializer.serialize(positionOpening)
    }
}

