package no.nav.pam.euresstillingeksport.infrastruktur

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.euresstillingeksport.repository.StillingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@JsonIgnoreProperties(ignoreUnknown = true)
data class Credential(val username: String, val password: String, val ttl: Int)

@Component
@Profile("prod-sbs |dev-sbs")
class VaultClient(@Value("\${vault.dbcreds.url}") private val dbCredentialsUrl: String,
                  @Value("\${vault.auth.url}") private val vaultLoginUrl: String) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingRepository::class.java)
    }
    private val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        }

    private val vaultToken by lazy {
        fromOptionalFile(File("/var/run/secrets/nais.io/vault/vault_token"))
                ?: throw VaultTokenNotFoundException()
    }

    private val saToken by lazy {
        fromOptionalFile(File("/var/run/secrets/kubernetes.io/serviceaccount/token"))
                ?: throw SATokenNotFoundException()
    }

    fun getVaultToken(url: String = vaultLoginUrl, role: String, jwt: String = saToken): String {
        val loginRequest = VaultKubernetesLoginRequest(jwt, role)
        val loginRequestAsJson = objectMapper.writeValueAsString(loginRequest)
        val authAsJson = httpPost(url, loginRequestAsJson, "application/json")
        val auth : VaultAuth = objectMapper.readValue<VaultAuth>(authAsJson)
        return auth.auth.client_token
    }


    fun getDbCredentials(url: String = this.dbCredentialsUrl, vaultToken: String = this.vaultToken): Credential {
        val credsAsJson = httpGet(url, Pair("X-VAULT-TOKEN", vaultToken))
        val vaultCreds = objectMapper.readValue<VaultDatabaseCredential>(credsAsJson)
        return vaultCreds.data
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VaultDatabaseCredential(val data: Credential)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VaultKubernetesLoginRequest(val jwt: String, val role: String)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VaultAuth(val auth: Auth) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Auth(val client_token: String)
    }

    private fun fromOptionalFile(file: File): String? = when {
        file.exists() -> {
            file.readText(Charsets.UTF_8)
        }
        else -> null
    }

    private fun httpGet(url: String, header : Pair<String, String>? = null) : String {
        val client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
        val requestBuilder = java.net.http.HttpRequest.newBuilder()
                .GET()
                .uri(URI(url))
                .timeout(Duration.ofSeconds(5))
        if (header != null) requestBuilder.header(header.first, header.second)

        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 300) // TODO Throw exception instead of logging here?
            LOG.info("Failed to get url ${url}: ${response.body()}")
        return response.body()
    }

    private fun httpPost(url: String, body: String, contentType: String, header: Pair<String, String>? = null) : String {
        val client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
        val requestBuilder = java.net.http.HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI(url))
                .header("Content-Type", contentType)
                .timeout(Duration.ofSeconds(5))
        if (header != null) requestBuilder.header(header.first, header.second)

        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 300) // TODO Throw exception instead of logging here?
            LOG.info("Failed to post to url ${url}: ${response.body()}")
        return response.body()
    }

    private class SATokenNotFoundException : Throwable()
    private class VaultTokenNotFoundException : Throwable()
}