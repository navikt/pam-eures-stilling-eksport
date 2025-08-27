package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PositionOrganizationConverterTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    private val SN2025: Array<JsonNode> = objectMapper.readValue(javaClass.getResource("/nace/SN2025.json"))

    @Test
    fun `Skal konvertere SN2025 til gyldige EU NACE koder`() {
        SN2025.forEach {
            val nace: JsonNode = objectMapper.readValue(it["nace2"].asText(), JsonNode::class.java).first()
            val SSB_NACE = nace["code"].asText()
            val euNace = EuNace(SSB_NACE)
            assertThat(euNace.isValid()).isTrue
        }
    }
}
