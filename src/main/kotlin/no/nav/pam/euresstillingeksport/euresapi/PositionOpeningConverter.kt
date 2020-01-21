package no.nav.pam.euresstillingeksport.euresapi

import no.nav.pam.euresstillingeksport.model.Ad
import java.util.Arrays.asList

enum class PropertyMapping(val key: String) {
    applicationdue("applicationdue"), // may be "snarest"
    positionCount("positioncount"),
    adtext("adtext"),
    starttime("starttime"), // may be "snarest"
    extent("extent"),
    engagementtype("engagementtype"),
    sourceurl("sourceurl")
}


fun Ad.convertToPositionOpening(): PositionOpening {
    return PositionOpening(
            documentID = DocumentId(uuid = uuid),
            positionOpeningStatusCode = PositionOpeningStatusCode("Active", "Active"),
            postingRequester = PostingRequester(),
            positionProfile = asList(toPositionProfile())
    )
}

private fun Ad.toPositionProfile(): PositionProfile {
    return PositionProfile(
            postingInstruction = PostingInstruction(
                    postingOptionCode = null, // TODO add eures flag if set
                    applicationMethod = ApplicationMethod(
                            instructions = properties[PropertyMapping.sourceurl.key] ?: "See jobdescription"
                    )
            ),
            positionTitle = title ?: "" ,
            positionLocation = locationList.map { it.toPositionLocation() },
            positionOrganization = employer?.toPositionOrganization(),
            positionOpenQuantity = properties[PropertyMapping.positionCount.key]?.toInt() ?: 1,
            jobCategoryCode = toJobCategoryCode(),
            positionOfferingTypeCode = extentToPositionOfferingTypeCode(properties[PropertyMapping.engagementtype.key]
                    ?: ""),
            positionQualifications = null, // We do not have these data in a structured format
            positionFormattedDescription = PositionFormattedDescription(properties[PropertyMapping.adtext.key] ?: ""),
            workingLanguageCode = "NO",
            positionPeriod = PositionPeriod(startDate = Date(dateText = properties[PropertyMapping.starttime.key] ?: "na")),
            immediateStartIndicator = guessImmediatStartTime(properties[PropertyMapping.starttime.key]
                    ?: ""),
            positionScheduleTypeCode = extentToPositionScheduleTypeCode(properties[PropertyMapping.extent.key]
                    ?: ""),
            applicationCloseDate = expires!!
    )
}

private fun Ad.toJobCategoryCode(): List<JobCategoryCode> {
    return categoryList.filter { it.categoryType?.equals("STYRK08NAV", ignoreCase = true) ?: false }
            .map { JobCategoryCode(code = it.code?.substring(0..3) ?: "INGEN") }

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

private fun guessImmediatStartTime(startTime: String) = startTime.contains("snarest", ignoreCase = true)

