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
    fun lagreStilling(stilling: Ad): Int {
        val eksisterendeStilling = stillingRepository.findStillingsannonseById(stilling.uuid)
        val skalSendestilEures = skalStillingSendesTilEures(stilling)
        LOG.debug("Stilling ${stilling.uuid} skal sendes til Eures: ${skalSendestilEures} eksisterende stilling: ${eksisterendeStilling}")

        if (eksisterendeStilling == null && skalSendestilEures) {
            if (AdStatus.fromString(stilling.status) == AdStatus.ACTIVE) {
                val jsonAd = objectMapper.writeValueAsString(stilling)
                LOG.debug("Lagrer ny stilling ${stilling.uuid}")
                stillingRepository.saveStillingsannonser(
                    listOf(
                        StillingsannonseJson(
                            konverterTilStillingsannonseMetadata(stilling),
                            jsonAd
                        )
                    )
                )
                return 1
            } else {
                LOG.info("Ny annonse {} har status {}, blir ikke lagt til", stilling.uuid, stilling.status)
            }
        }
        if (eksisterendeStilling != null && skalSendestilEures) {
            val jsonAd = objectMapper.writeValueAsString(stilling)
            val eksisterendeAd = objectMapper.readValue(eksisterendeStilling?.jsonAd, Ad::class.java)
            val eksisterendeMetadata = eksisterendeStilling?.stillingsannonseMetadata

            val nyMetadata = konverterTilStillingsannonseMetadata(stilling, eksisterendeMetadata)

            if (eksisterendeMetadata != null && nyMetadata != null
                && (!eksisterendeAd.equals(stilling)
                        || eksisterendeMetadata.status != nyMetadata.status)
            ) {
                // Reell endring i annonse
                LOG.debug("Oppdaterer eksisterende stilling ${stilling.uuid}")
                stillingRepository.updateStillingsannonser(listOf(StillingsannonseJson(nyMetadata, jsonAd)))
                LOG.info("Annonse {} er endret. Status er {}", stilling.uuid, stilling.status)
                return 1
            } else {
                LOG.info("Ingen endring i annonse {} - ignorerer", stilling.uuid)
            }
        }
        if (eksisterendeStilling != null && !skalSendestilEures && AdStatus.ACTIVE.equals(eksisterendeStilling?.stillingsannonseMetadata?.status)) {
            LOG.debug("Setter eksisterende stilling ${stilling.uuid} til inaktiv, da den ikke skal vises hos Eures")
            val nyMetadata = eksisterendeStilling.stillingsannonseMetadata.copy(status=AdStatus.INACTIVE, sistEndretTs = stilling.updated, lukketTs = LocalDateTime.now())
            val annonseSattInaktiv = eksisterendeStilling.copy(stillingsannonseMetadata = nyMetadata)
            stillingRepository.updateStillingsannonser(listOf(annonseSattInaktiv))
            return 1
        }
        return 0
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

    fun skalStillingSendesTilEures(stilling: Ad) : Boolean {
        if (stilling.erIntern()) {
            LOG.info("Avviser stillingen ${stilling.uuid} siden den er intern, men ${stilling.privacy}")
            return false
        }
        if (stilling.erFraEures()) {
            LOG.info("Avviser stillingen ${stilling.uuid} siden den stammer fra EURES, source=${stilling.source}")
            return false
        }
        if (!stilling.erSaksbehandlet()) {
            LOG.info("Avviser stillingen ${stilling.uuid} siden den ikke er saksbehandlet, men har status ${stilling.administration?.status}")
            return false
        }
        try {
            stilling.convertToPositionOpening()
        } catch (e: Exception) {
            LOG.error("Import av stillingsannonse avvist pga at den ikke kan konverteres til Eures format. " +
                    "Stillingsannonse id:{}, feilmelding: {}", stilling.uuid, e.message, e)
            return false
        }
        return true
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
