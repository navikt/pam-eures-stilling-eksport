package no.nav.pam.euresstillingeksport.euresapi

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PositionOrganizationConverterTest {

    private val objectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    private val SN2025: Array<JsonNode> = objectMapper.readValue(javaClass.getResourceAsStream("/nace/SN2025.json"), Array<JsonNode>::class.java)

    @Test
    fun `Skal konvertere SN2025 til gyldige EU NACE koder`() {
        SN2025.forEach {
            val nace: JsonNode = objectMapper.readValue(it["nace2"].asString(), JsonNode::class.java).first()
            val SSB_NACE = nace["code"].asString()
            val euNace = EuNace(SSB_NACE)
            assertThat(euNace.isValid()).isTrue
        }
    }
}
