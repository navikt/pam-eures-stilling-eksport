package no.nav.pam.euresstillingeksport.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ConvertersTest {
    @Test
    fun skalKonvertereLocalDateTilMillisEpoch() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val nowMillis = Converters.localdatetimeToTimestamp(now)

        val konvertert = Converters.timestampToLocalDateTime(nowMillis)
        Assertions.assertEquals(now, konvertert)
    }

    @Test
    fun skalKonvertereIsoTidspunktTilMillisEpoch() {
        val tidspunkt = "2019-10-01T10:30:15"
        val tidspunktMs = Converters.isoDatetimeToTimestamp(tidspunkt)
        val tidspunktLdt = Converters.timestampToLocalDateTime(tidspunktMs)

        Assertions.assertEquals(tidspunkt, tidspunktLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
}