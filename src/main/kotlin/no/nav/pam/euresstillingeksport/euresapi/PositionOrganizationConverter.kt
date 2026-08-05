package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.euresapi.EmployerPropertyMapping.Nace2
import no.nav.pam.euresstillingeksport.model.Employer
import org.slf4j.LoggerFactory
import java.lang.ClassCastException

private val JSON = jacksonObjectMapper()

enum class EmployerPropertyMapping(val key: String) {
    Nace2("nace2")
}

private class Nace2Converter {
    fun convert(v: Any): List<NorskNace> {
        val value = if (v is String)
                JSON.readValue(v)
            else
                v
        try {
            val map = (value as List<*>)
                    .map {
                        it as Map<*, *>;
                        NorskNace(it["code"] as String, it["name"] as String)
                    }

            return map
        } catch (e: ClassCastException) {
            LoggerFactory.getLogger(Nace2Converter::class.java).error("Greide ikke å konvertere nacekode $value : ${e.message}", e)
            return emptyList()
        }
    }
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
    if(!properties.containsKey(Nace2.key))
        return emptyList()

    return properties[Nace2.key]
            ?.let { Nace2Converter().convert(it) }
            ?.map { EuNace(it.code) }
            ?.filter { it.isValid() }
            ?.map { IndustryCode(it.code()) }
            ?: emptyList()

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
                in 58..60 -> "J"
                in 61..63 -> "K"
                in 64..66 -> "L"
                68 -> "M"
                in 69..75 -> "N"
                in 77..82 -> "O"
                84 -> "P"
                85 -> "Q"
                in 86..88 -> "R"
                in 90..93 -> "S"
                in 94..96 -> "T"
                in 97..98 -> "U"
                99 -> "V"
                else -> ""
            }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NorskNace(
        val code: String,
        val name: String
)
