package no.nav.pam.euresstillingeksport

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.feedclient.AdFeedClient
import no.nav.pam.euresstillingeksport.feedclient.FeedTransport
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.AdStatus
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseJson
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseMetadata
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import no.nav.pam.euresstillingeksport.service.GetAllResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminApiTests {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper : ObjectMapper

    @Autowired
    lateinit var stillingRepository: StillingRepository

    @MockBean
    lateinit var adClient: AdFeedClient

    val apiRoot = "/input/api/jv/v0.1"
    val root = "/internal/admin"

    fun initAd() : Ad =
            objectMapper.readValue<FeedTransport>(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"),
                    FeedTransport::class.java)
                    .content[0]
                    .copy(uuid = UUID.randomUUID().toString(), status="ACTIVE")

    private fun toStillingsannonseJson(ad: Ad): StillingsannonseJson =
        StillingsannonseJson(StillingsannonseMetadata(ad.uuid, "test", AdStatus.ACTIVE, ad.created, ad.created, null),
                objectMapper.writeValueAsString(ad))

    @Test
    fun skalJustereFeedpeker() {
        val ad1 = initAd().copy(created = LocalDateTime.parse("2019-12-01T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        val ad2 = initAd().copy(created = LocalDateTime.parse("2019-12-03T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        stillingRepository.saveStillingsannonser(
                listOf(toStillingsannonseJson(ad1), toStillingsannonseJson(ad2))
        )

        var alleResponse = restTemplate.getForEntity("$apiRoot/getAll", GetAllResponse::class.java)
        Assertions.assertTrue(alleResponse.body!!.allReferences.size == 2)

        // Skru tilbake feedpeker uten å wipe database
        var feedpekerResponse = restTemplate.exchange("$root/feedpeker/2019-12-02T14:00:00", HttpMethod.PUT, HttpEntity.EMPTY,
                Any::class.java, emptyMap<String, String>())
        Assertions.assertEquals(200, feedpekerResponse.statusCodeValue)

        // Se at dette ikke har påvirket antall stillinger i databasen
        alleResponse = restTemplate.getForEntity("$apiRoot/getAll", GetAllResponse::class.java)
        Assertions.assertTrue(alleResponse.body!!.allReferences.size == 2)

        // Skru tilbake feedpeke, samt wipe database
        feedpekerResponse = restTemplate.exchange("$root/feedpeker/2019-12-02T14:00:00?wipeDb=true", HttpMethod.PUT, HttpEntity.EMPTY,
                Any::class.java, emptyMap<String, String>())
        Assertions.assertEquals(200, feedpekerResponse.statusCodeValue)

        // Se at dette har påvirket antall stillinger i databasen
        alleResponse = restTemplate.getForEntity("$apiRoot/getAll", GetAllResponse::class.java)
        Assertions.assertTrue(alleResponse.body!!.allReferences.size == 1)
    }
}
