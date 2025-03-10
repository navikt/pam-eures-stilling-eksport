package no.nav.pam.euresstillingeksport.euresapi

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/input/api/jv/v0.1")
class ApiController {

    companion object {
        private val LOG = LoggerFactory.getLogger(ApiController::class.java)
    }

    @Autowired
    private lateinit var apiService: ApiService

    @GetMapping("/ping", produces = ["text/plain"])
    fun ping(): String  {
        LOG.debug("ping called")
        return "Hello from Input API"
    }

    /**
     * Returner alle aktive stillinger
     */
    @GetMapping("/getAll", produces = ["application/json"])
    fun getAll(): GetAllResponse {
        LOG.debug("getAll called")
        return apiService.getAll()
    }

    /**
     * Returner alle endringer (inkludert slettede) siden timestamp.
     */
    @GetMapping("/getChanges/{timestamp}", produces = ["application/json"])
    fun getChanges(@PathVariable("timestamp") ts: Long): GetChangesResponse {
        LOG.debug("getChanges called with ts $ts")
        return apiService.getChanges(ts)
    }

    /**
     * Henter detaljer om en stilling
     */
    @PostMapping("/getDetails", produces = ["application/json"], consumes = ["application/json"])
    fun getDetails(@RequestBody referanser : List<String>): GetDetailsResponse {
        LOG.debug("getDetails called with referanser $referanser")
        return apiService.getDetails(referanser)
    }
}
