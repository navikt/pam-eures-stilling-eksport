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
    euresflagg("euresflagg"),
    employerDescription("employerdescription")
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
                            instructions = properties[PropertyMapping.sourceurl.key]?.toString() ?: "<a href=\"https://arbeidsplassen.nav.no/stillinger/stilling/$uuid\" rel=\"nofollow\">Link to the vacancy on the Norwegian job board</a>"
                    )
            ),
            positionTitle = title ?: "" ,
            positionLocation = locationList.map { it.toPositionLocation() },
            positionOrganization = employer?.toPositionOrganization(),
            positionOpenQuantity = properties[PropertyMapping.positionCount.key]?.toString()?.filter { it.isDigit() }?.toInt() ?: 1,
            jobCategoryCode = toJobCategoryCode(),
            positionOfferingTypeCode = extentToPositionOfferingTypeCode(properties[PropertyMapping.engagementtype.key].toString()
                    ?: ""),
            positionQualifications = null, // We do not have these data in a structured format
            positionFormattedDescription = toFormattedDescription(),
            workingLanguageCode = "NO",
            positionPeriod = PositionPeriod(startDate = Date(dateText = properties[PropertyMapping.starttime.key]?.toString()?: "na")),
            immediateStartIndicator = guessImmediatStartTime(properties[PropertyMapping.starttime.key]?.toString()
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
        val arbeidsgiverBeskrivelse = (properties[PropertyMapping.employerDescription.key] as String?)
                .let { "<p><b>Om arbeidsgiveren:</b></p>$it" }
        val content = properties[PropertyMapping.adtext.key] as String? ?: ""
        return PositionFormattedDescription("$content$arbeidsgiverBeskrivelse")
    }
}

private fun Ad.toJobCategoryCode(): List<JobCategoryCode> {
    val euresCodes: MutableList<JobCategoryCode> = mutableListOf()

    properties["classification_esco_code"]?.let {
        euresCodes.add(JobCategoryCode(listName = "ESCO_Occupations", listVersionID = "ESCOv1.07", listURI = "https://ec.europa.eu/esco/portal",
                    code = it.toString()))
        Ad.LOG.info("La til ESCO kode ${it.toString()} til $uuid")
    }
    categoryList.forEach { c ->
        if (c.categoryType?.equals("STYRK08NAV", ignoreCase = true) == true) {
            euresCodes.add(JobCategoryCode(code = styrkToEsco(c.code)))
        } else if (c.categoryType?.equals("STYRK08", ignoreCase = true) == true) {
            euresCodes.add(JobCategoryCode(code = styrkToEsco(c.code)))
        } else if (c.categoryType?.equals("ESCO", ignoreCase = true) == true) {
            euresCodes.add(JobCategoryCode(listName = "ESCO_Occupations", listVersionID = "ESCOv1.07", listURI = "https://ec.europa.eu/esco/portal",
                    code = c.code ?:"INGEN"))
        }
    }
    return euresCodes
}

private fun styrkToEsco(styrk: String?) : String {
    if(styrk == null) return "INGEN"
    if(styrk.length < 4) return "INGEN"
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

