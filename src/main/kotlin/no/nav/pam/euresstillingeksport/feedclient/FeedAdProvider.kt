package no.nav.pam.euresstillingeksport.feedclient

import no.nav.pam.euresstillingeksport.model.Ad
import org.elasticsearch.common.inject.name.Named
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service("pam-ad-feed-provider")
class FeedAdProvider (
        @Value("\${pam-ad.url}")
        private val adApiURL : String,
        private val restTemplate : RestTemplate
): AdProvider  {

    companion object {
        private val LOG = LoggerFactory.getLogger(FeedAdProvider::class.java)
    }
    override fun `fetch updated after`(sistLest: LocalDateTime): FeedTransport {
        try {
            val uri = UriComponentsBuilder.fromUriString("${adApiURL}/feed")
                    .queryParam("size", 150)
                    .queryParam("sort", "updated,asc")
                    .queryParam("updatedSince", sistLest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build()
                    .toUri()
            val response = restTemplate.getForEntity(uri, FeedTransport::class.java)
            return response.body ?: throw IllegalArgumentException("Fikk ikke svar fra ad ved lesing av feed" +
                    " responskode: ${response.statusCodeValue}")
        } catch (e: HttpStatusCodeException) {
            if (e.rawStatusCode in 400..499) {
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

    override fun fetch(uuid: String): Ad {
        try {
            val response = restTemplate.getForEntity("${adApiURL}/feed?uuid=${uuid}", FeedTransport::class.java)
            // TODO sjekk responskode og håndter feil på en grei nok måte
            return response.body?.content.orEmpty()[0]
        } catch (e: HttpStatusCodeException) {
            if (e.rawStatusCode in 400..499) {
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
}