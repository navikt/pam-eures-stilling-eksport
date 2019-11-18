package no.nav.pam.euresstillingeksport.model.eures

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object HrxmlSerializer {

    private val xml: XmlMapper = XmlMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        propertyNamingStrategy = PropertyNamingStrategy.UPPER_CAMEL_CASE
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun serialize(positionOpening: PositionOpening): String {
        return xml.writeValueAsString(positionOpening)
    }

}