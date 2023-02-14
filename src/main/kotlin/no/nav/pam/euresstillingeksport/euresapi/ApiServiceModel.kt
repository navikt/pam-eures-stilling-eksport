package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.pam.euresstillingeksport.model.AdStatus

/**
 * Request/responsobjekter som brukes i API'er
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Stillingreferanse (
    val creationTimestamp : Long,
    val lastModificationTimestamp: Long,
    val closingTimestamp: Long?,
    val reference: String,
    val source: String,
    val status: EuresStatus
)

enum class EuresStatus {
    CLOSED,
    ACTIVE;

    companion object {
        @JvmStatic
        fun fromAdStatus(s : AdStatus) : EuresStatus {
            if (s == AdStatus.ACTIVE) {
                return ACTIVE
            } else {
                return CLOSED
            }
        }
    }
}

data class GetAllResponse (
    val allReferences: List<Stillingreferanse>
)

data class GetChangesResponse (
    val createdReferences : List<Stillingreferanse> = emptyList(),
    val modifiedReferences : List<Stillingreferanse> = emptyList(),
    val closedReferences : List<Stillingreferanse> = emptyList()
)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class JvDetails (
        val reference: String,
        val source: String,
        val status: EuresStatus,
        val content: String,
        val contentFormatVersion : String = "1.3",
        val creationTimestamp : Long,
        val lastModificationTimestamp: Long?,
        val closingTimestamp: Long?
)

data class GetDetailsResponse (
    val details : Map<String, JvDetails>
)
