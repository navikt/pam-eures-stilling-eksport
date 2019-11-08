package no.nav.pam.euresstillingeksport

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.service.ApiServiceStub
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import java.io.IOException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PamAdTests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Autowired
	lateinit var adClient: AdFeedClient

	val root = "/input/api/jv/v0.1"
	var wireMockServer : WireMockServer? = null

	@TestConfiguration
	class TestConfig : ApiTestConfiguration() {
		@Bean
		fun apiService() =
				ApiServiceStub()
	}


	@BeforeAll
	fun initWiremock() {
		wireMockServer = ApiTestConfiguration.wiremockServer()
		wireMockServer!!.start()
	}

	@AfterAll
	fun shutdownWiremock() {
		wireMockServer!!.stop()
	}

	@Test
	fun skaHenteAd() {
		val ad = adClient.getAd("db6cc067-7f39-42f1-9866-d9ee47894ec6")
		Assertions.assertEquals("INACTIVE", ad.status)
	}

	@Test
	fun skaHaandtereNotFound() {
		Assertions.assertThrows(IllegalArgumentException::class.java) {
			adClient.getAd("not_found")
		}
	}

	@Test
	fun skaHaandtereUgyldigJSONRespons() {
		Assertions.assertThrows(IllegalArgumentException::class.java) {
			adClient.getAd("ad-ugyldig_json")
		}
	}

	@Test
	fun skalHaandtereBadRequest() {
		Assertions.assertThrows(IllegalArgumentException::class.java) {
			adClient.getAd("bad_request")
		}
	}

	@Test
	fun skaHaandtereServerUnavailable() {
		Assertions.assertThrows(IOException::class.java) {
			adClient.getAd("service_unavailable")
		}
	}
}
