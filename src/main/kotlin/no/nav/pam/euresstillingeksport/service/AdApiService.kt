package no.nav.pam.euresstillingeksport.service

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.pam.convertToPositionOpening
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AdApiService(@Autowired private val stillingService: StillingService) : ApiService {
    /** Kun referanser til aktive stillingsannonser skal returneres */
    override fun getAll(): GetAllResponse {
        val stillingsannonser = stillingService.hentAlleAktiveStillinger()

        return GetAllResponse(stillingsannonser.map {
            Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                    Converters.localdatetimeToTimestamp(it.sistEndretTs),
                    it.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts)} ?: null,
                    it.id,
                    it.kilde,
                    EuresStatus.fromAdStatus(it.status))
        })
    }

    override fun getChanges(ts: Long): GetChangesResponse {
        val stillingsannonser = stillingService.hentAlleStillinger(ts)

        val opprettet = ArrayList<Stillingreferanse>()
        val endret = ArrayList<Stillingreferanse>()
        val lukket = ArrayList<Stillingreferanse>()

        stillingsannonser.forEach {
            val stillingreferanse = Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                Converters.localdatetimeToTimestamp(it.sistEndretTs),
                it.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts)} ?: null,
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
        return GetChangesResponse(opprettet, endret, lukket)
    }

    override fun getDetails(referanser : List<String>): GetDetailsResponse {
        val stillingsannonser = stillingService.hentStillingsannonser(referanser)

        return GetDetailsResponse(stillingsannonser.associateBy({ it.stillingsannonseMetadata.id },
                {
                    JvDetails(it.stillingsannonseMetadata.id,
                            it.stillingsannonseMetadata.kilde,
                            EuresStatus.fromAdStatus(it.stillingsannonseMetadata.status),
                            it.ad.convertToPositionOpening().toString(), // Dette er ment å skulle returnere HR-XML...
                            "1.0",
                            Converters.localdatetimeToTimestamp(it.stillingsannonseMetadata.opprettetTs),
                            Converters.localdatetimeToTimestamp(it.stillingsannonseMetadata.sistEndretTs),
                            it.stillingsannonseMetadata.lukketTs?.let { ts -> Converters.localdatetimeToTimestamp(ts) } ?: null
                    )
                }))
    }
}

