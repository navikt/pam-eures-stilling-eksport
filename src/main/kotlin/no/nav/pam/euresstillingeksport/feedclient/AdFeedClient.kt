package no.nav.pam.euresstillingeksport.feedclient

import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.service.StillingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AdFeedClient @Autowired constructor (
        @Value("\${pam-ad.url}")
        private val adApiURL : String,
        private val restTemplate : RestTemplate) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdFeedClient::class.java)

        private val retryTemplate = initRetryTemplate()

        private fun initRetryTemplate() : RetryTemplate {
            val retryTemplate = RetryTemplate()

            retryTemplate.setBackOffPolicy(ExponentialBackOffPolicy().apply {
                initialInterval = 100
                multiplier = 2.0
                maxInterval = 1000
            })
            retryTemplate.setThrowLastExceptionOnExhausted(true)
            retryTemplate.setRetryPolicy(SimpleRetryPolicy(3,
                    mapOf(IOException::class.java to true)))
            return retryTemplate
        }

    }


    fun getAd(uuid : String) : Ad =
        retryTemplate.execute<Ad, Exception>({getAdWrapped(uuid)})

    fun getAdWrapped(uuid : String) : Ad {
        try {
            val response = restTemplate.getForEntity("${adApiURL}/feed?uuid=${uuid}", FeedTransport::class.java)
            // TODO sjekk responskode og håndter feil på en grei nok måte
            return response.body?.content.orEmpty()[0]
        } catch (e: HttpStatusCodeException) {
            if (e.rawStatusCode >= 400 && e.rawStatusCode < 500) {
                LOG.error("Greide ikke å lese svar fra pam-ad ved uthenting av ad {}: {} " +
                        "HTTP feilkode: {}", uuid, e.message, e.rawStatusCode, e)
                throw IllegalArgumentException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            } else if (e.rawStatusCode >= 500) {
                LOG.info("pam-ad utilgjengelig/feiler ved uthenting av ad {}: {} " +
                        "HTTP feilkode: {}", uuid, e.message, e.rawStatusCode, e)
                throw IOException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            }
            throw e
        } catch (e: RestClientException) {
            if (e.cause is HttpMessageNotReadableException) {
                // Dette er en applikasjonsfeil som det ikke vil nytte å rekjøre, typisk pga feil i parsing av JSON
                LOG.error("Greide ikke å lese svar fra pam-ad ved uthenting av ad {}: {}", uuid, e.message, e)
                throw IllegalArgumentException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            }
            throw e
        }

    }

    fun hentPage(sistLest: LocalDateTime) : FeedTransport {
        return retryTemplate.execute<FeedTransport, Exception>({hentPageWrapped(sistLest)})
    }

    fun hentPageWrapped(sistLest: LocalDateTime) : FeedTransport {
        try {
            val uri = UriComponentsBuilder.fromUriString("${adApiURL}/feed")
                    .queryParam("size", 100)
                    .queryParam("sort", "updated,asc")
                    .queryParam("updatedSince", sistLest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build()
                    .toUri()
            val response = restTemplate.getForEntity(uri, FeedTransport::class.java)
            return response.body ?: throw IllegalArgumentException("Fikk ikke svar fra ad ved lesing av feed" +
                    " responskode: ${response.statusCodeValue}")
        } catch (e: HttpStatusCodeException) {
            if (e.rawStatusCode >= 400 && e.rawStatusCode < 500) {
                LOG.error("Greide ikke å lese svar fra pam-ad ved uthenting av feed siden {}: {} " +
                        "HTTP feilkode: {}", sistLest, e.message, e.rawStatusCode, e)
                throw IllegalArgumentException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            } else if (e.rawStatusCode >= 500) {
                LOG.info("pam-ad utilgjengelig/feiler ved uthenting av feed siden {}: {} " +
                        "HTTP feilkode: {}", sistLest, e.message, e.rawStatusCode, e)
                throw IOException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            }
            throw e
        } catch (e: RestClientException) {
            if (e.cause is HttpMessageNotReadableException) {
                // Dette er en applikasjonsfeil som det ikke vil nytte å rekjøre, typisk pga feil i parsing av JSON
                LOG.error("Greide ikke å lese svar fra pam-ad ved uthenting av feed siden {}: {}", sistLest, e.message, e)
                throw IllegalArgumentException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            }
            throw e
        }
    }

    @Component
    class FeedLeser(@Autowired private val feedClient: AdFeedClient,
                    @Autowired private val stillingService: StillingService,
                    @Autowired private val feedRepository: FeedRepository) {
        companion object {
            private val LOG = LoggerFactory.getLogger(FeedLeser::class.java)
        }
        @Scheduled(cron = "0 */1 * * * *")
        @SchedulerLock(name = "adFeedLock")
        fun lesFeed() {
            LOG.info("Poller ad feed for stillinger")
            val sistLest = feedRepository.hentFeedPeker()
            poll(sistLest)
        }

        fun poll(sistLest: LocalDateTime) {
            // Batchupdate kommer nok ikke til å overleve særlig lenge
            // det er en litt eksperimentell "to early optimization" som ikke ser ut til å optimalisere noe særlig...
            val brukBatchUpdate = true
            var nyeste = sistLest
            val trans = feedClient.hentPage(sistLest)
            LOG.info("Leste {} elementer fra feeden. Totalt {} sider",
                    trans.numberOfElements, trans.totalPages)

            if (brukBatchUpdate)
                stillingService.lagreStillinger(trans.content)

            trans.content.forEach {
                if (!brukBatchUpdate)
                    stillingService.lagreStilling(it)

                if (it.updated.isAfter(nyeste))
                    nyeste = it.updated
            }
            feedRepository.oppdaterFeedPeker(nyeste)

            if (!trans.last)
                poll(nyeste)
        }
    }

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
            var sistLestStr = sistLest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            jdbcTemplate.update("delete from feedpeker")
            jdbcTemplate.update("insert into feedpeker(sist_lest) values(?)",
                    sistLestStr)
        }
    }
}
