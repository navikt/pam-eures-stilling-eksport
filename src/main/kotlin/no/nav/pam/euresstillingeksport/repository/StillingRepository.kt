package no.nav.pam.euresstillingeksport.repository

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.pam.Ad
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseMetadata
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.Timestamp

@Repository
class StillingRepository(@Autowired private val jdbcTemplate: JdbcTemplate) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingRepository::class.java)
    }

    val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    /**
     * @throws EmptyResultDataAccessException
     */
    fun findAdById(uuid: String): String {
        val jsonStilling = jdbcTemplate.queryForObject("select json_stilling from stillinger where id=?",
                arrayOf(uuid), String::class.java)
        return jsonStilling
    }

    fun findAdsByIds(uuider : List<String>) : List<Pair<StillingsannonseMetadata, String>>{
        val annonser = namedJdbcTemplate.query("select id, kilde, status, " +
                "opprettet_ts, sist_endret_ts, varer_til_ts, json_stilling " +
                "from stillinger " +
                "where id in (:uuider) " +
                "order by sist_endret_ts asc",
                MapSqlParameterSource().addValue("uuider", uuider)) {
            rs, rowNum -> Pair(StillingsannonseMetadata(rs.getString("id"),
                rs.getString("kilde"),
                rs.getString("status"),
                rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                rs.getTimestamp("varer_til_ts")?.let { it.toLocalDateTime() } ?: null),
                rs.getString("json_stilling"))
                }
        return annonser
    }

    fun findStillingsannonserByStatus(status: String, nyereEnnTS: Long?): List<StillingsannonseMetadata> {
        var nyereEnnSql = ""
        val sqlParametre = MapSqlParameterSource()
        sqlParametre.addValue("status", status)
        if (nyereEnnTS != null) {
            nyereEnnSql = "and sist_endret_ts >= :nyereEnn "
            sqlParametre.addValue("nyereEnn",
                    Timestamp.valueOf(Converters.timestampToLocalDateTime(nyereEnnTS)))
        }

        val stillingsannonser = namedJdbcTemplate.query("select id, kilde, status, " +
                "opprettet_ts, sist_endret_ts, varer_til_ts " +
                "from stillinger " +
                "where status=:status " +
                "order by sist_endret_ts asc",
                sqlParametre)
        { rs, rowNum ->
            StillingsannonseMetadata(rs.getString("id"),
                    rs.getString("kilde"),
                    rs.getString("status"),
                    rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                    rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                    rs.getTimestamp("varer_til_ts")?.let { it.toLocalDateTime() } ?: null)
        }

        return stillingsannonser
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
        val sqlInsert = "insert into " +
                "stillinger(id, json_stilling, kilde, status," +
                "opprettet_ts, sist_endret_ts, varer_til_ts) " +
                "values(?, ?, ?, ?, ?, ?, ?)"

        val sqlUpdate = "update stillinger " +
                "set json_stilling=?," +
                "kilde=?, status=?, opprettet_ts=?, sist_endret_ts=?, varer_til_ts=? " +
                "where id=?"

        val eksisterendeStillinger = finnEksisterendeStillinger(ads.map { it.first.uuid })
        val adsSomSkalOppdateres = ads.filter { eksisterendeStillinger.contains(it.first.uuid) }
        val adsSomSkalInsertes = ads.filter { !eksisterendeStillinger.contains(it.first.uuid) }

        /* TODO Dette er for lettvint. Vi må hente ut eksisterende annonser og endre
           opprettet/endret timestamp basert på eksisterende status
         */
        val antOppdatert = batchUpdateAds(sqlUpdate, adsSomSkalOppdateres)
        val antInserted = batchInsertAds(sqlInsert, adsSomSkalInsertes)

        return antOppdatert.sum() + antInserted.sum()
    }

    private fun batchUpdateAds(sqlUpdate: String, adsSomSkalOppdateres: List<Pair<Ad, String>>): IntArray {
        val antOppdatert = jdbcTemplate.batchUpdate(sqlUpdate,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalOppdateres.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(7, adsSomSkalOppdateres[idx].first.uuid)
                        pstmt.setString(1, adsSomSkalOppdateres[idx].second)
                        pstmt.setString(2, adsSomSkalOppdateres[idx].first.source ?: "NAV")
                        pstmt.setString(3, adsSomSkalOppdateres[idx].first.status ?: "INACTIVE")
                        pstmt.setTimestamp(4, Timestamp.valueOf(adsSomSkalOppdateres[idx].first.created))
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalOppdateres[idx].first.updated))
                        pstmt.setTimestamp(6,
                                if (adsSomSkalOppdateres[idx].first.expires == null) null
                                else Timestamp.valueOf(adsSomSkalOppdateres[idx].first.expires))
                    }
                }
        )
        return antOppdatert
    }

    private fun batchInsertAds(sqlInsert: String, adsSomSkalInsertes: List<Pair<Ad, String>>): IntArray {
        val antInserted = jdbcTemplate.batchUpdate(sqlInsert,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalInsertes.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(1, adsSomSkalInsertes[idx].first.uuid)
                        pstmt.setString(2, adsSomSkalInsertes[idx].second)
                        pstmt.setString(3, adsSomSkalInsertes[idx].first.source ?: "NAV")
                        pstmt.setString(4, adsSomSkalInsertes[idx].first.status ?: "INACTIVE")
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalInsertes[idx].first.created))
                        pstmt.setTimestamp(6, Timestamp.valueOf(adsSomSkalInsertes[idx].first.updated))
                        pstmt.setTimestamp(7,
                                if (adsSomSkalInsertes[idx].first.expires == null) null
                                else Timestamp.valueOf(adsSomSkalInsertes[idx].first.expires))
                    }
                }
        )
        return antInserted
    }

    private fun finnEksisterendeStillinger(adUUIDer: List<String>) : List<String> {
        val params = MapSqlParameterSource()
        params.addValue("uuider", adUUIDer)
        val eksisterendeUUIDer =
                namedJdbcTemplate.query("select id from stillinger where id in (:uuider)", params)
                { rs, rowNum -> rs.getString(1) }

        return eksisterendeUUIDer
    }
}