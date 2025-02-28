package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.dataformat.xml.annotation.*
import java.time.LocalDateTime

@JacksonXmlRootElement
data class PositionOpening(
        // Jackson namespace hack
        @JacksonXmlProperty(isAttribute = true, localName = "xmlns")
        val xmlns: String = "http://www.hr-xml.org/3",

        val documentID: DocumentId,
        val positionOpeningStatusCode: PositionOpeningStatusCode,
        val postingRequester: PostingRequester,
        @JacksonXmlElementWrapper(useWrapping = false)
        val positionProfile: List<PositionProfile>,
        @JacksonXmlProperty(isAttribute = true, localName = "validFrom")
        val validFrom: String = "2019-11-05",
        @JacksonXmlProperty(isAttribute = true, localName = "majorVersionID")
        val majorVersionID: String = "3",
        @JacksonXmlProperty(isAttribute = true, localName = "minorVersionID")
        val minorVersionID: String = "2"
)

data class DocumentId(
        @JacksonXmlProperty(isAttribute = true, localName = "schemeID")
        val schemeID: String = "NAV-002",
        @JacksonXmlProperty(isAttribute = true, localName = "schemeAgencyID")
        val schemeAgencyID: String = "NAV",
        @JacksonXmlProperty(isAttribute = true, localName = "schemeAgencyName")
        val schemeAgencyName: String = "NAV public employment services",
        @JacksonXmlProperty(isAttribute = true, localName = "schemeVersionID")
        val schemeVersionID: String = "1.3",
        @JacksonXmlText
        val uuid: String
)

data class PositionOpeningStatusCode(

        @JacksonXmlProperty(isAttribute = true, localName = "name")
        val name: String,
        @JacksonXmlText
        val value: String
)

data class PostingRequester(
        val partyID: PartyId = PartyId(),
        val partyName: String? = null
)

data class PartyId(
        @JacksonXmlProperty(isAttribute = true, localName = "schemeID")
        val schemeID: String = "NAV", // atributter
        @JacksonXmlProperty(isAttribute = true, localName = "schemeAgencyID")
        val schemeAgencyID: String = "NAV PES", // atributter
        @JacksonXmlProperty(isAttribute = true, localName = "schemeAgencyName")
        val schemeAgencyName: String = "Nav public employment services", // atributter
        @JacksonXmlText
        val partnerId: String = "9999"
)

data class PositionProfile(
        @JacksonXmlProperty(isAttribute = true, localName = "languageCode")
        val languageCode: String = "no",
        val postingInstruction: PostingInstruction,
        val positionTitle: String,
        @JacksonXmlElementWrapper(useWrapping = false)
        val positionLocation: List<PositionLocation>,
        val positionOrganization: PositionOrganization?,
        val positionOpenQuantity: Int,
        @JacksonXmlElementWrapper(useWrapping = false)
        val jobCategoryCode: List<JobCategoryCode>,
        val positionOfferingTypeCode: PositionOfferingTypeCode,
        val positionQualifications: PositionQualifications?,
        val positionFormattedDescription: PositionFormattedDescription,
        val workingLanguageCode: String,
        val positionPeriod: PositionPeriod,
        val immediateStartIndicator: Boolean,
        val positionScheduleTypeCode: PositionScheduleTypeCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        val applicationCloseDate: LocalDateTime       // YYYY-MM-DD
)


data class PositionOrganization(
        val organizationIdentifiers: OrganizationIdentifiers,
        @JacksonXmlElementWrapper(useWrapping = false)
        val industryCode: List<IndustryCode>,
        val organizationSizeCode: OrganizationSizeCode? = null // Belive we do not have this
)

data class OrganizationIdentifiers(
        val organizationName: String, // child element
//        val schemeID: String, // attribute
//        val schemeAgencyID: String, // attribute
//        val schemeAgencyName: String, // attribute
//        val schemeVersionID: String, // attribute
        val organizationLegalID: String?
)

data class IndustryCode( // enum/kodeverk // TODO NACE_2  -se 4.15.17
        @JacksonXmlText
        val code: String
)

class OrganizationSizeCode // enum/kodeverk

data class PositionLocation(
        val address: Address
)

data class Address(
        @JacksonXmlProperty(namespace = "http://www.openapplications.org/oagis/9")
        val addressLine: String?,
        @JacksonXmlProperty(namespace = "http://www.openapplications.org/oagis/9")
        val cityName: String?,
        @JacksonXmlProperty(namespace = "http://www.openapplications.org/oagis/9")
        val countrySubDivisionCode: String?, // TODO kommunenummer - namespace
        val countryCode: String,
        @JacksonXmlProperty(namespace = "http://www.openapplications.org/oagis/9")
        val postalCode: String?
)


data class PostingInstruction(
        val postingOptionCode: PostingOptionCode? = null,
        val applicationMethod: ApplicationMethod
)

enum class PostingOptionCode {
    EURESFlag
}

data class ApplicationMethod(
        @JacksonXmlCData
        val instructions: String = "See jobdescription"
)

data class PositionQualifications(
    val positionCompetency: PositionCompetency,
    val educationRequirement: EducationRequirement,
    val experienceSummary: ExperienceSummary,
    val licenseTypeCode: LicenseTypeCode
)

data class PositionCompetency (
    val competencyID: CompetencyID,
    val taxonomyID: String, // enum values
    val requiredProficiencyLevel: RequiredProficiencyLevel,
    val desiredProficiencyLevel: DesiredProficiencyLevel,
    val competencyDimension: CompetencyDimension
)

data class CompetencyID (
    val schemeID: String, // attribute ="ISO-639-1/2-Languages"
    val schemeAgencyID: String, // attribute ="ISO"
    val schemeAgencyName: String, // attribute ="ISO"
    val schemeVersionID: String, // attribute ="639-1:2002 Alpha 2"
    val schemeDataURI: String, // attribute ="http://www.loc.gov"

    val value: String // element value
)

data class RequiredProficiencyLevel (
        val scoreText: String
)

data class DesiredProficiencyLevel (
        val scoreText: String
)

data class CompetencyDimension(
        val competencyDimensionTypeCode: CompetencyDimensionTypeCodem,
        val score: Score
)
data class CompetencyDimensionTypeCodem(
        val listName: String, // attribute ="EURES_Dimension"
        val listVersionID: String, // attribute ="1.0"
        val listURI: String, // attribute ="http://www.coe.int"

        val value: String // element value
)

data class Score(
        val scoreText: String
)

data class EducationRequirement(
        val educationLevelCode: EducationLevelCode,
        val degreeTypeCode: DegreeTypeCode
)

data class EducationLevelCode(
        val listName: String, // Attribute
        val listVersionID: String, // Attribute
        val value: Int // value
)

data class ExperienceSummary(
        val experienceCategory: ExperienceCategory,
        val measure: Measure
)

data class ExperienceCategory(
        val listName: String, // CategoryCode attribute
        val listVersionID: String, // CategoryCode attribute
        val listURI: String, // CategoryCode attribute
        val name: String, // CategoryCode attribute

        val categoryCode: String

)

data class Measure(
    val unitCode: String, // attribute
    val value: Int // value
)

class DegreeTypeCode // enum/kodeverk
class LicenseTypeCode // enum/kodeverk
data class JobCategoryCode(
    @JacksonXmlProperty(isAttribute = true, localName = "listName")
    val listName: String = "ISCO2008", // Attribute
    @JacksonXmlProperty(isAttribute = true, localName = "listVersionID")
    val listVersionID: String = "2008", // Attribute
    @JacksonXmlProperty(isAttribute = true, localName = "listURI")
    val listURI: String = "http://ec.europa.eu/esco/ConceptScheme/ISCO2008", // Attribute
    @JacksonXmlText
    val code: String
)

enum class PositionOfferingTypeCode {
    DirectHire,
    Temporary,
    TemporaryToHire,
    ContractToHire,
    Contract,
    Internship,
    Apprenticeship,
    Seasonal,
    OnCall,
    RecruitmentReserve,
    SelfEmployed,
    Volunteer
}

enum class PositionScheduleTypeCode {
    FullTime,
    PartTime
}

data class PositionPeriod(
    val startDate: Date
)

data class Date(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val formattedDateTime: LocalDateTime? = null,
    val dateText: String
)

data class PositionFormattedDescription(
    @JacksonXmlCData
    val content: String
)

