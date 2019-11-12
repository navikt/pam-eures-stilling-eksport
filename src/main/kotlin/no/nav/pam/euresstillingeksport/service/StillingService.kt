package no.nav.pam.euresstillingeksport.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseMetadata
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
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

    fun hentAlleAktiveStillinger() : List<StillingsannonseMetadata> =
            hentAlleAktiveStillinger(null)
    fun hentAlleAktiveStillinger(nyereEnnTs: Long?) : List<StillingsannonseMetadata> =
        stillingRepository.findStillingsannonserByStatus("ACTIVE", nyereEnnTs)

    fun hentStillingsannonser(uuidListe : List<String>) : List<Pair<StillingsannonseMetadata, Ad>> {
        return stillingRepository.findAdsByIds(uuidListe).map {
            val ad = objectMapper.readValue(it.second, Ad::class.java)
            Pair(it.first, ad)
        }
    }

    // TODO Dette er ikke helt riktig...
    fun hentStilling(uuid: String) : Ad? {
        var jsonStilling : String?
        try {
            jsonStilling = stillingRepository.findAdById(uuid)
        } catch (e: EmptyResultDataAccessException){
            jsonStilling = null
        }

        return if (jsonStilling == null) null
            else objectMapper.readValue(jsonStilling, Ad::class.java)
    }
}