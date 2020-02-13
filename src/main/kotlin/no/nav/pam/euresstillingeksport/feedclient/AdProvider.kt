package no.nav.pam.euresstillingeksport.feedclient

import no.nav.pam.euresstillingeksport.model.Ad
import java.time.LocalDateTime

interface AdProvider {

    fun `fetch updated after`(sistLest: LocalDateTime): FeedTransport

    fun `fetch`(uuid: String): Ad
}