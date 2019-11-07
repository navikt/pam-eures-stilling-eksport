package no.nav.pam.euresstillingeksport.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StillingService(@Autowired private val stillingRepository: StillingRepository,
                      @Autowired private val objectMapper: ObjectMapper) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingService::class.java)
    }

    fun lagreStilling(ad: Ad) {
        val jsonStilling = objectMapper.writeValueAsString(ad)
        stillingRepository.saveAdAsJson(ad, jsonStilling)
    }

    fun lagreStillinger(ad: List<Ad>) {
        val ads: List<Pair<Ad, String>> =
                ad.map {
                  Pair(it, objectMapper.writeValueAsString(ad))
                }
        stillingRepository.saveAdsAsJson(ads)
    }

    fun hentStilling(uuid: String) : Ad? {
        val jsonStilling = stillingRepository.findAdById(uuid)

        return if (jsonStilling == null) null
            else objectMapper.readValue(jsonStilling, Ad::class.java)
    }
}