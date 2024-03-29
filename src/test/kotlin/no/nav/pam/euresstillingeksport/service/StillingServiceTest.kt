package no.nav.pam.euresstillingeksport.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pam.euresstillingeksport.model.*
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*

class FetchTest {

    private val mockedRepo: StillingRepository = mock<StillingRepository>(StillingRepository::class.java)

    private val stillingService = StillingService(mockedRepo, objectMapper)

    @Test
    fun `skal håndtere at stilling ikke finnes`() {
        Mockito.`when`(mockedRepo.findStillingsannonseById(ArgumentMatchers.anyString())).thenReturn(null)

        val res = stillingService.hentStillingsannonse(UUID.randomUUID().toString())

        assertThat(res).isNull()
    }

    @Test
    fun `at stillingsannonse mappes korrekt til Stillingsannonse`() {
        Mockito.`when`(mockedRepo.findStillingsannonseById(ArgumentMatchers.anyString())).thenReturn(
            StillingsannonseJson(
                stillingsannonseMetadataMother,
                objectMapper.writeValueAsString(adMother)
            )
        )

        val res = stillingService.hentStillingsannonse(UUID.randomUUID().toString())

        assertThat(res).isNotNull
        assertThat(res?.ad).isNotNull
        assertThat(res?.stillingsannonseMetadata).isNotNull
    }
}

class FiltreringsTest {

    private val mockedRepo: StillingRepository = mock<StillingRepository>(StillingRepository::class.java)

    private val stillingService = StillingService(mockedRepo, objectMapper)

    @Test
    fun `skal filtrere bort stillinger som ikke er saksbehandlet ferdig`() {
        val done = adMother.copy(administration = adMother.administration?.copy(status = "DONE"))
        val received = adMother.copy(administration = adMother.administration?.copy(status = "RECEIVED"))
        val pending = adMother.copy(administration = adMother.administration?.copy(status = "PENDING"))
        val approved = adMother.copy(administration = adMother.administration?.copy(status = "APPROVED"))
        val rejected = adMother.copy(administration = adMother.administration?.copy(status = "REJECTED"))
        val stopped = adMother.copy(administration = adMother.administration?.copy(status = "STOPPED"))

        val ads = listOf(done, received, pending, approved, rejected, stopped)

        var lagret = 0
        for (ad in ads) {
            lagret += stillingService.lagreStilling(ad)
        }
        assertThat(lagret == 1)
    }

    @Test
    fun `skal filtrere bort stillinger som ikke er mangler saksbehandlingsstatus`() {
        val missingAdministration = adMother.copy(administration = null)
        assertThat(stillingService.lagreStilling(missingAdministration)).isEqualTo(0)
    }

    @Test
    fun `skal filtrere bort stillinger som er interne`() {

        val intern = adMother.copy(privacy = "INTERNAL_NOT_SHOWN")
        val visAlt = adMother.copy(privacy = "SHOW_ALL")
        val skjulArbeidsgiver = adMother.copy(privacy = "DONT_SHOW_EMPLOYER")
        val skjulAvsender = adMother.copy(privacy = "DONT_SHOW_AUTHOR")

        val ads = listOf(intern, visAlt, skjulArbeidsgiver, skjulAvsender)
        var lagret = 0
        for (ad in ads) {
            lagret += stillingService.lagreStilling(ad)
        }
        assertThat(lagret == 3)
    }

    @Test
    fun `skal filtrere bort stillinger som mangler internflagg`() {
        val manglerPrivatflagg = adMother.copy(privacy = null)

        assertThat(stillingService.lagreStilling(manglerPrivatflagg)).isEqualTo(0)
    }

    @Test
    fun `skal filtrere bort stillinger som ikke er aktive`() {

        val active = adMother.copy(status = "ACTIVE")
        val inactive = adMother.copy(status = "INACTIVE")
        val stopped = adMother.copy(status = "STOPPED")
        val deleted = adMother.copy(status = "DELETED")
        val rejected = adMother.copy(status = "REJECTED")

        val ads = listOf(active, inactive, stopped, deleted, rejected)

        var lagret = 0
        for (ad in ads) {
            lagret += stillingService.lagreStilling(ad)
        }
        assertThat(lagret == 1)
    }

    @Test
    fun `skal filtrere bort stillinger med kilde EURES`() {
        val stillingMedEuresKilde = adMother.copy(source = "EURES")
        assertThat(stillingService.lagreStilling(stillingMedEuresKilde)).isEqualTo(0)
    }

    @Test
    fun `skal filtrere akseptere stillinger med andre kilder enn EURES`() {
        val stillingRegistrertAvSaksbehandler = adMother.copy(source = "ASS")
        assertThat(stillingService.lagreStilling(stillingRegistrertAvSaksbehandler)).isEqualTo(1)
    }

}

private val stillingsannonseMetadataMother = StillingsannonseMetadata(
    id = UUID.randomUUID().toString(),
    kilde = "NAV",
    status = AdStatus.ACTIVE,
    opprettetTs = LocalDateTime.now(),
    sistEndretTs = LocalDateTime.now(),
    lukketTs = LocalDateTime.now()
)

private val adMother = Ad(
    id = 1,
    uuid = UUID.randomUUID().toString(),
    created = LocalDateTime.now(),
    createdBy = null,
    updated = LocalDateTime.now(),
    updatedBy = null,
    locationList = listOf(),
    properties = mapOf(),
    title = null,
    status = "ACTIVE",
    privacy = "SHOW_ALL",
    source = null,
    medium = null,
    reference = null,
    published = null,
    expires = LocalDateTime.now(),
    employer = null,
    categoryList = listOf(),
    administration = Administration(
        id = 1,
        status = "DONE",
        comments = null,
        reportee = null,
        remarks = listOf(),
        navIdent = null
    ),
    publishedByAdmin = null,
    businessName = null,
    firstPublished = false,
    deactivatedByExpiry = false,
    activationOnPublishingDate = false
)

private val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(JavaTimeModule())
}
