package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.euresapi.HrxmlSerializer
import no.nav.pam.euresstillingeksport.euresapi.EuNace
import no.nav.pam.euresstillingeksport.euresapi.convertToPositionOpening
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

    @Test
    fun `that nace 2 is included`() {
        val ad = read("src/test/resources/ads/nace/ad_with_nace.json").let { JSON.readValue<Ad>(it) }
        val positionOpening = ad.convertToPositionOpening()
        val xml = HrxmlSerializer.serialize(positionOpening)

        val expectedXml = read("src/test/resources/ads/nace/ad_with_nace.xml")

        Assertions.assertThat(xml).isEqualTo(expectedXml)
        print(xml)
    }

    @Test
    fun `that IndustryCode is ignored without nace 2 code`() {
        val ad = read("src/test/resources/ads/nace/ad_without_nace.json").let { JSON.readValue<Ad>(it) }
        val positionOpening = ad.convertToPositionOpening()
        val xml = HrxmlSerializer.serialize(positionOpening)

        val expectedXml = read("src/test/resources/ads/nace/ad_without_nace.xml")

        Assertions.assertThat(xml).isEqualTo(expectedXml)
        print(xml)
    }

    private fun read(file: String) = FileInputStream(file).bufferedReader().use { it.readText() }

    @Test
    fun `validity of valid nace values`() {
        Assertions.assertThat(EuNace("74.300").isValid()).isTrue()
        Assertions.assertThat(EuNace("1.230").isValid()).isTrue()
        Assertions.assertThat(EuNace("09.10").isValid()).isTrue()
    }

    @Test
    fun `validity of invalid nace values`() {
        Assertions.assertThat(EuNace("743.00").isValid()).isFalse()
        Assertions.assertThat(EuNace("2.2").isValid()).isFalse()
        Assertions.assertThat(EuNace("").isValid()).isFalse()
    }

    @Test
    fun `string value of invalid nace values`() {
        Assertions.assertThat(EuNace("743.00").code()).isEqualTo("")
        Assertions.assertThat(EuNace("2.2").code()).isEqualTo("")
    }

    @Test
    fun `string value value of valid nace values`() {
        Assertions.assertThat(EuNace("74.300").code()).isEqualTo("M74.3.0")
        Assertions.assertThat(EuNace("1.230").code()).isEqualTo("A1.2.3")
        Assertions.assertThat(EuNace("09.109").code()).isEqualTo("B9.1.0")
    }

}