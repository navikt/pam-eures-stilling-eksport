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
    sourceurl("sourceurl"),
    euresflagg("euresflagg")
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
                    postingOptionCode = if(properties[PropertyMapping.euresflagg.key] == "true") PostingOptionCode.EURESFlag else null,
                    applicationMethod = ApplicationMethod(
                            instructions = properties[PropertyMapping.sourceurl.key].toString() ?: "See jobdescription"
                    )
            ),
            positionTitle = title ?: "" ,
            positionLocation = locationList.map { it.toPositionLocation() },
            positionOrganization = employer?.toPositionOrganization(),
            positionOpenQuantity = properties[PropertyMapping.positionCount.key]?.toString()?.toInt() ?: 1,
            jobCategoryCode = toJobCategoryCode(),
            positionOfferingTypeCode = extentToPositionOfferingTypeCode(properties[PropertyMapping.engagementtype.key].toString()
                    ?: ""),
            positionQualifications = null, // We do not have these data in a structured format
            positionFormattedDescription = toFormattedDescription(),
            workingLanguageCode = "NO",
            positionPeriod = PositionPeriod(startDate = Date(dateText = properties[PropertyMapping.starttime.key].toString() ?: "na")),
            immediateStartIndicator = guessImmediatStartTime(properties[PropertyMapping.starttime.key].toString()
                    ?: ""),
            positionScheduleTypeCode = extentToPositionScheduleTypeCode(properties[PropertyMapping.extent.key].toString()
                    ?: ""),
            applicationCloseDate = expires!!
    )
}

private fun Ad.toFormattedDescription(): PositionFormattedDescription {
    if (erFinnAnnonse()) {
        val finnURL = properties[PropertyMapping.sourceurl.key]
        return PositionFormattedDescription("For fullstendig annonsetekst, se annonsen hos finn.no: " +
                "<a href=\"$finnURL\">$finnURL</a>.")
    } else {
        return PositionFormattedDescription(properties[PropertyMapping.adtext.key].toString() ?: "")
    }
}

private fun Ad.toJobCategoryCode(): List<JobCategoryCode> {
    return categoryList
            .filter { it.categoryType?.equals("STYRK08NAV", ignoreCase = true) ?: false }
            .map { JobCategoryCode(code = styrkToEsco(it.code)) }

}

private fun styrkToEsco(styrk: String?) : String {
    if(styrk == null) return "INGEN"
    if(styrk.length < 3) return "INGEN"
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

private fun guessImmediatStartTime(startTime: String) = startTime.contains("snarest", ignoreCase = true)

