package no.nav.pam.euresstillingeksport

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.feedclient.FeedTransport
import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.StillingService
import no.nav.pam.euresstillingeksport.euresapi.EuresStatus
import no.nav.pam.euresstillingeksport.euresapi.GetAllResponse
import no.nav.pam.euresstillingeksport.euresapi.GetChangesResponse
import no.nav.pam.euresstillingeksport.euresapi.GetDetailsResponse
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimestampTests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Autowired
	lateinit var stillingRepository: StillingRepository

	@Autowired
	lateinit var objectMapper : ObjectMapper

	@Autowired
	lateinit var stillingService : StillingService

	@MockBean
	lateinit var adClient: AdFeedClient

	val root = "/input/api/jv/v0.1"

	fun initAd() : Ad =
		objectMapper.readValue<FeedTransport>(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"),
			FeedTransport::class.java)
				.content[0]
			.copy(uuid = UUID.randomUUID().toString(), status="ACTIVE")

	@BeforeEach
	fun cleanDb() {
		stillingRepository.slettNyereEnn(LocalDateTime.parse("1970-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
	}

	/*
	Dette testcaset står beskrevet i EURES Functional message exchange specifications new regulation v1.3.2
	Kapittel 2.2.1, eksempel 1-4
	 */
	@Test
	fun skalHandtereTimestamps() {
		// Eksempel 1
		val now = Converters.localdatetimeToTimestamp(LocalDateTime.now())
		val ad = initAd()
		stillingService.lagreStillinger(listOf(ad))

		val ex1Response = restTemplate.getForEntity("$root/getAll", GetAllResponse::class.java)
		Assertions.assertTrue(ex1Response.body!!.allReferences[0].creationTimestamp >= now)

		// Tester at også getChanges er i samsvar med eksempelet
		var nyereEnn = now
		val ex1ResponseChanges = restTemplate.getForEntity("$root/getChanges/$nyereEnn", GetChangesResponse::class.java)
		Assertions.assertTrue(ex1ResponseChanges.body!!.createdReferences[0].creationTimestamp >= now)

		// Eksempel 2: Annonsen fra eksempel 1 gjøres utilgjengelig i det nasjonale systemet. Da skal den
		// fremstå som lukket for EURES, og ikke finnes i getAll
		nyereEnn = Converters.localdatetimeToTimestamp(LocalDateTime.now())
		val adEx2 = ad.copy(status="INACTIVE")
		stillingService.lagreStillinger(listOf(adEx2))
		val ex2Response = restTemplate.getForEntity("$root/getAll", GetAllResponse::class.java)
		Assertions.assertTrue(ex2Response.body!!.allReferences.isEmpty())

		val ex2ResponseDetaljer = restTemplate.postForEntity("$root/getDetails",
				listOf(adEx2.uuid), GetDetailsResponse::class.java)
		val ex2Ad = ex2ResponseDetaljer.body!!.details[adEx2.uuid]!!
		Assertions.assertNotNull(ex2Ad.closingTimestamp)
		Assertions.assertTrue(ex2Ad.status == EuresStatus.CLOSED)

		// Tester at også getChanges er i samsvar med eksempelet
		val ex2ResponseChanges = restTemplate.getForEntity("$root/getChanges/$nyereEnn", GetChangesResponse::class.java)
		Assertions.assertTrue(ex2ResponseChanges.body!!.closedReferences[0]!!.closingTimestamp!! >= nyereEnn)

		// Eksempel 3: Annonsen fra eksempel 2 blir igjen tilgjengelig i det nasjonale systemet.
		// Da skal den fremstå med nytt creation timestamp for EURES
		val adEx3 = adEx2.copy(status="ACTIVE")
		Thread.sleep(1L)
		nyereEnn = Converters.localdatetimeToTimestamp(LocalDateTime.now())
		stillingService.lagreStillinger(listOf(adEx3))
		val ex3ResponseDetaljer = restTemplate.postForEntity("$root/getDetails",
				listOf(adEx3.uuid), GetDetailsResponse::class.java)
		val ex3Ad = ex3ResponseDetaljer.body!!.details[adEx3.uuid]!!
		Assertions.assertNull(ex3Ad.closingTimestamp)
		Assertions.assertTrue(ex3Ad.creationTimestamp > ex2Ad.creationTimestamp)
		Assertions.assertTrue(ex3Ad.status == EuresStatus.ACTIVE)

		// Tester at også getChanges er i samsvar med eksempelet
		val ex3ResponseChanges = restTemplate.getForEntity("$root/getChanges/$nyereEnn", GetChangesResponse::class.java)
		Assertions.assertTrue(ex3ResponseChanges.body!!.createdReferences[0]!!.creationTimestamp >= nyereEnn)

		// Eksempel 4: (eksempelet motsier annet som er sagt om modification timestamp, så her tolker jeg litt)
		val adEx4 = adEx3.copy(medium = UUID.randomUUID().toString())
		Thread.sleep(1L)
		nyereEnn = Converters.localdatetimeToTimestamp(LocalDateTime.now())
		stillingService.lagreStillinger(listOf(adEx4))
		val ex4ResponseDetaljer = restTemplate.postForEntity("$root/getDetails",
				listOf(adEx4.uuid), GetDetailsResponse::class.java)
		val ex4Ad = ex4ResponseDetaljer.body!!.details[adEx4.uuid]!!
		Assertions.assertTrue(ex4Ad.creationTimestamp == ex3Ad.creationTimestamp)
		Assertions.assertTrue(ex4Ad.lastModificationTimestamp!! > ex3Ad.lastModificationTimestamp!!)

		// Tester at også getChanges er i samsvar med eksempelet
		val ex4ResponseChanges = restTemplate.getForEntity("$root/getChanges/$nyereEnn", GetChangesResponse::class.java)
		Assertions.assertTrue(ex4ResponseChanges.body!!.modifiedReferences[0].lastModificationTimestamp >= nyereEnn)
	}
}
