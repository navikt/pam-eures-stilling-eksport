package no.nav.pam.euresstillingeksport.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Converters {
    @JvmStatic
    fun localdatetimeToTimestamp(ldt: LocalDateTime): Long =
        ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    @JvmStatic
    fun isoDatetimeToTimestamp(isoDatetime : String): Long =
        localdatetimeToTimestamp(
                LocalDateTime.parse(isoDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME))

    @JvmStatic
    fun timestampToLocalDateTime(ts: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC)

}