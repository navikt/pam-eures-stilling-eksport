package no.nav.pam.euresstillingeksport

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.AdStatus
import no.nav.pam.euresstillingeksport.model.StillingsannonseJson
import no.nav.pam.euresstillingeksport.model.StillingsannonseMetadata
import no.nav.pam.euresstillingeksport.euresapi.GetAllResponse
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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

    val root = "/internal/admin"

    fun initAd() : Ad =
            objectMapper.readValue<Ad>(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"),
                    Ad::class.java)
                    .copy(uuid = UUID.randomUUID().toString(), status="ACTIVE")

    private fun toStillingsannonseJson(ad: Ad): StillingsannonseJson =
            StillingsannonseJson(StillingsannonseMetadata(ad.uuid, "test", AdStatus.ACTIVE, false, ad.created, ad.created, null),
                    objectMapper.writeValueAsString(ad))

    @BeforeEach
    fun cleanDb() {
        stillingRepository.slettNyereEnn(LocalDateTime.parse("1970-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    @Test
    fun skalHenteStatistikk() {
        val ad1 = initAd().copy(created = LocalDateTime.parse("2019-12-01T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        val ad2 = initAd().copy(created = LocalDateTime.parse("2019-12-03T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        stillingRepository.saveStillingsannonser(
                listOf(toStillingsannonseJson(ad1), toStillingsannonseJson(ad2))
        )

        var statistikkResponse = restTemplate.getForEntity("$root/statistikk", Any::class.java)
    }
}
