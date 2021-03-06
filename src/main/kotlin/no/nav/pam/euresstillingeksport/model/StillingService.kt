package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.euresapi.convertToPositionOpening
import no.nav.pam.euresstillingeksport.repository.AnnonseStatistikk
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
open class StillingService(@Autowired private val stillingRepository: StillingRepository,
                      @Autowired private val objectMapper: ObjectMapper) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingService::class.java)
    }

    @Transactional
    fun lagreStillinger(ads: List<Ad>): Int {
        var antallModifiserteStillinger: Int = 0
        val eksisterendeStillinger = stillingRepository.findStillingsannonserByIds(ads.map {it.uuid})
                .associateBy({it.stillingsannonseMetadata.id}, {it})
        val nyeAnnonser = ArrayList<StillingsannonseJson>()
        val endredeAnnonser = ArrayList<StillingsannonseJson>()

        ads
                .filter {
                    if (it.erIntern()) LOG.info("Avviser stillingen ${it.uuid} siden den er intern, men ${it.privacy}")
                    it.erIkkeIntern()
                }.filter {
                    if(!it.erSaksbehandlet()) LOG.info("Avviser stillingen ${it.uuid} siden den ikke er saksbehandlet, men har status ${it.administration?.status}")
                    it.erSaksbehandlet()
                }.filter {
                    if(it.erFraEures()) LOG.info("Avviser stillingen ${it.uuid} siden den stammer fra EURES, source=${it.source}")
                    !it.erFraEures()
                }
                .filter {
            try {
                it.convertToPositionOpening()
                true
            } catch (e: Exception) {
                val suffix = if (eksisterendeStillinger[it.uuid] == null) ""
                        else ". Dette er en oppdatering av en eksisterende annonse."
                LOG.error("Import av stillingsannonse avvist pga at den ikke kan konverteres til Eures format. " +
                        "Stillingsannonse id:{}, feilmelding: {}{}", it.uuid, e.message, suffix, e)
                false
            }
        }.forEach {
           val jsonAd = objectMapper.writeValueAsString(it)
           if (eksisterendeStillinger[it.uuid] != null) {
               // Stillingsannonsen fins i databasen fra før.
               val eksisterendeAd = objectMapper.readValue(eksisterendeStillinger[it.uuid]?.jsonAd, Ad::class.java)
               val eksisterendeMetadata = eksisterendeStillinger[it.uuid]?.stillingsannonseMetadata

               val nyMetadata = konverterTilStillingsannonseMetadata(it, eksisterendeMetadata)

               if (eksisterendeMetadata != null && nyMetadata != null
                       && (!eksisterendeAd.equals(it)
                               || eksisterendeMetadata.status != nyMetadata.status)) {
                   // Reell endring i annonse
                   endredeAnnonser.add(StillingsannonseJson(nyMetadata, jsonAd))
                   antallModifiserteStillinger++
                   LOG.info("Annonse {} er endret. Status er {}", it.uuid, it.status)
               } else {
                   LOG.info("Ingen endring i annonse {} - ignorerer", it.uuid)
               }
           } else {
                // Ny Stillingsannonse - kun legg til nye annonser som er aktive
                if (AdStatus.fromString(it.status) == AdStatus.ACTIVE) {
                    nyeAnnonser.add(StillingsannonseJson(konverterTilStillingsannonseMetadata(it), jsonAd))
                    antallModifiserteStillinger++
               } else {
                   LOG.info("Ny annonse {} har status {}, blir ikke lagt til", it.uuid, it.status)
               }
           }
        }

        stillingRepository.updateStillingsannonser(endredeAnnonser)
        stillingRepository.saveStillingsannonser(nyeAnnonser)
        return antallModifiserteStillinger
    }

    fun slettNyereEnn(tidspunkt: LocalDateTime) = stillingRepository.slettNyereEnn(tidspunkt)

    // Konverterer en ny annonse til metadata
    private fun konverterTilStillingsannonseMetadata(ad : Ad) : StillingsannonseMetadata {
        val status = AdStatus.fromString(ad.status)
        val now = LocalDateTime.now()
        val closed = if (status == AdStatus.ACTIVE)
            null else now
        return StillingsannonseMetadata(
                id = ad.uuid,
                kilde = ad.source ?: "NAV",
                status = status,
                euresFlagget = ad.erEuresFlagget(),
                opprettetTs = now,
                sistEndretTs = now,
                lukketTs = closed)
    }

    // Konverterer en endret annonse til metadata
    private fun konverterTilStillingsannonseMetadata(ad : Ad, eksisterende: StillingsannonseMetadata?)
            : StillingsannonseMetadata? {
        if (eksisterende == null)
            return null
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

        return StillingsannonseMetadata(
                id = ad.uuid,
                kilde = ad.source ?: eksisterende.kilde,
                status = status,
                euresFlagget = ad.erEuresFlagget(),
                opprettetTs = opprettet,
                sistEndretTs = now,
                lukketTs = closed
        )
    }

    fun hentAlleAktiveStillinger() : List<StillingsannonseMetadata> =
            stillingRepository.findStillingsannonserByStatus("ACTIVE", null)

    fun hentAlleStillinger(nyereEnnTs: Long?) : List<StillingsannonseMetadata> =
        stillingRepository.findStillingsannonserByStatus(null, nyereEnnTs)

    fun hentStillingsannonser(uuidListe : List<String>) : List<Stillingsannonse> {
        return stillingRepository.findStillingsannonserByIds(uuidListe).map {
            val ad = objectMapper.readValue(it.jsonAd, Ad::class.java)
            Stillingsannonse(it.stillingsannonseMetadata, ad)
        }
    }

    fun hentStillingsannonse(uuid: String) : Stillingsannonse? {
        try {
            return stillingRepository.findStillingsannonseById(uuid)?.let {
                Stillingsannonse(it.stillingsannonseMetadata, objectMapper.readValue(it.jsonAd, Ad::class.java))
            }
        } catch (e: EmptyResultDataAccessException){
            return null
        }
    }

    fun hentStatistikk(fraOgMed : LocalDateTime?) : List<AnnonseStatistikk> {
        return stillingRepository.tellStillingsannonser(fraOgMed)
    }
}
