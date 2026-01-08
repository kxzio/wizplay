package org.example.folderGetter.tagsAndAudioGetter

import org.example.audioindex.ScannedAudio
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.Path

class AudioDatabase(dbPath: Path) {

    private val conn: Connection =
        DriverManager.getConnection("jdbc:sqlite:${dbPath.toAbsolutePath()}")

    fun loadRoots(): Set<Path> =
        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT path FROM roots")
            buildSet {
                while (rs.next()) add(Path(rs.getString(1)))
            }
        }



    fun insertRoot(path: Path) {
        conn.prepareStatement(
            "INSERT OR IGNORE INTO roots(path) VALUES (?)"
        ).use {
            it.setString(1, path.toString())
            it.executeUpdate()
        }
    }

    fun deleteRoot(root: Path) {
        conn.prepareStatement(
            "DELETE FROM roots WHERE path = ?"
        ).use {
            it.setString(1, root.toString())
            it.executeUpdate()
        }
    }

    init {
        conn.createStatement().use { st ->

            st.execute("""
            CREATE TABLE IF NOT EXISTS roots (
                path TEXT PRIMARY KEY
            )
        """.trimIndent())

            st.execute("""
            CREATE TABLE IF NOT EXISTS audio (
                path TEXT PRIMARY KEY,
                title TEXT,
                artist TEXT,
                album TEXT,
                year TEXT,
                pos TEXT,
                artwork_path TEXT,
                album_key TEXT,
                album_creator INTEGER
            )
        """.trimIndent())

            st.execute("""
            CREATE INDEX IF NOT EXISTS idx_album_key
            ON audio(album_key)
        """.trimIndent())

            st.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uniq_album_creator
            ON audio(album_key)
            WHERE album_creator = 1
        """.trimIndent())
        }
    }

    fun close() = conn.close()

    fun upsertAudio(a: ScannedAudio) {
        conn.prepareStatement("""
        INSERT INTO audio
        (path, title, artist, album, year, pos, artwork_path, album_key, album_creator)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(path) DO UPDATE SET
            title=excluded.title,
            artist=excluded.artist,
            album=excluded.album,
            year=excluded.year,
            pos=excluded.pos,
            artwork_path=excluded.artwork_path,
            album_key=excluded.album_key,
            album_creator=excluded.album_creator
    """.trimIndent()).use { ps ->
            ps.setString(1, a.path.toString())
            ps.setString(2, a.title)
            ps.setString(3, a.artist)
            ps.setString(4, a.album)
            ps.setString(5, a.year)
            ps.setString(6, a.pos)
            ps.setString(7, a.artworkPath?.toString())
            ps.setString(8, a.albumKey)
            ps.setInt(9, if (a.albumCreator) 1 else 0)
            ps.executeUpdate()
        }
    }

    fun loadAlbumCreators(): List<ScannedAudio> {
        val list = mutableListOf<ScannedAudio>()

        conn.createStatement().use { st ->
            val rs = st.executeQuery("""
            SELECT * FROM audio WHERE album_creator = 1
        """.trimIndent())

            while (rs.next()) {
                list += ScannedAudio(
                    path = Path(rs.getString("path")),
                    title = rs.getString("title"),
                    artist = rs.getString("artist"),
                    album = rs.getString("album"),
                    year = rs.getString("year"),
                    pos = rs.getString("pos"),
                    artworkPath = rs.getString("artwork_path")?.let { Path(it) },
                    albumCreator = true
                )
            }
        }
        return list
    }

    fun hasAlbumCreator(albumKey: String): Boolean {
        conn.prepareStatement(
            "SELECT 1 FROM audio WHERE album_key = ? AND album_creator = 1 LIMIT 1"
        ).use { ps ->
            ps.setString(1, albumKey)
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    fun loadAll(): Map<Path, ScannedAudio> {
        val map = mutableMapOf<Path, ScannedAudio>()

        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT * FROM audio")
            while (rs.next()) {
                val path = Path(rs.getString("path"))
                val audio = ScannedAudio(
                    path = path,
                    title = rs.getString("title"),
                    artist = rs.getString("artist"),
                    album = rs.getString("album"),
                    year = rs.getString("year"),
                    pos = rs.getString("pos"),
                    artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                )
                map[path] = audio
            }
        }
        return map
    }

    fun tracksByAlbum(albumKey: String): List<ScannedAudio> {
        val list = mutableListOf<ScannedAudio>()

        conn.prepareStatement("""
        SELECT * FROM audio WHERE album_key = ?
    """.trimIndent()).use { ps ->
            ps.setString(1, albumKey)
            val rs = ps.executeQuery()
            while (rs.next()) {
                list += ScannedAudio(
                    path = Path(rs.getString("path")),
                    title = rs.getString("title"),
                    artist = rs.getString("artist"),
                    album = rs.getString("album"),
                    year = rs.getString("year"),
                    pos = rs.getString("pos"),
                    artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                )
            }
        }
        return list
    }

    fun deleteByRoot(root: Path) {
        conn.prepareStatement(
            """
        DELETE FROM audio
        WHERE path LIKE ?
        """.trimIndent()
        ).use { ps ->
            ps.setString(1, root.toString() + "%")
            ps.executeUpdate()
        }
    }
}
