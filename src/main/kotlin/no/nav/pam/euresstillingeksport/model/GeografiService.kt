package no.nav.pam.euresstillingeksport.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class GeografiService(@Autowired private val objectMapper: ObjectMapper) {
    companion object {
        private const val baseUrl = "http://pam-geografi/rest/euland/"
    }

    fun settLandskoder(stilling: Ad): Ad {
        val nyLocationList = stilling.locationList.map { location ->
            if (location.landskode != null) return@map location

            location.country?.let { hentLandskodeHvisEuresLand(it) }?.let { location.copy(landskode = it.landskode) } ?: location
        }

        return stilling.copy(locationList = nyLocationList)
    }

    fun hentLandskodeHvisEuresLand(land: String): EuLandDTO? {
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/$land"))
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
