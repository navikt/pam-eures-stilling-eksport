package no.nav.pam.euresstillingeksport.euresapi

interface ApiService {
    fun getAll(): GetAllResponse
    fun getChanges(ts: Long): GetChangesResponse
    fun getDetails(referanser : List<String>): GetDetailsResponse
}