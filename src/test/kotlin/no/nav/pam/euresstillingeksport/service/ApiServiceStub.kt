package no.nav.pam.euresstillingeksport.service

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.euresapi.*

class ApiServiceStub : ApiService {
    override fun getAll(): GetAllResponse {
        // Returner mock data inntil videre
        return GetAllResponse(listOf(
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        null,
                        "xref1",
                        "NAV",
                        EuresStatus.ACTIVE),
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:05:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:05:00"),
                        null,
                        "xref2",
                        "NAV",
                        EuresStatus.ACTIVE)
        ))
    }

    override fun getChanges(ts: Long): GetChangesResponse {
        // Returner mock data inntil videre
        return GetChangesResponse(listOf(
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        null,
                        "xref1",
                        "NAV",
                        EuresStatus.ACTIVE),
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:05:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:05:00"),
                        null,
                        "xref2",
                        "NAV",
                        EuresStatus.ACTIVE)),
                emptyList(),
                listOf(
                        Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:10:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T13:10:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T12:40:00"),
                                "xref3",
                                "NAV",
                                EuresStatus.CLOSED))
        )
    }

    override fun getDetails(referanser : List<String>): GetDetailsResponse {
        return GetDetailsResponse(
                mapOf(Pair("ref1", JvDetails("ref1",
                        "NAV", EuresStatus.ACTIVE,
                        "HR_OPEN XML her",
                        "1.0",
                        Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00")
                )),
                        Pair("ref2", JvDetails("ref2",
                                "NAV", EuresStatus.ACTIVE,
                                "HR_OPEN XML her",
                                "1.0",
                                Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00")
                        )))
        )
    }
}

