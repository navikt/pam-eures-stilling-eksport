package no.nav.pam.euresstillingeksport.euresapi

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiController.BASE_PATH)
class ApiController(private val apiService: ApiService) {

    companion object {
        const val BASE_PATH = "/input/api/jv/v0.1"
        private val LOG = LoggerFactory.getLogger(ApiController::class.java)
    }

    @GetMapping("/ping", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun ping(): String  {
        LOG.debug("ping called")
        return "Hello from Input API"
    }

    /**
     * Returner alle aktive stillinger
     */
    @GetMapping("/getAll")
    fun getAll(): GetAllResponse {
        LOG.debug("getAll called")
        return apiService.getAll()
    }

    /**
     * Returner alle endringer (inkludert slettede) siden timestamp.
     */
    @GetMapping("/getChanges/{timestamp}")
    fun getChanges(@PathVariable("timestamp") ts: Long): GetChangesResponse {
        LOG.debug("getChanges called with ts $ts")
        return apiService.getChanges(ts)
    }

    /**
     * Henter detaljer om en stilling
     */
    @PostMapping("/getDetails")
    fun getDetails(@RequestBody referanser : List<String>): GetDetailsResponse {
        LOG.debug("getDetails called with referanser $referanser")
        return apiService.getDetails(referanser)
    }
}
