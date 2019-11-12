package no.nav.pam.euresstillingeksport.service

interface ApiService {
    fun getAll(): GetAllResponse
    fun getChanges(ts: Long): GetChangesResponse
    fun getDetails(referanser : List<String>): GetDetailsResponse
}