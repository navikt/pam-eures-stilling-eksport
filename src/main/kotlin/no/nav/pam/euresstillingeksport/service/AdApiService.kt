package no.nav.pam.euresstillingeksport.service

import no.nav.pam.euresstillingeksport.model.Converters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AdApiService(@Autowired private val stillingService: StillingService) : ApiService {
    override fun getAll(): GetAllResponse {
        val stillingsannonser = stillingService.hentAlleAktiveStillinger()

        return GetAllResponse(stillingsannonser.map {
            Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                    Converters.localdatetimeToTimestamp(it.sistEndretTs),
                    it.varerTilTs?.let {ts -> Converters.localdatetimeToTimestamp(ts)} ?: null,
                    it.id,
                    "NAV",
                    it.status)
        })
    }

    override fun getChanges(ts: Long): GetChangesResponse {
        val stillingsannonser = stillingService.hentAlleAktiveStillinger(ts)

        // Vi må gå opp livssyklusen til stillingsannonser og tilhørende statuser slik at
        // vi får satt riktige timestamps
        val opprettet = ArrayList<Stillingreferanse>()
        val endret = ArrayList<Stillingreferanse>()
        val lukket = ArrayList<Stillingreferanse>()

        return GetChangesResponse(stillingsannonser.map {
            Stillingreferanse(Converters.localdatetimeToTimestamp(it.opprettetTs),
                    Converters.localdatetimeToTimestamp(it.sistEndretTs),
                    it.varerTilTs?.let { ts -> Converters.localdatetimeToTimestamp(ts)} ?: null,
                    it.id,
                    "NAV",
                    it.status)
        }, // TODO finn ut av kravene for modified og closed lista
                emptyList(), emptyList())
    }

    override fun getDetails(referanser : List<String>): GetDetailsResponse {
        stillingService.hentStillingsannonser(referanser)
        return GetDetailsResponse(
                mapOf(Pair("ref1", JvDetails("ref1",
                        "NAV", "ACTIVE",
                        "HR_OPEN XML her",
                        "1.0",
                        Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00")
                        )),
                Pair("ref2", JvDetails("ref2",
                        "NAV", "ACTIVE",
                        "HR_OPEN XML her",
                        "1.0",
                        Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00")
                )))
        )
    }
}

