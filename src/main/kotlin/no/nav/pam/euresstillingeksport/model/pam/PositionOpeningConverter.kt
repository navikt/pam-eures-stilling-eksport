package no.nav.pam.euresstillingeksport.model.pam

import no.nav.pam.euresstillingeksport.model.eures.*
import java.time.format.DateTimeFormatter
import java.util.Arrays.asList

val formatter = DateTimeFormatter.ofPattern("YYYY-MM-DD")

enum class PropertyMapping(val key: String) {
    applicationdue("applicationdue"), // may be "snarest"
    positionCount("positioncount"),
    adtext("adtext"),
    starttime("starttime"), // may be "snarest"
    extent("extent"),
    engagementtype("engagementtype"),
    applicationurl("applicationurl")
}


fun Ad.convertToPositionOpening(): PositionOpening {
    return PositionOpening(
            id = toId(),
            positionOpeningStatusCode = PositionOpeningStatusCode("Active", "Active"),
            postingRequester = PostingRequester(),
            positionProfiles = asList(toPositionProfile())
    )
}

private fun Ad.toId() = Id(DocumentId(
        uuid = uuid,
        schemeAgencyID = toSchemeAgencyId(source!!, medium!!),
        schemeAgencyName= toSchemeAgencyName(source!!, medium!!)))


private fun Ad.toPositionProfile(): PositionProfile {
    return PositionProfile(
            postingInstruction = PostingInstruction(
                    postingOptionCode = null, // TODO add eures flag if set
                    applicationMethod = ApplicationMethod(
                            instructions = properties[PropertyMapping.applicationurl.key] ?: "See jobdescription"
                    )
            ),
            positionTitle = title ?: "" ,
            positionLocation = locationList.map { it.toPositionLocation() },
            positionOrganization = employer.toPositionOrganization(),
            positionOpenQuantity = properties[PropertyMapping.positionCount.key]?.toInt() ?: 1,
            jobCategoryCode = toJobCategoryCode(),
            positionOfferingTypeCode = extentToPositionOfferingTypeCode(properties[PropertyMapping.engagementtype.key] ?: ""),
            positionQualifications = null, // We do not have these data in a structured format
            positionFormattedDescription = PositionFormattedDescription(properties[PropertyMapping.positionCount.key] ?: ""),
            workingLanguageCode = "NO",
            positionPeriod = PositionPeriod(startDate = Date(dateText = properties[PropertyMapping.starttime.key] ?: "na")),
            immediateStartIndicator = guessImmediatStartTime(properties[PropertyMapping.starttime.key] ?: ""),
            positionScheduleTypeCode = extentToPositionScheduleTypeCode(properties[PropertyMapping.extent.key] ?: ""),
            applicationCloseDate = expires!!
    )
}

fun Ad.toJobCategoryCode(): List<JobCategoryCode> {
    return categoryList.filter { it.categoryType?.equals("STYRK08NAV", ignoreCase = true) ?: false }
            .map { JobCategoryCode(code = it.code ?: "INGEN") }

}

fun extentToPositionOfferingTypeCode(extent: String): PositionOfferingTypeCode { // same as PositionScheduleTypeCode...
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
fun extentToPositionScheduleTypeCode(extent: String): PositionScheduleTypeCode {
    return when(extent) {
        "Heltid" -> PositionScheduleTypeCode.FullTime
        "Deltid" -> PositionScheduleTypeCode.PartTime
        else -> PositionScheduleTypeCode.FullTime
    }
}

fun guessImmediatStartTime(startTime: String) = startTime.contains("snarest", ignoreCase = true)

fun toSchemeAgencyId(source: String, medium: String ): String {
    return when(source) { // TODO denne er nok litt tynn
        "FINN" -> "FINN1"
        "STILLINGSOLR" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "DEXI" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "POLARIS" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "STILLINGSREGISTRERING" -> "NAV"
        "AMEDIA" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        else -> throw IllegalStateException("Ukjent mapping, $source, $medium")
    }
}

fun toSchemeAgencyName(source: String, medium: String ): String {
    return when(source) { // TODO denne er nok også litt tynn
        "FINN" -> "finn.no"
        "STILLINGSOLR" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "DEXI" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "POLARIS" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        "STILLINGSREGISTRERING" -> "NAV PES"
        "AMEDIA" -> throw IllegalStateException("Ukjent mapping, $source, $medium")
        else -> throw IllegalStateException("Ukjent mapping, $source, $medium")
    }
}
