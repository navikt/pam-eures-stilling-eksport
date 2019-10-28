package no.nav.pam.euresstillingeksport.service

import com.fasterxml.jackson.annotation.JsonInclude

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
    val status: String
)

data class GetAllResponse (
    val allReferences: List<Stillingreferanse>
)

data class GetChangesResponse (
    val createdReferences : List<Stillingreferanse>,
    val modifiedReferences : List<Stillingreferanse>,
    val closedReferences : List<Stillingreferanse>
)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class JvDetails (
    val reference: String,
    val source: String,
    val status: String,
    /** Content er innholdet i stillingen serialisert til XML i HR Open */
    val content: String,
    val contentFormatVersion : String = "1.0",
    val creationTimestamp : Long,
    val lastModificationTimestamp: Long?,
    val closingTimestamp: Long?
)

data class GetDetailsResponse (
    val details : Map<String, JvDetails>
)