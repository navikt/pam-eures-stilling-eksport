package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.service.StillingService
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles

/**
 * Dette er en integrasjonstest som går mot pam-ad i q. Denne testen vil aldri kunne kjøre på github, og skal derfor ignoreres
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = ["pam-ad.url=https://pam-ad.nais.oera-q.local/api/v1/ads"])
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Integrasjonstest som ikke kjører på github")
class PamAdITests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Autowired
	lateinit var adClient: AdFeedClient

	@Autowired
	lateinit var stillingService: StillingService

	val root = "/input/api/jv/v0.1"

	@TestConfiguration
	class TestConfig : ApiTestConfiguration() {
	}


	@BeforeAll
	fun disableSSLChecks() {
		ApiTestConfiguration.disableSSLChecksDefaultHttpClient()
	}

	@Test
	fun skaHenteAd() {
		val ad = adClient.getAd("db6cc067-7f39-42f1-9866-d9ee47894ec6")
		Assertions.assertEquals("INACTIVE", ad.status)
	}

	@Test
	fun populerDb() {
		val ad = adClient.getAd("db6cc067-7f39-42f1-9866-d9ee47894ec6")
		stillingService.lagreStillinger(listOf(ad.copy(status="ACTIVE")))
		val stillingsannonse = stillingService.hentStillingsannonse(ad.uuid)
		Assertions.assertNotNull(stillingsannonse?.ad)
	}
}
