package no.nav.pam.euresstillingeksport.repository

import no.nav.pam.euresstillingeksport.model.Converters
import no.nav.pam.euresstillingeksport.model.AdStatus
import no.nav.pam.euresstillingeksport.model.StillingsannonseJson
import no.nav.pam.euresstillingeksport.model.StillingsannonseMetadata
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
import java.time.LocalDateTime

@Repository
class StillingRepository(@Autowired private val jdbcTemplate: JdbcTemplate) {
    companion object {
        private val LOG = LoggerFactory.getLogger(StillingRepository::class.java)
    }

    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val stillingFelter = "id, kilde, status, euresflagg, opprettet_ts, sist_endret_ts, lukket_ts, json_stilling"

    private val stillingsannonseRowMapper = RowMapper<StillingsannonseMetadata>
    { rs, rowNum ->
        StillingsannonseMetadata(rs.getString("id"),
                rs.getString("kilde"),
                AdStatus.valueOf(rs.getString("status")),
                rs.getInt("euresflagg") == 1,
                rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                rs.getTimestamp("lukket_ts")?.let { it.toLocalDateTime() } ?: null)
    }

    private val stillingsannonseMedContentRowMapper = RowMapper<StillingsannonseJson>
    { rs, rowNum ->
        StillingsannonseJson(StillingsannonseMetadata(rs.getString("id"),
                rs.getString("kilde"),
                AdStatus.valueOf(rs.getString("status")),
                rs.getInt("euresflagg") == 1,
                rs.getTimestamp("opprettet_ts").toLocalDateTime(),
                rs.getTimestamp("sist_endret_ts").toLocalDateTime(),
                rs.getTimestamp("lukket_ts")?.let { it.toLocalDateTime() } ?: null),
                rs.getString("json_stilling"))
    }

    /**
     * @throws EmptyResultDataAccessException
     */
    fun findStillingsannonseById(uuid: String): StillingsannonseJson? {
        val stillingsannonse = jdbcTemplate.queryForObject("select ${stillingFelter} " +
                "from stillinger where id=?",
                arrayOf(uuid), stillingsannonseMedContentRowMapper)
        return stillingsannonse
    }

    fun findStillingsannonserByIds(idListe : List<String>) : List<StillingsannonseJson>{
        val idChunks = idListe.chunked(200)
        val annonser = ArrayList<StillingsannonseJson>()

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

    fun updateStillingsannonser(stillingsannonser: List<StillingsannonseJson>): Int {
        val sqlUpdate = "update stillinger " +
                "set json_stilling=?," +
                "kilde=?, status=?, opprettet_ts=?, sist_endret_ts=?, lukket_ts=?, euresflagg=? " +
                "where id=?"

        val antOppdatert = batchUpdateAds(sqlUpdate, stillingsannonser)
        return antOppdatert
    }

    fun saveStillingsannonser(stillingsannonser: List<StillingsannonseJson>): Int {
        val sqlInsert = "insert into " +
                "stillinger(id, json_stilling, kilde, status," +
                "opprettet_ts, sist_endret_ts, lukket_ts, euresflagg) " +
                "values(?, ?, ?, ?, ?, ?, ?, ?)"
        val antInserted = batchInsertAds(sqlInsert, stillingsannonser)

        return antInserted
    }

    private fun batchUpdateAds(sqlUpdate: String, adsSomSkalOppdateres: List<StillingsannonseJson>): Int {
        val antOppdatert = jdbcTemplate.batchUpdate(sqlUpdate,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalOppdateres.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(8, adsSomSkalOppdateres[idx].stillingsannonseMetadata.id)
                        pstmt.setString(1, adsSomSkalOppdateres[idx].jsonAd)
                        pstmt.setString(2, adsSomSkalOppdateres[idx].stillingsannonseMetadata.kilde)
                        pstmt.setString(3, adsSomSkalOppdateres[idx].stillingsannonseMetadata.status.toString())
                        pstmt.setTimestamp(4, Timestamp.valueOf(adsSomSkalOppdateres[idx].stillingsannonseMetadata.opprettetTs))
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalOppdateres[idx].stillingsannonseMetadata.sistEndretTs))
                        pstmt.setTimestamp(6,
                                if (adsSomSkalOppdateres[idx].stillingsannonseMetadata.lukketTs == null) null
                                else Timestamp.valueOf(adsSomSkalOppdateres[idx].stillingsannonseMetadata.lukketTs))
                        pstmt.setInt(7, if (adsSomSkalOppdateres[idx].stillingsannonseMetadata.euresFlagget) 1 else 0)
                    }
                }
        )
        return antOppdatert.sum()
    }

    private fun batchInsertAds(sqlInsert: String, adsSomSkalInsertes: List<StillingsannonseJson>): Int {
        val antInserted = jdbcTemplate.batchUpdate(sqlInsert,
                object : BatchPreparedStatementSetter {
                    override fun getBatchSize(): Int = adsSomSkalInsertes.size
                    override fun setValues(pstmt: PreparedStatement, idx: Int) {
                        pstmt.setString(1, adsSomSkalInsertes[idx].stillingsannonseMetadata.id)
                        pstmt.setString(2, adsSomSkalInsertes[idx].jsonAd)
                        pstmt.setString(3, adsSomSkalInsertes[idx].stillingsannonseMetadata.kilde)
                        pstmt.setString(4, adsSomSkalInsertes[idx].stillingsannonseMetadata.status.toString())
                        pstmt.setTimestamp(5, Timestamp.valueOf(adsSomSkalInsertes[idx].stillingsannonseMetadata.opprettetTs))
                        pstmt.setTimestamp(6, Timestamp.valueOf(adsSomSkalInsertes[idx].stillingsannonseMetadata.sistEndretTs))
                        pstmt.setTimestamp(7,
                                if (adsSomSkalInsertes[idx].stillingsannonseMetadata.lukketTs == null) null
                                else Timestamp.valueOf(adsSomSkalInsertes[idx].stillingsannonseMetadata.lukketTs))
                        pstmt.setInt(8, if (adsSomSkalInsertes[idx].stillingsannonseMetadata.euresFlagget) 1 else 0)
                    }
                }
        )
        return antInserted.sum()
    }

    fun finnStillingsannonser(idListe: List<String>) : List<StillingsannonseMetadata> {
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

    fun slettNyereEnn(tidspunkt: LocalDateTime) {
        jdbcTemplate.update("delete from stillinger where sist_endret_ts > ?",
                Timestamp.valueOf(tidspunkt))
    }

    fun tellStillingsannonser(fraOgMedTidspunkt: LocalDateTime?) : List<AnnonseStatistikk> {
        val params = MapSqlParameterSource()
        var where = ""
        if (fraOgMedTidspunkt != null) {
            "where sist_endret_ts >= :fom "
            params.addValue("fom", fraOgMedTidspunkt)
        }

        val statistikk: MutableList<AnnonseStatistikk> = namedJdbcTemplate.query("select status, count(*) as antall " +
                "from stillinger " +
                where +
                "group by status", params,
                RowMapper<AnnonseStatistikk>
                { rs, rowNum ->
                    AnnonseStatistikk(rs.getString("status"),
                            rs.getLong("antall"))
                }
            ).toMutableList()

        statistikk.addAll(namedJdbcTemplate.query("select status, count(*) as antall " +
                "from stillinger " +
                (if (where == "") "where " else "and ") +
                    "status='ACTIVE' and euresflagg=1 " +
                "group by status", params,
                RowMapper<AnnonseStatistikk>
                { rs, rowNum ->
                    AnnonseStatistikk(rs.getString("status") + "Flagget",
                            rs.getLong("antall"))
                }
        ))
        return statistikk.toList()
    }
}

data class AnnonseStatistikk(
        val status : String,
        val antall : Long
)