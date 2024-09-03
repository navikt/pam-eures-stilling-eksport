package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.kafka.KafkaConfig
import no.nav.pam.euresstillingeksport.kafka.StillingTopicListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication()
@EnableConfigurationProperties(value = [KafkaConfig.InboundKafkaProperties::class])
class EuresStillingEksportApplication

fun main(args: Array<String>) {
	val ctx = runApplication<EuresStillingEksportApplication>(*args)

	val listener = ctx.getBean(StillingTopicListener::class.java)
	if (listener != null) {
		listener.startListener().join()
		ctx.close()
	} else {
		LoggerFactory.getLogger(EuresStillingEksportApplication::class.java).info("We are not starting listener - this should only happen in dev")
	}
}




