package no.nav.pam.euresstillingeksport.feedclient

import no.nav.pam.euresstillingeksport.model.Converters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class FeedRepository(@Autowired private val jdbcTemplate: JdbcTemplate) {

    fun hentFeedPeker(): LocalDateTime {
        try {
            val sistLest = jdbcTemplate.queryForObject("select sist_lest from feedpeker",
                    arrayOf(), String::class.java)
            return LocalDateTime.parse(sistLest, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        } catch (e: EmptyResultDataAccessException) {
            return Converters.timestampToLocalDateTime(0)
        }
    }

    fun oppdaterFeedPeker(sistLest: LocalDateTime) {
        val sistLestStr = sistLest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        jdbcTemplate.update("delete from feedpeker")
        jdbcTemplate.update("insert into feedpeker(sist_lest) values(?)",
                sistLestStr)
    }
}