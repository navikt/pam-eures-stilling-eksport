package no.nav.pam.euresstillingeksport

import no.nav.pam.euresstillingeksport.kafka.KafkaConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication()
@EnableConfigurationProperties(value = [KafkaConfig.InboundKafkaProperties::class])
class EuresStillingEksportApplication

fun main(args: Array<String>) {
	runApplication<EuresStillingEksportApplication>(*args)
}
