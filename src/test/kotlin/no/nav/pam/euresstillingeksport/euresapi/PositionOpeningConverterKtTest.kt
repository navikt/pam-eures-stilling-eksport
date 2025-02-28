package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.Category
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class PositionOpeningConverterKtTest {

    fun objectMapper() = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    fun initAd(): Ad = objectMapper()
        .readValue(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"), Ad::class.java)
        .copy(uuid = UUID.randomUUID().toString(), status = "ACTIVE")

    val random = Random(100000)

    @Test
    fun `Fjerne duplikate jobCategories`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/occupation/8d703e8f-1e57-4246-8a6b-cb87a760d4ab"), createStyrkCategory("3412")),
            properties = mapOf("classification_esco_code" to "http://data.europa.eu/esco/occupation/8d703e8f-1e57-4246-8a6b-cb87a760d4ab") //'http://data.europa.eu/esco/occupation/c56bb750-db0b-487b-89f4-82dee6dcab09
        ).toJobCategoryCode()
        assertEquals(2, jobCategoryCode.size)
    }

    @Test
    fun `Esco av typen Isco vises som Isco`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/isco/c4323")),
            properties = mapOf()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("4323", jobCategoryCode[0].code)
        assertEquals("http://ec.europa.eu/esco/ConceptScheme/ISCO2008", jobCategoryCode[0].listURI)
        assertEquals("ISCO2008", jobCategoryCode[0].listName)
        assertEquals("2008", jobCategoryCode[0].listVersionID)
    }

    fun createEscoCategory(code: String): Category {
        return Category(random.nextLong(), code, "ESCO", "", "", null)
    }

    fun createStyrkCategory(code: String): Category {
        return Category(random.nextLong(), code, "STYRK08", "", "", null)
    }
}
