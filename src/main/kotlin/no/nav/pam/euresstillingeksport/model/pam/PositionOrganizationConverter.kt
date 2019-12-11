package no.nav.pam.euresstillingeksport.model.pam

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.model.eures.IndustryCode
import no.nav.pam.euresstillingeksport.model.eures.OrganizationIdentifiers
import no.nav.pam.euresstillingeksport.model.eures.PositionOrganization

private val JSON = jacksonObjectMapper()

enum class EmployerPropertyMapping(val key: String) {
    nace2("nace2")
}

fun Employer.toPositionOrganization(): PositionOrganization {
    return PositionOrganization(
            organizationIdentifiers = OrganizationIdentifiers(
                    organizationLegalID = orgnr,
                    organizationName = name ?: ""
            ),
            industryCode = toIndustryCode()
    )
}

fun Employer.toIndustryCode(): List<IndustryCode> {
    if(!properties.containsKey(EmployerPropertyMapping.nace2.key))
        return emptyList()

    return JSON.readValue<List<Nace2>>(properties.getValue(EmployerPropertyMapping.nace2.key))
            .mapNotNull { NaceConverter.naceToEuNace(it.code)}
            .map { IndustryCode(it) }
}

object NaceConverter {
    // NACE kodene kommer på formatet d?d.ddd
    // EU Nace skal på formatet Add.d.d
    fun naceToEuNace(nace: String): String? {
        val regex = Regex("^(\\d?\\d)\\.(\\d)(\\d).*")
        val matchResult = regex.findAll(nace)
        val groupValues = matchResult.map { it.groupValues }.flatten()

        if (groupValues.count() < 4)
            return null
        val division = groupValues.elementAt(1)
        val section = findSection(division)
        if (section.isEmpty())
            return null
        val groups = groupValues.elementAt(2)
        val classes = groupValues.elementAt(3)

        return "${section}${division}.$groups.$classes"
    }

    fun findSection(division: String): String =
            when (division.toInt()) {
                in 1..3 -> "A"
                in 5..9 -> "B"
                in 10..33 -> "C"
                35 -> "D"
                in 36..39 -> "E"
                in 41..43 -> "F"
                in 45..47 -> "G"
                in 49..53 -> "H"
                in 55..56 -> "I"
                in 58..63 -> "J"
                in 64..66 -> "K"
                68 -> "L"
                in 69..75 -> "M"
                in 77..82 -> "N"
                84 -> "O"
                85 -> "P"
                in 86..88 -> "Q"
                in 90..93 -> "R"
                in 94..96 -> "S"
                in 97..98 -> "T"
                99 -> "U"
                else -> ""
            }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Nace2(
        val code: String,
        val name: String
)

