package no.nav.pam.euresstillingeksport.feedclient

import no.nav.pam.euresstillingeksport.model.pam.Ad
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class AdFeedClient @Autowired constructor (
        @Value("\${pam-ad.url}")
        private val adApiURL : String,
        private val restTemplate : RestTemplate) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdFeedClient::class.java)
    }


    fun getAd(uuid : String) : Ad {
        try {
            val response = restTemplate.getForEntity("${adApiURL}/feed?uuid=${uuid}", FeedTransport::class.java)
            // TODO sjekk responskode og håndter feil på en grei nok måte
            return response.body?.content.orEmpty()[0]
        } catch (e: RestClientException) {
            if (e.cause is HttpMessageNotReadableException) {
                // Dette er en applikasjonsfeil som det ikke vil nytte å rekjøre, typisk pga feil i parsing av JSON
                LOG.error("Greide ikke å lese svar fra pam-ad ved uthenting av ad {}: {}", uuid, e.message, e)
                throw IllegalArgumentException("Greide ikke å lese respons fra pam-ad: ${e.message}", e)
            }
            // TODO det er mer som skal håndteres her...
            throw e
        } catch (e: Exception) {
            // TODO vi må ha ordentlig feilhåndtering, og der har ikke catch all en rolle
            LOG.error("Dette var ikke helt som forventet", e)
            throw e
        }

    }
}
