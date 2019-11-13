package no.nav.pam.euresstillingeksport.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.AdStatus
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseMetadata
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StillingService(@Autowired private val stillingRepository: StillingRepository,
                      @Autowired private val objectMapper: ObjectMapper) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingService::class.java)
    }

    @Transactional
    fun lagreStillinger(ad: List<Ad>) {
        val eksisterendeStillinger = stillingRepository.findStillingsannonserByIds(ad.map {it.uuid})
                .associateBy({it.first.id}, {it})
        val nyeAnnonser = ArrayList<Pair<StillingsannonseMetadata, String>>()
        val endredeAnnonser = ArrayList<Pair<StillingsannonseMetadata, String>>()

        ad.forEach {
           val jsonAd = objectMapper.writeValueAsString(it)
           if (eksisterendeStillinger[it.uuid] != null) {
               // Stillingsannonsen fins i databasen fra før.
               val eksisterendeAd = objectMapper.readValue(eksisterendeStillinger[it.uuid]?.second, Ad::class.java)
               val eksisterendeMetadata = eksisterendeStillinger[it.uuid]?.first

               // TODO Kunne vi brukt String.equals på jsonAd og json i databasen isteden for å sammenligne deserialiserte objekter?
               if (!eksisterendeAd.equals(it) && eksisterendeMetadata != null) {
                   // Reell endring i annonse
                    val nyMetadata =
                            konverterTilStillingsannonseMetadata(it, eksisterendeMetadata)
                   endredeAnnonser.add(Pair(nyMetadata, jsonAd))
                   LOG.info("Annonse {} er endret. Status er {}", it.uuid, it.status)
               } else {
                   LOG.info("Ingen endring i annonse {} - ignorerer", it.uuid)
               }
           } else {
                // Ny Stillingsannonse - kun legg til nye annonser som er aktive
                if (AdStatus.fromString(it.status) == AdStatus.ACTIVE) {
                    nyeAnnonser.add(Pair(konverterTilStillingsannonseMetadata(it), jsonAd))
                    LOG.info("Ny annonse {}", it.uuid)
               } else {
                   LOG.info("Ny annonse {} har status {}, blir ikke lagt til", it.uuid, it.status)
               }
           }
        }

        stillingRepository.updateStillingsannonser(endredeAnnonser)
        stillingRepository.saveStillingsannonser(nyeAnnonser)
    }

    // Konverterer en ny annonse til metadata
    private fun konverterTilStillingsannonseMetadata(ad : Ad) : StillingsannonseMetadata {
        val status = AdStatus.fromString(ad.status)
        val now = LocalDateTime.now()
        val closed = if (status == AdStatus.ACTIVE)
            null else now
        return StillingsannonseMetadata(ad.uuid, ad.source ?: "NAV",
                status,
                now, now, closed)
    }

    // Konverterer en endret annonse til metadata
    private fun konverterTilStillingsannonseMetadata(ad : Ad, eksisterende: StillingsannonseMetadata)
            : StillingsannonseMetadata {
        val status = AdStatus.fromString(ad.status)
        val now = LocalDateTime.now()

        var opprettet = now
        var closed : LocalDateTime? = null

        if (status == AdStatus.ACTIVE && eksisterende.lukketTs != null) {
            // Tidligere slettet annonse som har blitt aktiv igjen. Skal ha opprettet tidspunkt=nå
            opprettet = now
            closed = null
        } else {
            opprettet = eksisterende.opprettetTs
            closed = if (status == AdStatus.ACTIVE)
                null else now
        }

        return StillingsannonseMetadata(ad.uuid, ad.source ?: eksisterende.kilde,
                status,
                opprettet, now, closed)
    }

    fun hentAlleAktiveStillinger() : List<StillingsannonseMetadata> =
            stillingRepository.findStillingsannonserByStatus("ACTIVE", null)
    fun hentAlleStillinger(nyereEnnTs: Long?) : List<StillingsannonseMetadata> =
        stillingRepository.findStillingsannonserByStatus(null, nyereEnnTs)

    fun hentStillingsannonser(uuidListe : List<String>) : List<Pair<StillingsannonseMetadata, Ad>> {
        return stillingRepository.findStillingsannonserByIds(uuidListe).map {
            val ad = objectMapper.readValue(it.second, Ad::class.java)
            Pair(it.first, ad)
        }
    }

    fun hentStillingsannonse(uuid: String) : Pair<StillingsannonseMetadata, Ad>? {
        var stillingsannonseJson : Pair<StillingsannonseMetadata, String>?
        try {
            stillingsannonseJson = stillingRepository.findStillingsannonseById(uuid)
        } catch (e: EmptyResultDataAccessException){
            stillingsannonseJson = null
        }

        return if (stillingsannonseJson == null) null
            else Pair<StillingsannonseMetadata, Ad>(stillingsannonseJson.first,
                objectMapper.readValue(stillingsannonseJson.second, Ad::class.java))
    }
}