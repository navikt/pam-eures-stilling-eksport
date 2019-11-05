package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.service.GetAllResponse
import no.nav.pam.euresstillingeksport.service.GetChangesResponse
import no.nav.pam.euresstillingeksport.service.GetDetailsResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Dette er en integrasjonstest som går mot pam-ad i q. Denne testen vil aldri kunne kjøre på github, og skal derfor ignoreres
 * Hvis man skal kjøre denne desten må man enten sette opp en testpipeline som deployer til q (ganske uaktuelt) eller
 * lage en egen test som mocker pam-ad (litt mer sannsynlig - men da er det ikke en ordentlig integrasjonstest lenger)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Disabled("Integrasjonstest som ikke kjører på github")
class PamAdITests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Autowired
	lateinit var adClient: AdFeedClient

	val root = "/input/api/jv/v0.1"

	@TestConfiguration
	class TestConfig {
		@Bean
		open fun restTemplate(@Autowired restTemplateBuilder: RestTemplateBuilder): RestTemplate {
			// Vi ignorerer SSL feil når vi tester mot preprod/q
			val trustManager: X509TrustManager = object: X509TrustManager {
				override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
				}
				override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
				}
				override fun getAcceptedIssuers(): Array<X509Certificate> =
						emptyArray()
			}

			val sslContext: SSLContext = SSLContext.getInstance("TLS")
			sslContext.init(null, arrayOf(trustManager), SecureRandom())

			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
			HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }

			return restTemplateBuilder.build()
		}
	}


	@Test
	fun skaHenteAd() {
		val ad = adClient.getAd("db6cc067-7f39-42f1-9866-d9ee47894ec6")
		Assertions.assertEquals("INACTIVE", ad.status)
	}
}
