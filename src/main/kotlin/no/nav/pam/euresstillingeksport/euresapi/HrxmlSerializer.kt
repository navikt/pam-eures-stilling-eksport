package no.nav.pam.euresstillingeksport.euresapi

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.dataformat.xml.XmlWriteFeature
import tools.jackson.module.kotlin.KotlinModule

object HrxmlSerializer {

    private val xml: XmlMapper = XmlMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
        .changeDefaultPropertyInclusion {
            JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)
        }
        .enable(XmlWriteFeature.WRITE_XML_DECLARATION)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

    fun serialize(positionOpening: PositionOpening): String {
        return xml.writeValueAsString(positionOpening)
    }

}