package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.model.eures.HrxmlSerializer
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.convertToPositionOpening
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

}