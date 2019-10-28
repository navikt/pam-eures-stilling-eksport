package no.nav.pam.euresstillingeksport.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class StatusController {
    @RequestMapping("/isAlive")
    public fun isAlive(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)

    @RequestMapping("/isReady")
    public fun isReady(): ResponseEntity<String> =
            ResponseEntity("OK", HttpStatus.OK)
}
