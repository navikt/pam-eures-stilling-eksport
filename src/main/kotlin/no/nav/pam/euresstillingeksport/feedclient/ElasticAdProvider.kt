package no.nav.pam.euresstillingeksport.feedclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.pam.euresstillingeksport.model.Ad
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.min


@Service("pam-ad-elastic-provider")
class ElasticAdProvider(
        @Qualifier("safeElasticClientBuilder") private val clientBuilder: RestClientBuilder,
        @Qualifier("objectMapper") private val mapper: ObjectMapper) : AdProvider {

    val pageSize = 1000;

    override fun `fetch updated after`(sistLest: LocalDateTime): FeedTransport {

        val client = RestHighLevelClient(clientBuilder)

        val searchSourceBuilder = SearchSourceBuilder()
                .sort("updated", SortOrder.ASC)
                .size(pageSize)
                .query(QueryBuilders.rangeQuery("updated").gte(sistLest))

        val request = SearchRequest().source(searchSourceBuilder)
        request.isCcsMinimizeRoundtrips = false


        val response = client.search(request, RequestOptions.DEFAULT)

        val totalHits = response.hits.totalHits?.value?.toInt() ?: 0

        client.close()

        return FeedTransport(
                last = totalHits < pageSize,
                totalPages = ceil(totalHits.toDouble() / pageSize).toInt(),
                totalElements = totalHits,
                size = min(totalHits, pageSize),
                number = 0,
                first = true,
                numberOfElements = totalHits,
                content = response.hits.hits.map {
                    mapper.readValue(it.sourceAsString, Ad::class.java )
                }
        )


    }

    override fun fetch(uuid: String): Ad {
        val client = RestHighLevelClient(clientBuilder)

        val searchSourceBuilder = SearchSourceBuilder()
                .query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("uuid", uuid)))

        val request = SearchRequest().source(searchSourceBuilder)

        val response = client.search(request, RequestOptions.DEFAULT)

        if(response.hits.hits.size != 1) throw IllegalArgumentException("Ad not found")

        client.close()

        return mapper.readValue(response.hits.hits[0].sourceAsString, Ad::class.java )
    }
}

private val mapper = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(JavaTimeModule())
}