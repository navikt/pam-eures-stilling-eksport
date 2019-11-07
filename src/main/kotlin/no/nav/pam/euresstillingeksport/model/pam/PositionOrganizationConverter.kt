package no.nav.pam.euresstillingeksport.model.pam

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.model.eures.IndustryCode
import no.nav.pam.euresstillingeksport.model.eures.OrganizationIdentifiers
import no.nav.pam.euresstillingeksport.model.eures.OrganizationLegalID
import no.nav.pam.euresstillingeksport.model.eures.PositionOrganization

private val JSON = jacksonObjectMapper()

enum class EmployerPropertyMapping(val key: String) {
    nace2("nace2")
}

fun Employer.toPositionOrganization(): PositionOrganization {
    return PositionOrganization(
            organizationIdentifiers = OrganizationIdentifiers(
                    organizationLegalID = orgnr?.let { OrganizationLegalID(it) },
                    organizationName = name ?: ""
            ),
            industryCode = toIndustryCode()
    )
}

fun Employer.toIndustryCode(): List<IndustryCode> {
    if(!properties.containsKey(EmployerPropertyMapping.nace2.key))
        return emptyList()

    return JSON.readValue<List<Nace2>>(properties.getValue(EmployerPropertyMapping.nace2.key))
            .map { IndustryCode(it.code) }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Nace2(
        val code: String,
        val name: String
)

