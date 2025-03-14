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

    private fun objectMapper() = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    private fun initAd(): Ad = objectMapper()
        .readValue(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6.json"), Ad::class.java)
        .copy(uuid = UUID.randomUUID().toString(), status = "ACTIVE")

    private val random = Random(100000)

    @Test
    fun `Fjerne duplikate jobCategories`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/occupation/8d703e8f-1e57-4246-8a6b-cb87a760d4ab"), createStyrkCategory("3412")),
            properties = mapOf("classification_esco_code" to "http://data.europa.eu/esco/occupation/8d703e8f-1e57-4246-8a6b-cb87a760d4ab")
        ).toJobCategoryCode()
        assertEquals(2, jobCategoryCode.size)
    }

    @Test
    fun `Esco av typen Isco vises som Isco fra categoryList`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/isco/C5321")),
            properties = mapOf()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("5321", jobCategoryCode[0].code)
        assertEquals("http://ec.europa.eu/esco/ConceptScheme/ISCO2008", jobCategoryCode[0].listURI)
        assertEquals("ISCO2008", jobCategoryCode[0].listName)
        assertEquals("2008", jobCategoryCode[0].listVersionID)
    }

    @Test
    fun `Esco av typen Isco vises som Isco for property classification_esco_code`() {
        val jobCategoryCode = initAd().copy(
            categoryList = emptyList(),
            properties = mapOf("classification_esco_code" to "http://data.europa.eu/esco/isco/c4323")
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("4323", jobCategoryCode[0].code)
        assertEquals("http://ec.europa.eu/esco/ConceptScheme/ISCO2008", jobCategoryCode[0].listURI)
        assertEquals("ISCO2008", jobCategoryCode[0].listName)
        assertEquals("2008", jobCategoryCode[0].listVersionID)
    }

    @Test
    fun `JobCategoryCode for Esco har riktige opplysninger`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/occupation/4ad4024e-d1d3-4dea-b6d1-2c7948111dce")),
            properties = mapOf()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("http://data.europa.eu/esco/occupation/4ad4024e-d1d3-4dea-b6d1-2c7948111dce", jobCategoryCode[0].code)
        assertEquals("https://ec.europa.eu/esco/portal", jobCategoryCode[0].listURI)
        assertEquals("ESCO_Occupations", jobCategoryCode[0].listName)
        assertEquals("ESCOv1.09", jobCategoryCode[0].listVersionID)
    }

    @Test
    fun `Parse experience in years gives the least required number of years`() {
        assertEquals(0, parseExperienceInYears(""""experience": ["Ingen"]"""))
        assertEquals(1, parseExperienceInYears(""""experience": ["Noe"]"""))
        assertEquals(4, parseExperienceInYears(""""experience": ["Mye"]"""))
        assertEquals(0, parseExperienceInYears(""""experience": ["Ingen","Noe","Mye"]"""))
        assertEquals(1, parseExperienceInYears(""""experience": ["Noe","Mye"]"""))
    }

    @Test
    fun `Styrk08 kode 2223 Sykepleier oversettes til Isco kode 2221`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createStyrkCategory("2223")),
            properties = emptyMap()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("2221", jobCategoryCode[0].code)
        assertEquals("ISCO2008", jobCategoryCode[0].listName)
        assertEquals("http://ec.europa.eu/esco/ConceptScheme/ISCO2008", jobCategoryCode[0].listURI)
        assertEquals("2008", jobCategoryCode[0].listVersionID)
    }

    @Test
    fun `Styrk08 kode 2224 Helsepleier oversettes til Isco kode 3412`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createStyrkCategory("2224")),
            properties = emptyMap()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("3412", jobCategoryCode[0].code)
    }

    @Test
    fun `Styrk08 kode 3439 Andre yrker innen estetiske fag oversettes til Isco kode 3435`() {
        val jobCategoryCode = initAd().copy(
            categoryList = listOf(createStyrkCategory("3439")),
            properties = emptyMap()
        ).toJobCategoryCode()
        assertEquals(1, jobCategoryCode.size)
        assertEquals("3435", jobCategoryCode[0].code)
    }

    @Test
    fun `Ugyldige Isco koder blir fjernet`() {
        val invalidJobCategoryCode = initAd().copy(
            categoryList = listOf(createEscoCategory("http://data.europa.eu/esco/isco/C532")), // Kun tre tall er ugyldig
            properties = emptyMap()
        ).toJobCategoryCode()
        assertEquals(0, invalidJobCategoryCode.size)
    }

    private fun createEscoCategory(code: String): Category {
        return Category(random.nextLong(), code, "ESCO", "", "", null)
    }

    private fun createStyrkCategory(code: String): Category {
        return Category(random.nextLong(), code, "STYRK08", "", "", null)
    }
}
