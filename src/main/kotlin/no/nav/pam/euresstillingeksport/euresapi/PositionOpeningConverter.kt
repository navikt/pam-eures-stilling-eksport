package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.pam.euresstillingeksport.euresapi.ExperienceLevel.INGEN_KRAV_TIL_ARBEIDSERFARING
import no.nav.pam.euresstillingeksport.euresapi.ExperienceLevel.MYE_ARBEIDSERFARING
import no.nav.pam.euresstillingeksport.euresapi.ExperienceLevel.NOE_ARBEIDSERFARING
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.removeAddressesWithOnlyCountryIfMoreSpecificIsGiven

enum class PropertyMapping(val key: String) {
    applicationdue("applicationdue"), // may be "snarest"
    positionCount("positioncount"),
    adtext("adtext"),
    starttime("starttime"), // may be "snarest"
    extent("extent"),
    engagementtype("engagementtype"),
    sourceurl("sourceurl"),
    euresflagg("euresflagg"),
    employerDescription("employerdescription"),
    experience("experience")
}

fun Ad.convertToPositionOpening(): PositionOpening {
    return PositionOpening(
            documentID = DocumentId(uuid = uuid),
            positionOpeningStatusCode = PositionOpeningStatusCode("Active", "Active"),
            postingRequester = PostingRequester(),
            positionProfile = listOf(toPositionProfile())
    )
}

private fun Ad.toPositionProfile(): PositionProfile {
    return PositionProfile(
            postingInstruction = PostingInstruction(
                    postingOptionCode = if(properties[PropertyMapping.euresflagg.key] == "true") PostingOptionCode.EURESFlag else null,
                    applicationMethod = ApplicationMethod(
                            instructions = properties[PropertyMapping.sourceurl.key]?.toString() ?: "<a href=\"https://arbeidsplassen.nav.no/stillinger/stilling/$uuid\" rel=\"nofollow\">Link to the vacancy on the Norwegian job board</a>"
                    )
            ),
            positionTitle = title ?: "" ,
            positionLocation = locationList.map { it.toPositionLocation() }.removeAddressesWithOnlyCountryIfMoreSpecificIsGiven(),
            positionOrganization = employer?.toPositionOrganization(),
            positionOpenQuantity = properties[PropertyMapping.positionCount.key]?.toString()?.filter { it.isDigit() }?.toInt() ?: 1,
            jobCategoryCode = toJobCategoryCode(),
            positionOfferingTypeCode = extentToPositionOfferingTypeCode(properties[PropertyMapping.engagementtype.key].toString()),
            positionQualifications = toPositionQualifications(), // We do not have these data in a structured format
            positionFormattedDescription = toFormattedDescription(),
            workingLanguageCode = "NO",
            positionPeriod = PositionPeriod(startDate = Date(dateText = properties[PropertyMapping.starttime.key]?.toString()?: "na")),
            immediateStartIndicator = guessImmediatStartTime(properties[PropertyMapping.starttime.key]?.toString() ?: ""),
            positionScheduleTypeCode = extentToPositionScheduleTypeCode(properties[PropertyMapping.extent.key].toString()),
            applicationCloseDate = expires!!
    )
}

fun Ad.toPositionQualifications(): PositionQualifications? {
    val experienceInYears = parseExperienceInYears(properties[PropertyMapping.experience.key]?.toString())
    val jobCategoryCodes = toJobCategoryCode()
    if (experienceInYears != null && jobCategoryCodes.isNotEmpty()) {
        val jobCategoryCode = jobCategoryCodes[0] // Bruk bare den første - det er stort sett bare en som er satt uansett
        val measure = Measure(unitCode = "year", value = experienceInYears)
        val experienceCategory = ExperienceCategory(categoryCode = jobCategoryCode, measure = measure)
        val experienceSummary = ExperienceSummary(experienceCategory = experienceCategory)
        return PositionQualifications(experienceSummary = experienceSummary)
    }
    return null
}

private fun Ad.toFormattedDescription(): PositionFormattedDescription {
    if (erFinnAnnonse()) {
        val finnURL = properties[PropertyMapping.sourceurl.key]
        return PositionFormattedDescription("For fullstendig annonsetekst, se annonsen hos finn.no: " +
                "<a href=\"$finnURL\">$finnURL</a>.")
    } else {
        val arbeidsgiverBeskrivelse = (properties[PropertyMapping.employerDescription.key] as String?)
                .let { "<p><b>Om arbeidsgiveren:</b></p>$it" }
        val content = properties[PropertyMapping.adtext.key] as String? ?: ""
        return PositionFormattedDescription("$content$arbeidsgiverBeskrivelse")
    }
}

 fun Ad.toJobCategoryCode(): List<JobCategoryCode> {
    val euresCodes: MutableSet<JobCategoryCode> = mutableSetOf()

    categoryList.forEach { c ->
        val iscoCode = styrkToIsco(c.code)
        if (c.categoryType?.uppercase() == "STYRK08NAV" && iscoCode != null) {
            euresCodes.add(JobCategoryCode(code = iscoCode))
        } else if (c.categoryType?.uppercase() == "STYRK08" && iscoCode != null) {
            euresCodes.add(JobCategoryCode(code = iscoCode))
        } else if (c.categoryType?.uppercase() == "ESCO") {
            val jobCategoryCode = createJobCategoryCodeForIscoOrEsco(c.code, uuid)
            if (jobCategoryCode != null) {
                euresCodes.add(jobCategoryCode)
            }
        }
    }
    return euresCodes.toList()
}

fun createJobCategoryCodeForIscoOrEsco(code: String?, uuid: String): JobCategoryCode? {
    if (code.isNullOrEmpty()) {
        return null
    }

    val iscoPrefix = "http://data.europa.eu/esco/isco/c"
    val iscoPrefixWithCapitalC = "http://data.europa.eu/esco/isco/C"
    if (code.lowercase().startsWith(iscoPrefix)) {
        Ad.LOG.debug("ESCO code contains '/isco' $code $uuid")
        val iscoCode = styrkToIsco(code
                .replace(iscoPrefix, "")
                .replace(iscoPrefixWithCapitalC, "")
        )
        if (iscoCode == null) {
            return null
        }
        return JobCategoryCode(code = iscoCode)
    }
    return JobCategoryCode(
        listName = "ESCO_Occupations",
        listVersionID = "ESCOv1",
        listURI = "https://ec.europa.eu/esco/portal",
        code = code
    )
}

private fun styrkToIsco(styrk: String?) : String? {
    if(styrk == null) return null
    if(styrk.length < 4) return null
    if (styrk == "2223") return "2221" // Sykepleier Styrk08 kode som ikke finnes i Isco
    if (styrk == "2224") return "3412" // Helsepleier Styrk08 kode som ikke finnes i Isco
    if (styrk == "3439") return "3435" // Andre yrker innen estetiske fag som ikke finnes i Isco
    return styrk.substring(0..3)
}

private fun extentToPositionOfferingTypeCode(extent: String): PositionOfferingTypeCode { // same as PositionScheduleTypeCode...
    return when(extent) {
        "Engasjement" -> PositionOfferingTypeCode.Temporary
        "Fast" -> PositionOfferingTypeCode.DirectHire
        "Prosjekt" -> PositionOfferingTypeCode.Temporary
        "Sesong" -> PositionOfferingTypeCode.Seasonal
        "Selvstendig næringsdrivende" -> PositionOfferingTypeCode.Contract
        "Vikariat" -> PositionOfferingTypeCode.Temporary
        "Åremål" -> PositionOfferingTypeCode.Contract
        "Trainee" -> PositionOfferingTypeCode.Apprenticeship
        "Lærling" -> PositionOfferingTypeCode.Apprenticeship
        "Annet" -> PositionOfferingTypeCode.DirectHire
        else -> PositionOfferingTypeCode.DirectHire
    }
}

private fun extentToPositionScheduleTypeCode(extent: String): PositionScheduleTypeCode {
    return when(extent) {
        "Heltid" -> PositionScheduleTypeCode.FullTime
        "Deltid" -> PositionScheduleTypeCode.PartTime
        else -> PositionScheduleTypeCode.FullTime
    }
}

fun parseExperienceInYears(experiencePropertyAsString: String?): Int? {
    if (experiencePropertyAsString == null) {
        return null
    }
    if (experiencePropertyAsString.contains(INGEN_KRAV_TIL_ARBEIDSERFARING.description)) {
        return 0
    }
    if (experiencePropertyAsString.contains(NOE_ARBEIDSERFARING.description)) {
        return 1
    }
    if (experiencePropertyAsString.contains(MYE_ARBEIDSERFARING.description)) {
        return 4
    }
    return null
}

enum class ExperienceLevel(@JsonValue val description: String) {
    INGEN_KRAV_TIL_ARBEIDSERFARING("Ingen"),
    NOE_ARBEIDSERFARING("Noe"), // 1-3år
    MYE_ARBEIDSERFARING("Mye"); // 4år+

    override fun toString(): String {
        return description
    }
}

private fun guessImmediatStartTime(startTime: String) = startTime.contains("snarest", ignoreCase = true)

