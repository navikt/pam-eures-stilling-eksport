package no.nav.pam.euresstillingeksport

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.feedclient.FeedTransport
import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.euresapi.convertToPositionOpening
import no.nav.pam.euresstillingeksport.model.StillingService
import no.nav.pam.euresstillingeksport.euresapi.AdApiService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KonverteringTests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Autowired
	lateinit var objectMapper : ObjectMapper

	@Autowired
	lateinit var stillingService : StillingService

	@Autowired
	lateinit var adApi : AdApiService

	@MockBean
	lateinit var adClient: AdFeedClient

	val root = "/input/api/jv/v0.1"

	fun initAd() : Ad =
		objectMapper.readValue<FeedTransport>(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"),
			FeedTransport::class.java)
				.content[0]
			.copy(uuid = UUID.randomUUID().toString(), status="ACTIVE")

	@Test
	fun skalKonvertereTilPositionOpening() {
		val now = Converters.localdatetimeToTimestamp(LocalDateTime.now())
		val ad = initAd()
		val po = ad.convertToPositionOpening()
		// Mulig vi trenger *litt* mer testing på om vi konverterer til riktig HR-XML
		Assertions.assertTrue(po.positionOpeningStatusCode.name == "Active")
		//System.out.println(adApi.toXML(po))

	}
}
