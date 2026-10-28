package no.nav.pam.euresstillingeksport.euresapi

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import no.nav.pam.euresstillingeksport.model.Employer
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

    @Test
    fun `OrganizationLegalID skal være parentOrgnr når den finnes`() {
        val organization = employer(orgnr = "987938323", parentOrgnr = "914251559").toPositionOrganization()
        assertThat(organization.organizationIdentifiers.organizationLegalID).isEqualTo("914251559")
    }

    @Test
    fun `OrganizationLegalID skal falle tilbake til orgnr når parentOrgnr mangler`() {
        val organization = employer(orgnr = "987938323", parentOrgnr = null).toPositionOrganization()
        assertThat(organization.organizationIdentifiers.organizationLegalID).isEqualTo("987938323")
    }

    @Test
    fun `OrganizationLegalID skal falle tilbake til orgnr når parentOrgnr er blank`() {
        val organization = employer(orgnr = "987938323", parentOrgnr = " ").toPositionOrganization()
        assertThat(organization.organizationIdentifiers.organizationLegalID).isEqualTo("987938323")
    }

    private fun employer(orgnr: String?, parentOrgnr: String?) = Employer(
        id = 1,
        uuid = "uuid",
        created = null,
        createdBy = null,
        updated = null,
        updatedBy = null,
        locationList = emptyList(),
        properties = emptyMap(),
        name = "Arbeidsgiver AS",
        orgnr = orgnr,
        status = null,
        parentOrgnr = parentOrgnr,
        publicName = null,
        orgform = null
    )
}
