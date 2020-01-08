package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ad(
        val id: Long,
        val uuid: String,
        val created: LocalDateTime,
        val createdBy: String?,
        val updated: LocalDateTime,
        val updatedBy: String?,
        //val mediaList: [],
        //val contactList [],
        val locationList: List<Location>,
        val properties: Map<String, String>,
        val title: String?,
        val status: String?,
        val privacy: String?,
        val source: String?,
        val medium: String?,
        val reference: String?,
        val published: LocalDateTime?,
        val expires: LocalDateTime?,
        val employer: Employer?,
        val categoryList: List<Category>,
        val administration: Administration?,
        val publishedByAdmin: String?,
        val businessName: String?,
        val firstPublished: Boolean,
        val deactivatedByExpiry: Boolean,
        val activationOnPublishingDate: Boolean
) {
    fun erSaksbehandlet() = administration?.erSaksbehandlet() ?: false
    fun erIntern() = privacy == null || privacy == "INTERNAL_NOT_SHOWN"
    fun erIkkeIntern() = erIntern().not()
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class Location(
        val address: String?,
        val postalCode: String?,
        val county: String?,
        val municipal: String?,
        val municipalCode: String?,
        val city: String?,
        val country: String?,
        val latitude: String?,
        val longitude: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Category(
        val id: Long,
        val code: String?,
        val categoryType: String?,
        val name: String?,
        val description: String?,
        val parentId: Long?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Administration(
        val id: Long,
        val status: String,
        val comments: String?,
        val reportee: String?,
        val remarks: List<String>,
        val navIdent: String?
) {
    fun erSaksbehandlet() = status == "DONE"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Employer(
        val id: Long,
        val uuid: String,
        val created: String?,
        val createdBy: String?,
        val updated: String?,
        val updatedBy: String?,
        // val mediaList: [],
        // val contactList: [],
        val locationList: List<Location>,
        // Key er key (f.eks nace2, value er en json string)
        val properties: Map<String, String>,
        val name: String?,
        val orgnr: String?,
        val status: String?,
        val parentOrgnr: String?,
        val publicName: String?,
        val deactivated: Boolean,
        val orgform: String?,
        val employees: Int
)

enum class AdStatus {
    DELETED,
    INACTIVE,
    REJECTED,
    STOPPED,
    ACTIVE,
    UNKNOWN;

    companion object {
        @JvmStatic
        @JsonCreator()
        fun fromString(ad : String?) : AdStatus {
            if (ad == null) {
                return UNKNOWN
            } else {
                try {
                    return valueOf(ad)
                } catch (e : Exception) {
                    return UNKNOWN
                }
            }
        }
    }

    fun euresStatus() = if(this == ACTIVE) "ACTIVE" else "CLOSED"
}


/**
 * NB: Timestamps her er ikke de samme tidsstempel som man finner inne i annonsen. Tidsstempel her er
 * tidsstempel som brukt i EURES.
 * Important note: these timestamps should be considered from the point of providing the data to the uniform exchange
 * system of EURES. This means that they might be different from the original timestamps that are recorded in
 * the source systems.
 */
data class StillingsannonseMetadata (
        val id: String,
        val kilde: String = "NAV",
        val status: AdStatus,
        val opprettetTs : LocalDateTime,
        val sistEndretTs : LocalDateTime,
        val lukketTs: LocalDateTime?
)

data class Stillingsannonse (
        val stillingsannonseMetadata: StillingsannonseMetadata,
        val ad: Ad
)

data class StillingsannonseJson (
        val stillingsannonseMetadata: StillingsannonseMetadata,
        val jsonAd: String
)