package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class GeografiService(@Autowired private val objectMapper: ObjectMapper) {
    companion object {
        private const val baseUrl = "http://pam-geografi/rest/euland"
        private val logger = LoggerFactory.getLogger(GeografiService::class.java)
    }

    fun settLandskoder(stilling: Ad): Ad {
        logger.debug("Setter landskoder for ${stilling.uuid}. LocationList: ${stilling.locationList}")

        val nyLocationList = stilling.locationList.map { location ->


            if (location.landskode != null) return@map location

            location.country?.let { hentLandskodeHvisEuresLand(it) }?.let { location.copy(landskode = it.landskode) } ?: location
        }

        return stilling.copy(locationList = nyLocationList)
    }

    fun hentLandskodeHvisEuresLand(land: String): EuLandDTO? {
        val urlEncodedLand = URLEncoder.encode(land, Charsets.UTF_8.name())
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/$urlEncodedLand"))
            .header("Nav-CallId", "pam-eures-stilling-eksport")
            .GET().build()

        val response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 404) return null
        if (response.statusCode() >= 300 || response.body() == null) throw RuntimeException("Greide ikke hente eu-land fra pam-geografi (responseCode=${response.statusCode()})")
        return objectMapper.readValue(response.body(), EuLandDTO::class.java)
    }
}

data class EuLandDTO(
    val landskode: String, val navn: String, val engelskNavn: String
)
