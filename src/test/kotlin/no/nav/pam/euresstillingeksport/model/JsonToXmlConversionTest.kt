package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.model.eures.HrxmlSerializer
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.NaceConverter
import no.nav.pam.euresstillingeksport.model.pam.convertToPositionOpening
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileInputStream

class ConversionTest {

    val JSON = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
    }

    @Test
    fun initialTest() {
        val ad = read("src/test/resources/ads/ad_1.json").let { JSON.readValue<Ad>(it) }
        val positionOpening = ad.convertToPositionOpening()
        val xml = HrxmlSerializer.serialize(positionOpening)
        print(xml)
    }

    @Test
    fun initialTes2t() {
        val ad = read("src/test/resources/ads/ad_2.json").let { JSON.readValue<Ad>(it) }
        val positionOpening = ad.convertToPositionOpening()
        val xml = HrxmlSerializer.serialize(positionOpening)
        print(xml)
    }

    private fun read(file: String) = FileInputStream(file).bufferedReader().use { it.readText() }

    @Test
    fun skalKonvertereNace() {
        Assertions.assertThat(NaceConverter.naceToEuNace("74.300")).isEqualTo("M74.3.0")
        Assertions.assertThat(NaceConverter.naceToEuNace("743.00")).isNull()
        Assertions.assertThat(NaceConverter.naceToEuNace("1.230")).isEqualTo("A1.2.3")
        Assertions.assertThat(NaceConverter.naceToEuNace("2.2")).isNull()
    }
}