package no.nav.pam.euresstillingeksport.service

import no.nav.pam.euresstillingeksport.model.Converters
import org.springframework.stereotype.Service

@Service
class ApiService {
    fun getAll(): GetAllResponse {
        // Returner mock data inntil videre
        return GetAllResponse(listOf(
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        null,
                        "xref1",
                        "NAV",
                        "ACTIVE"),
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:05:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:05:00"),
                        null,
                        "xref2",
                        "NAV",
                        "ACTIVE")
                ))
    }

    fun getChanges(ts: Long): GetChangesResponse {
        // Returner mock data inntil videre
        return GetChangesResponse(listOf(
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:00:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:00:00"),
                        null,
                        "xref1",
                        "NAV",
                        "ACTIVE"),
                Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:05:00"),
                        Converters.isoDatetimeToTimestamp("2019-01-11T13:05:00"),
                        null,
                        "xref2",
                        "NAV",
                        "ACTIVE")),
                emptyList(),
                listOf(
                        Stillingreferanse(Converters.isoDatetimeToTimestamp("2019-01-11T12:10:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T13:10:00"),
                                Converters.isoDatetimeToTimestamp("2019-01-11T12:40:00"),
                                "xref3",
                                "NAV",
                                "CLOSED"))
                )
    }

    fun getDetails(referanser : List<String>): GetDetailsResponse {
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

