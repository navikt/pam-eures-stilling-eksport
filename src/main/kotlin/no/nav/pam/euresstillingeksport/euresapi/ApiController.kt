package no.nav.pam.euresstillingeksport.euresapi

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/input/api/jv/v0.1")
class ApiController {
    @Autowired
    private lateinit var apiService: ApiService

    @GetMapping("/ping", produces = ["text/plain"])
    fun ping(): String =
            "Hello from Input API"

    /**
     * Returner alle aktive stillinger
     */
    @GetMapping("/getAll", produces = ["application/json"])
    fun getAll(): GetAllResponse =
            apiService.getAll()

    /**
     * Returner alle endringer (inkludert slettede) siden timestamp.
     */
    @GetMapping("/getChanges/{timestamp}", produces = ["application/json"])
    fun getChanges(@PathVariable("timestamp") ts: Long): GetChangesResponse =
        apiService.getChanges(ts)

    /**
     * Henter detaljer om en stilling
     */
    @PostMapping("/getDetails", produces = ["application/json"], consumes = ["application/json"])
    fun getDetails(@RequestBody referanser : List<String>): GetDetailsResponse =
            apiService.getDetails(referanser)
}
