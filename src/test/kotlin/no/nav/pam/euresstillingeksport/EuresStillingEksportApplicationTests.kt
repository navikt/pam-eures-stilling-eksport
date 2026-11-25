package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.model.Converters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
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
		val pong = client.get().uri("$root/ping").exchange()
			.expectStatus().is2xxSuccessful.returnResult(String::class.java).responseBody
		assertThat(pong).startsWith("Hello from Input API")
	}

	@Test
	fun skalSvarePaaGetAll() {
		client.get().uri("$root/getAll").accept(APPLICATION_JSON).exchange()
			.expectStatus().is2xxSuccessful
			.expectBody().jsonPath("$.allReferences").isArray
	}

	@Test
	fun skalSvarePaaGetChanges() {
		val ts = Converters.isoDatetimeToTimestamp("2019-10-01T12:00:00")
		client.get().uri("$root/getChanges/$ts").accept(APPLICATION_JSON).exchange()
			.expectStatus().is2xxSuccessful
			.expectBody().jsonPath("$.modifiedReferences").isArray
	}

	@Test
	fun skalSvarePaaGetDetails() {
		client.post().uri("$root/getDetails").accept(APPLICATION_JSON).body(listOf("id1", "id2")).exchange()
			.expectStatus().is2xxSuccessful
			.expectBody().jsonPath("$.details").isMap
	}
}
