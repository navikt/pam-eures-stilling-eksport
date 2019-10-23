package no.nav.pam.euresstillingeksport.model.eures

import java.time.LocalDateTime

data class PositionOpening(
        private val id: Id,
        private val positionOpeningStatusCode: String,
        private val postingRequester: PostingRequester,
        private val positionProfiles: List<PositionProfile>
)

data class Id(
        private val documentId: DocumentId
)

data class DocumentId(
        private val schemeID: String = "ID-1234", // attributter
        private val schemeName: String = "DocumentIdentifier", // attributter
        private val schemeAgencyID: String = "", // attributter
        private val schemeAgencyName: String = "PublicEmploymentServices01", // attributter
        private val schemeVersionID: String = "1.2", // attributter
        private val schemeDataURI: String = "https://ec.europa.eu/eures/standards/2014/DOC/Data/ID", // attributter
        private val schemeURI: String = "https://ec.europa.eu/eures/standards/2014/DOC/ID", // attributter
        private val validFrom: String = "2014-01-01", // attributter

        private val uuid: String // Body

)


data class PostingRequester(
        private val partyId: PartyId,
        private val partyName: String
)

data class PartyId(
        private val schemeID: String = "Party-IDs", // atributter
        private val schemeName: String = "PartyIdentifier", // atributter
        private val schemeAgencyID: String = "", // atributter
        private val schemeAgencyName: String = "", // atributter
        private val schemeVersionID: String = "1.00", // atributter
        private val schemeDataURI: String = "https://ec.europa.eu/eures/standards/2014/Supplier/Data/ID", // atributter
        private val schemeURI: String = "http://www.pes01.eu", // atributter
        private val validFrom: String = "2014-01-01", // atributter
        private val validTo: String = "2016-12-31", // atributter

        private val partnerId: String // Body - vår ID
)

data class PositionProfile(
        private val postingInstruction: PostingInstruction,
        private val positionTitle: String,
        private val positionLocation: PositionLocation,
        private val positionOrganization: PositionOrganization,
        private val positionOpenQuantity: Int,
        private val jobCategoryCode: JobCategoryCode,
        private val positionOfferingTypeCode: PositionOfferingTypeCode,
        private val positionQualifications: PositionQualifications,
        private val positionFormattedDescription: PositionFormattedDescription,
        private val workingLanguageCode: WorkingLanguageCode,
        private val positionPeriod: PositionPeriod,
        private val immediateStartIndicator: Boolean,
        private val positionScheduleTypeCode: PositionScheduleTypeCode,
        private val applicationCloseDate: LocalDateTime       // YYYY-MM-DD
)


data class PositionOrganization(
        private val organizationIdentifiers: OrganizationIdentifiers,
        private val industryCode: List<IndustryCode>,
        private val organizationSizeCode: OrganizationSizeCode
)

data class OrganizationIdentifiers(
        private val organizationName: String, // child element
        private val organizationLegalID: OrganizationLegalID

)

data class OrganizationLegalID(
        private val schemeID: String, // attribute
        private val schemeAgencyID: String, // attribute
        private val schemeAgencyName: String, // attribute
        private val schemeVersionID: String, // attribute

        private val organizationId: String // Content
)

class IndustryCode // enum/kodeverk
class OrganizationSizeCode // enum/kodeverk

data class PositionLocation(
        private val address: Address
)

data class Address(
        private val cityName: String,
        private val countryCode: CountryCode,
        private val postalCode: String
)

class CountryCode // enum/kodeverk


data class PostingInstruction(
        private val postingOptionCode: String, // enum??
        private val applicationMethod: ApplicationMethod
)

data class ApplicationMethod(
        private val Instructions: String
)

data class PositionQualifications(
    private val positionCompetency: PositionCompetency,
    private val educationRequirement: EducationRequirement,
    private val experienceSummary: ExperienceSummary,
    private val licenseTypeCode: LicenseTypeCode
)

data class PositionCompetency (
    private val competencyID: CompetencyID,
    private val taxonomyID: String, // enum values
    private val requiredProficiencyLevel: RequiredProficiencyLevel,
    private val desiredProficiencyLevel: DesiredProficiencyLevel,
    private val competencyDimension: CompetencyDimension
)

data class CompetencyID (
    private val schemeID: String, // attribute ="ISO-639-1/2-Languages"
    private val schemeAgencyID: String, // attribute ="ISO"
    private val schemeAgencyName: String, // attribute ="ISO"
    private val schemeVersionID: String, // attribute ="639-1:2002 Alpha 2"
    private val schemeDataURI: String, // attribute ="http://www.loc.gov"

    private val value: String // element value
)

data class RequiredProficiencyLevel (
        private val scoreText: String
)

data class DesiredProficiencyLevel (
        private val scoreText: String
)

data class CompetencyDimension(
        private val competencyDimensionTypeCode: CompetencyDimensionTypeCodem,
        private val score: Score
)
data class CompetencyDimensionTypeCodem(
        private val listName: String, // attribute ="EURES_Dimension"
        private val listVersionID: String, // attribute ="1.0"
        private val listURI: String, // attribute ="http://www.coe.int"

        private val value: String // element value
)

data class Score(
        private val scoreText: String
)

data class EducationRequirement(
        private val educationLevelCode: EducationLevelCode,
        private val degreeTypeCode: DegreeTypeCode
)

data class EducationLevelCode(
        private val listName: String, // Attribute
        private val listVersionID: String, // Attribute
        private val value: Int // value
)

data class ExperienceSummary(
        private val experienceCategory: ExperienceCategory,
        private val measure: Measure
)

data class ExperienceCategory(
        private val listName: String, // CategoryCode attribute
        private val listVersionID: String, // CategoryCode attribute
        private val listURI: String, // CategoryCode attribute
        private val name: String, // CategoryCode attribute

        private val categoryCode: String

)

data class Measure(
        private val unitCode: String, // attribute
        private val value: Int // value
)

class DegreeTypeCode // enum/kodeverk
class LicenseTypeCode // enum/kodeverk
class JobCategoryCode // enum/kodeverk + klasse
class PositionOfferingTypeCode // enum/kodeverk
class WorkingLanguageCode // enum/kodeverk
class PositionScheduleTypeCode // enum/kodeverk

data class PositionPeriod(
        private val startDate: FormattedDateTime?,
        private val endDate: FormattedDateTime?,
        private val description: String
)

data class FormattedDateTime(
        private val FormattedDateTime: LocalDateTime
)

data class PositionFormattedDescription(
        private val content: String
)

