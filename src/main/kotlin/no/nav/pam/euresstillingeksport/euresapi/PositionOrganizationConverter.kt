package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.euresapi.EmployerPropertyMapping.Nace2
import no.nav.pam.euresstillingeksport.model.Employer
import org.slf4j.LoggerFactory

private val JSON = jacksonObjectMapper()

enum class EmployerPropertyMapping(val key: String) {
    Nace2("nace2")
}

private class Nace2Converter {
    fun convert(value: Any): List<NorskNace> {
        try {
            val map = (value as List<*>)
                    .map {
                        it as Map<*, *>;
                        NorskNace(it["code"] as String, it["name"] as String)
                    }

            return map
        } catch (e: TypeCastException) {
            LoggerFactory.getLogger(Nace2Converter::class.java).error(e.message, e)
            return emptyList()
        }
    }
}

fun Employer.toPositionOrganization(description: String?): PositionOrganization {
    return PositionOrganization(
            organizationIdentifiers = OrganizationIdentifiers(
                    organizationLegalID = orgnr,
                    organizationName = name ?: ""
            ),
            industryCode = toIndustryCode(),
            description = description
    )
}

fun Employer.toIndustryCode(): List<IndustryCode> {
    if(!properties.containsKey(Nace2.key))
        return emptyList()

    return properties.getValue(Nace2.key)
            .let { Nace2Converter().convert(it) }
            .map { EuNace(it.code) }
            .filter { it.isValid() }
            .map { IndustryCode(it.code()) }

}

class EuNace(nace: String) {
    private val unknown = ""

    private val division: String
    private val group: String
    private val clazz: String
    private val section: String get() = findSection(division)

    init {
        val regex = Regex("^(\\d?\\d)\\.(\\d)(\\d).*")
        val groupValues = regex.findAll(nace).map { it.groupValues }
                .flatten()
        division = groupValues.elementAtOrElse(1) {unknown}.trimStart('0')
        group = groupValues.elementAtOrElse(2) {unknown}
        clazz = groupValues.elementAtOrElse(3) {unknown}
    }

    fun isValid() = listOf(division, group, clazz).contains(unknown).not()

    fun code() = if(isValid()) "${section}${division}.$group.$clazz" else ""

    private fun findSection(division: String): String =
            when (division.toIntOrNull()) {
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
private data class NorskNace(
        val code: String,
        val name: String
)

