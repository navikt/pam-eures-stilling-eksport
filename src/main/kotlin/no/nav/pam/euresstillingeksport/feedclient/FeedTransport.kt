package no.nav.pam.euresstillingeksport.feedclient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.pam.euresstillingeksport.model.pam.Ad

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeedTransport (
    val last: Boolean,
    val totalPages: Int,
    val totalElements: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val numberOfElements: Int,
    val content: List<Ad>
)