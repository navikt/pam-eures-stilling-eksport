package no.nav.pam.euresstillingeksport.repository

import no.nav.pam.euresstillingeksport.model.pam.Ad
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types

@Repository
class StillingRepository(@Autowired private val jdbcTemplate: JdbcTemplate) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingRepository::class.java)
    }

    /**
     * @throws
     */
    fun findAdById(uuid: String): String {
        // Vi må kunne søke på en stilling som ikke fins uten å tryne...
        val jsonStilling = jdbcTemplate.queryForObject("select json_stilling from stillinger where id=?",
                arrayOf(uuid), String::class.java)
        return jsonStilling
    }

    fun saveAdAsJson(ad: Ad, adAsJson: String) {
        val finsFraFoer = jdbcTemplate.queryForObject("select count(*) from stillinger where id=?",
                arrayOf(ad.uuid), Int::class.java)
        if (finsFraFoer > 0) {
            jdbcTemplate.update("update stillinger " +
                    "set json_stilling=?," +
                    "kilde=?, status=?, opprettet_ts=?, sist_endret_ts=?, varer_til_ts=? " +
                    "where id=?",
                    adAsJson, ad.source ?: "NAV", ad.status ?: "INACTIVE",
                    ad.created, ad.updated, ad.expires, ad.uuid)
        } else {
            jdbcTemplate.update("insert into " +
                    "stillinger(id, json_stilling, kilde, status," +
                    "opprettet_ts, sist_endret_ts, varer_til_ts) " +
                    "values(?, ?, ?, ?, ?, ?, ?)",
                    ad.uuid, adAsJson, ad.source ?: "NAV", ad.status ?: "INACTIVE",
                    ad.created, ad.updated, ad.expires)
        }
    }

    fun saveAdsAsJson(ads: List<Pair<Ad, String>>): Int {
        val sql = "merge into stillinger s\n" +
                "using (select cast(? as varchar) as id, " +
                "       cast(? AS CLOB) as json_stilling, " +
                "       cast(? AS varchar) as kilde, " +
                "       cast(? AS varchar) as status, " +
                "       cast(? AS timestamp) as opprettet_ts, " +
                "       cast(? AS timestamp) as sist_endret_ts, " +
                "       cast(? AS timestamp) as varer_til_ts " +
                ") v\n" +
                "on v.id=s.id\n" +
                "when not matched then\n" +
                "  insert values(v.id, v.kilde, v.status, " +
                "    v.opprettet_ts, v.sist_endret_ts, v.varer_til_ts, v.json_stilling)\n" +
                "when matched then\n" +
                "  update set json_stilling=v.json_stilling," +
                "       kilde=v.kilde, " +
                "       status=v.status, " +
                "       opprettet_ts=v.opprettet_ts, " +
                "       sist_endret_ts=v.sist_endret_ts, " +
                "       varer_til_ts=v.varer_til_ts"

        val antOppdatert = jdbcTemplate.batchUpdate(sql,
                object: BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = ads.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(1, ads[idx].first.uuid)
                        pstmt.setString(2, ads[idx].second)
                        pstmt.setString(3, ads[idx].first.source ?: "NAV")
                        pstmt.setString(4, ads[idx].first.status ?: "INACTIVE")
                        pstmt.setTimestamp(5, Timestamp.valueOf(ads[idx].first.created))
                        pstmt.setTimestamp(6, Timestamp.valueOf(ads[idx].first.updated))
                        pstmt.setTimestamp(7,
                                if (ads[idx].first.expires == null) null
                                    else Timestamp.valueOf(ads[idx].first.expires))
                    }
                }
        )
        return antOppdatert.sum()
    }

}