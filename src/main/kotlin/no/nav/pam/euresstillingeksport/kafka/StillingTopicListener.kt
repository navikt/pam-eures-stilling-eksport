package no.nav.pam.euresstillingeksport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.StillingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Service
@Profile("!test")
class StillingTopicListener(
    @Autowired private val kafkaConsumer: KafkaConsumer<String?, ByteArray?>,
    @Autowired private val kafkaHealthService: KafkaHealthService,
    @Autowired private val objectMapper: ObjectMapper,
    @Value("\${kafka.inboundTopic}") private val inboundTopic: String,
    @Autowired private val stillingService: StillingService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingTopicListener::class.java)
    }

    fun startListener() {
        LOG.info("Starter kafka listener...")
        var records: ConsumerRecords<String?, ByteArray?>? = null
        val rollbackCounter = AtomicInteger(0)
        while (kafkaHealthService.isHealthy() && rollbackCounter.get() < 10) {
            try {
                records = kafkaConsumer.poll(Duration.ofSeconds(10))
                LOG.info("Fikk ${records.count()} verdier. ")
                if (records.count() > 0) {
                    if (records.count() > 1) {
                        error("Skal bare få inn en record om gangen")
                    }
                    val record = records.first()
                    LOG.info(
                        "Leste fra $inboundTopic. Keys: {}. Offset: ${record.offset()} . Partition: ${record.partition()}",
                        records.records(inboundTopic).map { it.key() }.joinToString()
                    )
                    handleRecord(record)
                    //kafkaConsumer.commitSync()
                }
            } catch (e: AuthorizationException) {
                LOG.error("AuthorizationException i consumerloop, restarter app ${e.message}", e)
                kafkaHealthService.addUnhealthyVote()
            } catch (ke: KafkaException) {
                LOG.error("KafkaException occurred in consumeLoop", ke)
                // Enten så ruller vi tilbake, eller så dreper vi appen - uvisst hva som er best strategi?
                // Rollback har potensiale for å sende svært mange meldinger til topicet på veldig kort tid
                // Har derfor lagt inn en grense på 10 rollback før appen omstartes. Det er fortsatt potensiale
                // for å publisere svært mange meldinger, men det går ikke like fort...
                if (ke.cause != null && ke.cause is AuthorizationException)
                    kafkaHealthService.addUnhealthyVote()
                else
                    rollback(records!!, kafkaConsumer, rollbackCounter)
            } catch (e: Exception) {
                // Catchall - impliserer at vi skal restarte app
                LOG.error("Uventet Exception i consumerloop, restarter app ${e.message}", e)
                kafkaHealthService.addUnhealthyVote()
            }

        }
        LOG.info("Closing KafkaConsumer. Helsestatus: ${kafkaHealthService.isHealthy()}")
        kafkaHealthService.addUnhealthyVote()
        kafkaConsumer.close()
    }

    private fun handleRecord(record: ConsumerRecord<String?, ByteArray?>) {
        val stilling = objectMapper.readValue(record.value(), Ad::class.java)

        LOG.info("Stilling ${stilling.uuid} parset OK")
        stillingService.lagreStilling(stilling)
    }

    private fun rollback(
        records: ConsumerRecords<String?, ByteArray?>,
        kafkaConsumer: KafkaConsumer<String?, ByteArray?>,
        rollbackCounter: AtomicInteger
    ) {
        try {
            val firstOffsets = mutableMapOf<String, MutableMap<Int, Long>>()
            records.forEach() {
                val partitions = firstOffsets[it.topic()] ?: mutableMapOf()
                firstOffsets[it.topic()] = partitions
                val currentOffset = it.offset()
                val earliest = partitions[it.partition()] ?: currentOffset
                partitions[it.partition()] = Math.min(currentOffset, earliest)
            }
            firstOffsets.forEach { entry ->
                for (partitionOffset in entry.value) {
                    LOG.info("Ruller tilbake ${entry.key} partition ${partitionOffset.key} til ${partitionOffset.value}")
                    kafkaConsumer.seek(TopicPartition(entry.key, partitionOffset.key), partitionOffset.value)
                }
            }
            rollbackCounter.addAndGet(1)
        } catch (e: Exception) {
            LOG.error("Rollback feilet, restarter appen", e)
            kafkaHealthService.addUnhealthyVote()
        }
    }
}

@Service
class KafkaHealthService {
    private val unhealthyVotes = AtomicInteger(0)

    fun addUnhealthyVote() =
        unhealthyVotes.addAndGet(1)

    fun isHealthy() = unhealthyVotes.get() == 0
}
