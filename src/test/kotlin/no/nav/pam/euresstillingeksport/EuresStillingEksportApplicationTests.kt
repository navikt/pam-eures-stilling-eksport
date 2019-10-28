package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.service.GetAllResponse
import no.nav.pam.euresstillingeksport.service.GetChangesResponse
import no.nav.pam.euresstillingeksport.service.GetDetailsResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EuresStillingEksportApplicationTests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	val root = "/input/api/jv/v0.1"

	@Test
	fun contextLoads() {
	}

	@Test
	fun skalSvarePaaPing() {
		val pong = restTemplate.getForEntity("${root}/ping", String::class.java)
		Assertions.assertTrue(pong.statusCodeValue == 200)
		Assertions.assertTrue(pong.body!!.startsWith("Hello from Input API"))
	}

	@Test
	fun skalSvarePaaGetAll() {
		val response = restTemplate.getForEntity("${root}/getAll", GetAllResponse::class.java)
		Assertions.assertTrue(response.statusCodeValue == 200)
		Assertions.assertNotNull(response.body)
		Assertions.assertNotNull(response.body!!.allReferences)
	}

	@Test
	fun skalSvarePaaGetChanges() {
		val ts = Converters.isoDatetimeToTimestamp("2019-10-01T12:00:00")
		val response = restTemplate.getForEntity("${root}/getChanges/${ts}", GetChangesResponse::class.java)
		Assertions.assertTrue(response.statusCodeValue == 200)
		Assertions.assertNotNull(response.body)
		Assertions.assertNotNull(response.body!!.modifiedReferences)
	}

	@Test
	fun skalSvarePaaGetDetails() {
		val response = restTemplate.postForEntity("${root}/getDetails",
				listOf("id1", "id2"),
				GetDetailsResponse::class.java)
		Assertions.assertTrue(response.statusCodeValue == 200)
		Assertions.assertNotNull(response.body)
		Assertions.assertNotNull(response.body!!.details)
	}

}
