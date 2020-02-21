package no.nav.pam.euresstillingeksport

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.ContentPattern
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.json.JsonContent
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@TestConfiguration
open class ApiTestConfiguration {
    @Bean
    @Primary
    open fun restTemplate(@Autowired restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        val factory = HttpComponentsClientHttpRequestFactory()
        factory.httpClient = disableSSLChecksApacheHttpClient()

        val restTemplate = restTemplateBuilder.requestFactory { factory }
                .build()
        return restTemplate
    }

    companion object {
        /**
         * wiremockserver må starte hentes og startes manuelt i de testklassene som skal bruke det
         */
        fun wiremockServer() : WireMockServer =
                wiremockServer(8081)
        fun wiremockServer(port : Int) : WireMockServer {
            val wms = WireMockServer(port)

            wms.stubFor(post(urlPathEqualTo("/eures/internalad/_search"))
                    .withRequestBody(matchingJsonPath("$.query.bool.must[0].term.uuid.value",
                            equalTo("db6cc067-7f39-42f1-9866-d9ee47894ec6")))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(javaClass.getResource("/mockdata/ad-db6cc067-7f39-42f1-9866-d9ee47894ec6_with-elastic-metadata.json").readText()
                            )))

            wms.stubFor(post(urlPathEqualTo("/eures/internalad/_search"))
                    .withRequestBody(matchingJsonPath("$.query.bool.must[0].term.uuid.value", equalTo("not_found")))
                    .willReturn(aResponse().withStatus(404)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Finner ikke stillingsannonse"
                            )))

            wms.stubFor(post(urlPathEqualTo("/eures/internalad/_search"))
                    .withRequestBody(matchingJsonPath("$.query.bool.must[0].term.uuid.value", equalTo("bad_request")))
                    .willReturn(aResponse().withStatus(400)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Dårlig EDB"
                            )))

            wms.stubFor(post(urlPathEqualTo("/eures/internalad/_search"))
                    .withRequestBody(matchingJsonPath("$.query.bool.must[0].term.uuid.value", equalTo("service_unavailable")))
                    .willReturn(aResponse().withStatus(503)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Utilgjengelig"
                            )))

            return wms
        }

        /**
         * Disable SSL validering i default (Java innebygd) http client
         */
        fun disableSSLChecksDefaultHttpClient() {
            val trustManager: X509TrustManager = object: X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> =
                        emptyArray()
            }

            val sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
        }

        /**
         * Disable SSL validering i Apache HTTP Client - det er aldri godt å vite hvilken klient som faktisk brukes
         */
        fun disableSSLChecksApacheHttpClient() : CloseableHttpClient {
            val trustManager: X509TrustManager = object: X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> =
                        emptyArray()
            }

            val sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())

            val httpClient : CloseableHttpClient = HttpClients
                    .custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build()
            return httpClient
        }
    }
}