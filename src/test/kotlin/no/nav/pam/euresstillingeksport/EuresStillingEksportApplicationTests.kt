package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.euresapi.GetAllResponse
import no.nav.pam.euresstillingeksport.euresapi.GetChangesResponse
import no.nav.pam.euresstillingeksport.euresapi.GetDetailsResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EuresStillingEksportApplicationTests {

	@Autowired
	lateinit var client: RestTestClient

	val root = "/input/api/jv/v0.1"

	@Test
	fun contextLoads() {
	}

	@Test
	fun skalSvarePaaPing() {
		val pong = client.get().uri("${root}/ping").exchange().expectStatus().is2xxSuccessful.returnResult(String::class.java)
		Assertions.assertTrue(pong.responseBody!!.startsWith("Hello from Input API"))
	}

	@Test
	fun skalSvarePaaGetAll() {
		val response = client.get().uri("${root}/getAll").exchange().expectStatus().is2xxSuccessful.returnResult(GetAllResponse::class.java)
		Assertions.assertNotNull(response.responseBody)
		Assertions.assertNotNull(response.responseBody!!.allReferences)
	}

	@Test
	fun skalSvarePaaGetChanges() {
		val ts = Converters.isoDatetimeToTimestamp("2019-10-01T12:00:00")
		val response = client.get().uri("${root}/getChanges/${ts}").exchange().expectStatus().is2xxSuccessful.returnResult(GetChangesResponse::class.java)
		Assertions.assertNotNull(response.responseBody)
		Assertions.assertNotNull(response.responseBody!!.modifiedReferences)
	}

	@Test
	fun skalSvarePaaGetDetails() {
		val response = client.post().uri("$root/getDetails").body(listOf("id1", "id2"))
			.exchange().expectStatus().is2xxSuccessful.returnResult(GetDetailsResponse::class.java)
		Assertions.assertNotNull(response.responseBody)
		Assertions.assertNotNull(response.responseBody!!.details)
	}
}
