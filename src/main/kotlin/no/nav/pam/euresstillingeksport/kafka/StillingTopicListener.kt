package no.nav.pam.euresstillingeksport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.GeografiService
import no.nav.pam.euresstillingeksport.model.StillingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
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
    @Autowired private val stillingService: StillingService,
    @Autowired private val geografiService: GeografiService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingTopicListener::class.java)
    }

    fun startListener(): Thread {
        val t = object: Thread () {
            override fun run() {
                startListenerInternal()
            }
        }
        t.name = "KafkaListener"
        t.start()
        return t
    }

    private fun startListenerInternal() {
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
                    kafkaConsumer.commitSync()
                }
            } catch (e: AuthorizationException) {
                LOG.error("AuthorizationException i consumerloop, restarter app ${e.message}", e)
                kafkaHealthService.addUnhealthyVote()
            } catch (ke: KafkaException) {
                LOG.error("KafkaException occurred in consumeLoop", ke)
                kafkaHealthService.addUnhealthyVote()
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
        val stilling = objectMapper.readValue(record.value(), Ad::class.java).let { geografiService.settLandskoder(it) }
        LOG.info("Stilling ${stilling.uuid} parset OK")
        stillingService.lagreStilling(stilling)
    }
}

@Service
class KafkaHealthService {
    companion object {
        private val unhealthyVotes = AtomicInteger(0)
    }

    fun addUnhealthyVote(): Int {
        return unhealthyVotes.addAndGet(1)
    }

    fun isHealthy() :Boolean {
        val healthy = (unhealthyVotes.get() == 0)
        return healthy
    }
}
