package no.nav.pam.stilling.eksternbridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pam.geography.PostDataDAO
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.retry.annotation.EnableRetry
import java.net.URL
import java.util.*


@Configuration
@EnableRetry
class KafkaConfig {
    companion object {
        private val LOG = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    fun postDataDAO() =
            PostDataDAO()

    @Bean
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

    @Bean
    fun kafkaProducer(@Autowired outboundKafkaProperties: OutboundKafkaProperties): KafkaProducer<String, ByteArray?> {
        return KafkaProducer<String, ByteArray?>(outboundKafkaProperties.asProperties())
    }

    @Bean(name = ["adValueSerializer"])
    fun kafkaAvroValueSerializer(@Autowired schemaRegistryClient: SchemaRegistryClient,
            @Autowired outboundKafkaProperties: OutboundKafkaProperties) : KafkaAvroSerializer {
        val serializer = KafkaAvroSerializer(schemaRegistryClient)
        serializer.configure(outboundKafkaProperties.asProperties(), false)
        return serializer
    }

    @Bean
    fun kafkaAvroValueDeserializer(@Autowired schemaRegistryClient: SchemaRegistryClient,
                                 @Autowired inboundKafkaProperties: InboundKafkaProperties) : Deserializer<Any> {
        val deserializer = KafkaAvroDeserializer(schemaRegistryClient)
        deserializer.configure(inboundKafkaProperties.asProperties(), false)
        return deserializer
    }

    @Bean
    fun schemaRegistryClient(@Autowired outboundKafkaProperties: OutboundKafkaProperties) : SchemaRegistryClient {
        val configs = mutableMapOf<String, Any>()
        if (!outboundKafkaProperties.registryUser.isNullOrEmpty())  {
            configs[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
            configs[SchemaRegistryClientConfig.USER_INFO_CONFIG] =  (outboundKafkaProperties.registryUser?:"") +
                ":" + (outboundKafkaProperties.registryPassword ?:"")
        }
        val url = outboundKafkaProperties.schemaRegistry!!
        val registryClient = CachedSchemaRegistryClient(url, 100, configs)

        return registryClient
    }

    /**
     * See [Producer configs](http://kafka.apache.org/documentation.html#producerconfigs)
     */
    @ConfigurationProperties(prefix = "kafka")
    @ConstructorBinding
    data class OutboundKafkaProperties(var bootstrapServers: String? = null,
                                       var caPath: String? = null,
                                       var truststorePath: String? = null,
                                       var keystorePath: String? = null,
                                       var credstorePassword: String? = null,
                                       var schemaRegistry: String? = null,
                                       var registryUser: String? = null,
                                       var registryPassword: String? = null,
                                       var acks: String = "all",
                                       var securityProtocol: String? = null,
                                       var clientId: String = "StillingInternEksternBridge",
                                       var valueSerializerClass: String = ByteArraySerializer::class.java.name,
                                       var keySerializerCLass: String = StringSerializer::class.java.name,
                                       var retries: Int = Int.MAX_VALUE,
                                       var deliveryTimeoutMs: Int = 10100,
                                       var requestTimeoutMs: Int = 10000,
                                       var lingerMs: Int = 100,
                                       var batchSize: Int = 16384*4
    ) {
        fun asProperties(): Map<String, Any> {
            val props = mutableMapOf<String, Any>()
            bootstrapServers?.let { props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers!! }
            credstorePassword?.let {
                props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword!!
                props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
                props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            }
            schemaRegistry?.let {props["schema.registry.url"] = schemaRegistry!! }
            if (!registryUser.isNullOrEmpty())  {
                props[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
                props[SchemaRegistryClientConfig.USER_INFO_CONFIG] =  (registryUser?:"") + ":" + (registryPassword ?:"")
            }
            // TODO registry username password??? schemaRegistry?.let {props["schema.registry.url"] = registryPassword!! }
            truststorePath?.let { props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath!! }
            keystorePath?.let { props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath!! }
            securityProtocol?.let { props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = securityProtocol!! }

            props[ProducerConfig.CLIENT_ID_CONFIG] = clientId
            props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = valueSerializerClass
            props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = keySerializerCLass

            props[ProducerConfig.RETRIES_CONFIG] = retries
            props[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = deliveryTimeoutMs
            props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = requestTimeoutMs
            props[ProducerConfig.LINGER_MS_CONFIG] = lingerMs
            props[ProducerConfig.BATCH_SIZE_CONFIG] = batchSize
            props[ProducerConfig.ACKS_CONFIG] = acks

            return props
        }
    }

    /**
     * See [Consumer configs](http://kafka.apache.org/documentation.html#consumerconfigs)
     */
    @ConfigurationProperties(prefix = "kafka")
    @ConstructorBinding
    data class InboundKafkaProperties(var bootstrapServers: String? = null,
                                      var truststorePath: String? = null,
                                      var credstorePassword: String? = null,
                                      var schemaRegistry: String? = null,
                                      var registryUser: String? = null,
                                      var registryPassword: String? = null,
                                      var keystorePath: String? = null,
                                      var securityProtocol: String? = null,
                                      var inboundTopic: String? = null,
                                      var clientId: String = "StillingInternEksternBridge",
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
            schemaRegistry?.let {props["schema.registry.url"] = schemaRegistry!! }
            if (!registryUser.isNullOrEmpty())  {
                props[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
                props[SchemaRegistryClientConfig.USER_INFO_CONFIG] =  (registryUser?:"") + ":" + (registryPassword ?:"")
            }

            inboundGroupId?.let { props[ConsumerConfig.GROUP_ID_CONFIG] = inboundGroupId!! }
            props[ConsumerConfig.CLIENT_ID_CONFIG] = clientId
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = valueDeserializerClass
            props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = keyDeserializerCLass
            props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enableAutoCommit
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetResetConfig
            props[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = maxPollIntervalMs
            props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 10000
            props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
            return props
        }
    }

    @Bean
    @Primary
    open fun objectMapper() =
            ObjectMapper().apply {
                registerModule(KotlinModule())
                registerModule(JavaTimeModule())
            }
}