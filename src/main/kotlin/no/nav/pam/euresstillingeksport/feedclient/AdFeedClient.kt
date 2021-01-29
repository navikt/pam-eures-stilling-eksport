package no.nav.pam.euresstillingeksport.feedclient

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.pam.euresstillingeksport.model.Ad
import no.nav.pam.euresstillingeksport.model.StillingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

@Service
class AdFeedClient (@Qualifier("pam-ad-feed-provider") private val adProvider: AdProvider) {
//class AdFeedClient (@Qualifier("pam-ad-elastic-provider") private val adProvider: AdProvider) {

    companion object {
        private val retryTemplate = initRetryTemplate()

        private fun initRetryTemplate(): RetryTemplate {
            val retryTemplate = RetryTemplate()

            retryTemplate.setBackOffPolicy(ExponentialBackOffPolicy().apply {
                initialInterval = 100
                multiplier = 2.0
                maxInterval = 1000
            })
            retryTemplate.setThrowLastExceptionOnExhausted(true)
            retryTemplate.setRetryPolicy(SimpleRetryPolicy(3,
                    mapOf(IOException::class.java to true)))
            return retryTemplate
        }

    }


    fun getAd(uuid: String) = retryTemplate.execute<Ad, Exception> { getAdWrapped(uuid) }

    fun hentPage(sistLest: LocalDateTime) = retryTemplate.execute<FeedTransport, Exception> { hentPageWrapped(sistLest) }

    private fun getAdWrapped(uuid: String) = adProvider.fetch(uuid)

    private fun hentPageWrapped(sistLest: LocalDateTime) = adProvider.`fetch updated after`(sistLest)

    @Component
    open class FeedLeser(
            @Autowired private val feedClient: AdFeedClient,
            @Autowired private val stillingService: StillingService,
            @Autowired private val feedRepository: FeedRepository,
            @Autowired private val meterRegistry: MeterRegistry) {

        companion object {
            private val LOG = LoggerFactory.getLogger(FeedLeser::class.java)
        }

        val feedLagMeter = meterRegistry.gauge("pam.ad.feed.lag", AtomicInteger(0))

        @Scheduled(cron = "*/10 * * * * *")
        @SchedulerLock(name = "adFeedLock", lockAtMostForString = "PT90M")
        fun lesFeed() {
            LOG.info("Poller ad feed for stillinger")
            val sistLest = feedRepository.hentFeedPeker()
            poll(sistLest)
        }

        fun poll(sistLest: LocalDateTime) {
            var ferdig = false
            var nyeste = sistLest
            while (!ferdig) {
                var now = System.currentTimeMillis()
                val trans = feedClient.hentPage(nyeste)
                var msBrukt = System.currentTimeMillis() - now
                LOG.info("Leste {} elementer fra feeden på {}ms. Totalt {} sider igjen ",
                        trans.numberOfElements, msBrukt, trans.totalPages)
                feedLagMeter?.set(trans.totalElements)

                now = System.currentTimeMillis()
                trans.content.forEach {
                    if (it.updated.isAfter(nyeste))
                        nyeste = it.updated
                }
                oppdaterPeedpekerOgLagreStillinger(trans.content, nyeste)
                msBrukt = System.currentTimeMillis() - now
                LOG.info("Brukte {}ms på å lagre/oppdatere {} stillinger i databasen.", msBrukt, trans.content.size)
                ferdig = trans.last
            }
            feedLagMeter?.getAndSet(0)
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        open fun oppdaterPeedpekerOgLagreStillinger(ads: List<Ad>, sistLest: LocalDateTime) {
            val antallModifiserteStillinger = stillingService.lagreStillinger(ads)
            feedRepository.oppdaterFeedPeker(sistLest)
            LOG.info("Antall modifiserte stillinger: $antallModifiserteStillinger " +
                    "ny feedpeker: {}", DateTimeFormatter.ISO_DATE_TIME.format(sistLest))
        }

        fun feedpeker() = feedRepository.hentFeedPeker()

        @Transactional
        fun feedpeker(sistLest: LocalDateTime, wipeDb: Boolean = false) {
            feedRepository.oppdaterFeedPeker(sistLest)
            if (wipeDb)
                stillingService.slettNyereEnn(sistLest)
        }
    }

}
