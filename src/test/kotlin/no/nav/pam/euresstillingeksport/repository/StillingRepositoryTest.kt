package no.nav.pam.euresstillingeksport.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

class StillingRepositoryTest {

    lateinit var stillingRepository: StillingRepository

    val jdbcTemplate: JdbcTemplate = mock<JdbcTemplate>(JdbcTemplate::class.java)

    @BeforeEach
    fun init() {
        stillingRepository = StillingRepository(jdbcTemplate)
    }

    @Test
    fun `findanonnsebyid returnerer null når stilling ikke finnes i DB`() {
        val randomUuid = UUID.randomUUID().toString()
        Assertions.assertNull(stillingRepository.findStillingsannonseById(randomUuid))
    }
}