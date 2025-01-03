package no.nav.pam.euresstillingeksport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.EnableRetry
import java.util.*


@Configuration
@EnableRetry
class KafkaConfig {
    companion object {
        private val LOG = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    @Profile("!test")
    fun kafkaConsumer(@Autowired inboundKafkaProperties: InboundKafkaProperties)
            : KafkaConsumer<String?, ByteArray?> {
        val kafkaConfig = inboundKafkaProperties.asProperties()
        val topic = inboundKafkaProperties.inboundTopic!!
        val consumer: KafkaConsumer<String?, ByteArray?> = KafkaConsumer(kafkaConfig)
        LOG.info("Subscribe to topic '{}' as part of consumer group '{}'", topic, inboundKafkaProperties.inboundGroupId!!)

        // Subscribe, which uses auto-partition assignment (Kafka consumer-group balancing)
        consumer.subscribe(Collections.singleton(topic), object : ConsumerRebalanceListener {
            override fun onPartitionsRevoked(partitions: Collection<TopicPartition?>) {
                partitions.forEach { tp ->
                    LOG.info("Rebalance: no longer assigned to topic {}, partition {}",
                            tp?.topic(), tp?.partition())
                }
            }

            override fun onPartitionsAssigned(partitions: Collection<TopicPartition?>) {
                partitions.forEach { tp ->
                    LOG.info("Rebalance: assigned to topic {}, partition {}",
                            tp?.topic(), tp?.partition())
                }
            }
        })

        return consumer
    }

    /**
     * See [Consumer configs](http://kafka.apache.org/documentation.html#consumerconfigs)
     */
    @ConfigurationProperties(prefix = "kafka")
    data class InboundKafkaProperties(var bootstrapServers: String? = null,
                                      var truststorePath: String? = null,
                                      var credstorePassword: String? = null,
                                      var schemaRegistry: String? = null,
                                      var registryUser: String? = null,
                                      var registryPassword: String? = null,
                                      var keystorePath: String? = null,
                                      var securityProtocol: String? = null,
                                      var inboundTopic: String? = null,
                                      var inboundGroupId: String? = null,
                                      var valueDeserializerClass: String = ByteArrayDeserializer::class.java.name,
                                      var keyDeserializerCLass: String = StringDeserializer::class.java.name,
                                      var enableAutoCommit: Boolean = false,
                                      var autoOffsetResetConfig: String = "earliest",
                                      var maxPollIntervalMs: Int = 500000
    ) {
        fun asProperties(): Map<String, Any> {
            val props = mutableMapOf<String, Any>()
            bootstrapServers?.let { props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers!! }
            credstorePassword?.let {
                props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
                props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            }
            truststorePath?.let { props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath!! }
            keystorePath?.let { props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath!! }
            securityProtocol?.let { props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = securityProtocol!! }

            inboundGroupId?.let { props[ConsumerConfig.GROUP_ID_CONFIG] = inboundGroupId!! }
            props[ConsumerConfig.CLIENT_ID_CONFIG] = System.getenv("HOSTNAME") ?: "pam-eures-stilling-eksport-${UUID.randomUUID()}"
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = valueDeserializerClass
            props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = keyDeserializerCLass
            props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enableAutoCommit
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetResetConfig
            props[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = maxPollIntervalMs
            props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 10000
            props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
            props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1

            LOG.info("Logger kafkaproperties:  ${props}")
            return props
        }
    }

    @Bean
    @Primary
    open fun objectMapper() =
            ObjectMapper().apply {
                registerModule(KotlinModule.Builder().build())
                registerModule(JavaTimeModule())
            }
}
