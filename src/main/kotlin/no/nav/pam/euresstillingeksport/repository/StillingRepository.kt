package no.nav.pam.euresstillingeksport.repository

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.pam.AdStatus
import no.nav.pam.euresstillingeksport.model.pam.StillingsannonseMetadata
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
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

    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val stillingFelter = "id, kilde, status, opprettet_ts, sist_endret_ts, lukket_ts, json_stilling"

    private val stillingsannonseRowMapper = RowMapper<StillingsannonseMetadata>
    { rs, rowNum ->
        StillingsannonseMetadata(rs.getString("id"),
                rs.getString("kilde"),
                AdStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                rs.getTimestamp("lukket_ts")?.let { it.toLocalDateTime() } ?: null)
    }

    private val stillingsannonseMedContentRowMapper = RowMapper<Pair<StillingsannonseMetadata, String>>
    { rs, rowNum ->
        Pair(StillingsannonseMetadata(rs.getString("id"),
                rs.getString("kilde"),
                AdStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                rs.getTimestamp("lukket_ts")?.let { it.toLocalDateTime() } ?: null),
                rs.getString("json_stilling"))
    }

    /**
     * @throws EmptyResultDataAccessException
     */
    fun findStillingsannonseById(uuid: String): Pair<StillingsannonseMetadata, String>? {
        val stillingsannonse = jdbcTemplate.queryForObject("select ${stillingFelter} " +
                "from stillinger where id=?",
                arrayOf(uuid), stillingsannonseMedContentRowMapper)
        return stillingsannonse
    }

    fun findStillingsannonserByIds(idListe : List<String>) : List<Pair<StillingsannonseMetadata, String>>{
        val idChunks = idListe.chunked(200)
        val annonser = ArrayList<Pair<StillingsannonseMetadata, String>>()

        idChunks.forEach {
            val params = MapSqlParameterSource()
            params.addValue("idListe", it)
            val annonserIChunk = namedJdbcTemplate.query("select ${stillingFelter} " +
                        "from stillinger " +
                        "where id in (:idListe) " +
                        "order by sist_endret_ts asc",
                MapSqlParameterSource().addValue("idListe", idListe),
                stillingsannonseMedContentRowMapper)
            annonser.addAll(annonserIChunk)
        }

        return annonser
    }

    fun findStillingsannonserByStatus(status: String?, nyereEnnTS: Long?): List<StillingsannonseMetadata> {
        var nyereEnnSql = ""
        var statusSql = ""
        val sqlParametre = MapSqlParameterSource()

        if (nyereEnnTS != null) {
            nyereEnnSql = "and sist_endret_ts >= :nyereEnn "
            sqlParametre.addValue("nyereEnn",
                    Timestamp.valueOf(Converters.timestampToLocalDateTime(nyereEnnTS)))
        }
        if (status != null) {
            statusSql = "and status=:status "
            sqlParametre.addValue("status", status)
        }

        val stillingsannonser = namedJdbcTemplate.query("select ${stillingFelter} " +
                "from stillinger " +
                "where 1=1 " +
                "$statusSql $nyereEnnSql " +
                "order by sist_endret_ts asc",
                sqlParametre,
                stillingsannonseRowMapper)

        return stillingsannonser
    }

    fun updateStillingsannonser(stillingsannonser: List<Pair<StillingsannonseMetadata, String>>): Int {
        val sqlUpdate = "update stillinger " +
                "set json_stilling=?," +
                "kilde=?, status=?, opprettet_ts=?, sist_endret_ts=?, lukket_ts=? " +
                "where id=?"

        val antOppdatert = batchUpdateAds(sqlUpdate, stillingsannonser)
        return antOppdatert
    }

    fun saveStillingsannonser(stillingsannonser: List<Pair<StillingsannonseMetadata, String>>): Int {
        val sqlInsert = "insert into " +
                "stillinger(id, json_stilling, kilde, status," +
                "opprettet_ts, sist_endret_ts, lukket_ts) " +
                "values(?, ?, ?, ?, ?, ?, ?)"
        val antInserted = batchInsertAds(sqlInsert, stillingsannonser)

        return antInserted
    }

    private fun batchUpdateAds(sqlUpdate: String, adsSomSkalOppdateres: List<Pair<StillingsannonseMetadata, String>>): Int {
        val antOppdatert = jdbcTemplate.batchUpdate(sqlUpdate,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalOppdateres.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(7, adsSomSkalOppdateres[idx].first.id)
                        pstmt.setString(1, adsSomSkalOppdateres[idx].second)
                        pstmt.setString(2, adsSomSkalOppdateres[idx].first.kilde)
                        pstmt.setString(3, adsSomSkalOppdateres[idx].first.status.toString())
                        pstmt.setTimestamp(4, Timestamp.valueOf(adsSomSkalOppdateres[idx].first.opprettetTs))
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalOppdateres[idx].first.sistEndretTs))
                        pstmt.setTimestamp(6,
                                if (adsSomSkalOppdateres[idx].first.lukketTs == null) null
                                else Timestamp.valueOf(adsSomSkalOppdateres[idx].first.lukketTs))
                    }
                }
        )
        return antOppdatert.sum()
    }

    private fun batchInsertAds(sqlInsert: String, adsSomSkalInsertes: List<Pair<StillingsannonseMetadata, String>>): Int {
        val antInserted = jdbcTemplate.batchUpdate(sqlInsert,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalInsertes.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(1, adsSomSkalInsertes[idx].first.id)
                        pstmt.setString(2, adsSomSkalInsertes[idx].second)
                        pstmt.setString(3, adsSomSkalInsertes[idx].first.kilde)
                        pstmt.setString(4, adsSomSkalInsertes[idx].first.status.toString())
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalInsertes[idx].first.opprettetTs))
                        pstmt.setTimestamp(6, Timestamp.valueOf(adsSomSkalInsertes[idx].first.sistEndretTs))
                        pstmt.setTimestamp(7,
                                if (adsSomSkalInsertes[idx].first.lukketTs == null) null
                                else Timestamp.valueOf(adsSomSkalInsertes[idx].first.lukketTs))
                    }
                }
        )
        return antInserted.sum()
    }

    fun finnStillingsannonser(idListe: List<String>) : List<StillingsannonseMetadata> {
        // Hvis listen med id'er er veldig stor, så vil vi få sql-feil
        val idChunks = idListe.chunked(200)
        val eksisterendeAnnonser = ArrayList<StillingsannonseMetadata>()

        idChunks.forEach {
            val params = MapSqlParameterSource()
            params.addValue("idListe", it)
            val annonserIChunk =
                    namedJdbcTemplate.query("select id, kilde, status, opprettet_ts, sist_endret_ts, lukket_ts " +
                            "from stillinger where id in (:idListe)", params,
                            stillingsannonseRowMapper)
            eksisterendeAnnonser.addAll(annonserIChunk)
        }

        return eksisterendeAnnonser
    }

}