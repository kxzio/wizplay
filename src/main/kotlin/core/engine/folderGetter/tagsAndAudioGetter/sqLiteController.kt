package org.example.folderGetter.tagsAndAudioGetter

import org.example.audioindex.ScannedAudio
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.Path

class AudioDatabase(dbPath: Path) {

    private val conn: Connection =
        DriverManager.getConnection("jdbc:sqlite:${dbPath.toAbsolutePath()}")

    init {
        conn.autoCommit = true

        conn.createStatement().use { st ->

            st.execute("PRAGMA journal_mode=WAL")
            st.execute("PRAGMA synchronous=NORMAL")
            st.execute("PRAGMA temp_store=MEMORY")
            st.execute("PRAGMA foreign_keys=ON")

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS roots (
                    path TEXT PRIMARY KEY
                )
            """
            )

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS audio (
                    path TEXT PRIMARY KEY,
                    title TEXT,
                    artist TEXT,
                    album TEXT,
                    year TEXT,
                    pos TEXT,
                    artwork_path TEXT,
                    album_key TEXT,
                    disc TEXT
                )
            """
            )

            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_audio_album_key ON audio(album_key)"
            )

            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_audio_path ON audio(path)"
            )

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS playlists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """
            )

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS playlist_tracks (
                    playlist_id INTEGER NOT NULL,
                    track_path TEXT NOT NULL,
                    position INTEGER NOT NULL,

                    PRIMARY KEY (playlist_id, track_path),
                    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                    FOREIGN KEY (track_path) REFERENCES audio(path) ON DELETE CASCADE
                )
            """
            )

            st.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_playlist_tracks_playlist
                ON playlist_tracks(playlist_id)
                """
            )
        }
    }

    fun close() = conn.close()

    // ROOTS

    fun loadRoots(): Set<Path> =
        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT path FROM roots")
            buildSet {
                while (rs.next()) {
                    add(Path(rs.getString(1)))
                }
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
            "DELETE FROM roots WHERE path=?"
        ).use {
            it.setString(1, root.toString())
            it.executeUpdate()
        }
    }

    // AUDIO

    fun upsertAudio(a: ScannedAudio) {

        conn.prepareStatement(
            """
            INSERT INTO audio
            (path,title,artist,album,year,pos,artwork_path,album_key,disc)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT(path) DO UPDATE SET
                title=excluded.title,
                artist=excluded.artist,
                album=excluded.album,
                year=excluded.year,
                pos=excluded.pos,
                artwork_path=excluded.artwork_path,
                album_key=excluded.album_key,
                disc=excluded.disc
            """
        ).use { ps ->

            ps.setString(1, a.path.toString())
            ps.setString(2, a.title)
            ps.setString(3, a.artist)
            ps.setString(4, a.album)
            ps.setString(5, a.year)
            ps.setString(6, a.pos)
            ps.setString(7, a.artworkPath?.toString())
            ps.setString(8, a.albumKey)
            ps.setString(9, a.disc)

            ps.executeUpdate()
        }
    }

    // ⚡ ОПТИМИЗИРОВАННЫЙ ЗАПРОС АЛЬБОМОВ

    fun loadAlbumCreators(): List<ScannedAudio> {

        val list = ArrayList<ScannedAudio>(256)

        conn.createStatement().use { st ->

            val rs = st.executeQuery(
                """
                SELECT *
                FROM audio a
                WHERE a.path =
                    (SELECT MIN(path)
                     FROM audio
                     WHERE album_key = a.album_key)
                """
            )

            while (rs.next()) {

                list.add(
                    ScannedAudio(
                        path = Path(rs.getString("path")),
                        title = rs.getString("title"),
                        artist = rs.getString("artist"),
                        album = rs.getString("album"),
                        year = rs.getString("year"),
                        pos = rs.getString("pos"),
                        disc = rs.getString("disc"),
                        artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                    )
                )
            }
        }

        return list
    }

    fun loadAll(): Map<Path, ScannedAudio> {

        val map = HashMap<Path, ScannedAudio>(4096)

        conn.createStatement().use { st ->

            val rs = st.executeQuery("SELECT * FROM audio")

            while (rs.next()) {

                val path = Path(rs.getString("path"))

                map[path] = ScannedAudio(
                    path = path,
                    title = rs.getString("title"),
                    artist = rs.getString("artist"),
                    album = rs.getString("album"),
                    year = rs.getString("year"),
                    pos = rs.getString("pos"),
                    disc = rs.getString("disc"),
                    artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                )
            }
        }

        return map
    }

    fun tracksByAlbum(albumKey: String): List<ScannedAudio> {

        val list = ArrayList<ScannedAudio>(32)

        conn.prepareStatement(
            "SELECT * FROM audio WHERE album_key=?"
        ).use { ps ->

            ps.setString(1, albumKey)

            val rs = ps.executeQuery()

            while (rs.next()) {

                list.add(
                    ScannedAudio(
                        path = Path(rs.getString("path")),
                        title = rs.getString("title"),
                        artist = rs.getString("artist"),
                        album = rs.getString("album"),
                        year = rs.getString("year"),
                        pos = rs.getString("pos"),
                        disc = rs.getString("disc"),
                        artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                    )
                )
            }
        }

        return list
    }

    fun deleteByPath(path: Path) {

        conn.prepareStatement(
            "DELETE FROM audio WHERE path=?"
        ).use {

            it.setString(1, path.toString())
            it.executeUpdate()
        }
    }

    fun deleteByRoot(root: Path) {

        conn.prepareStatement(
            "DELETE FROM audio WHERE path LIKE ?"
        ).use {

            it.setString(1, root.toString() + "%")
            it.executeUpdate()
        }
    }

    fun pathsByRoot(root: Path): Set<Path> {

        conn.prepareStatement(
            "SELECT path FROM audio WHERE path LIKE ?"
        ).use { ps ->

            ps.setString(1, root.toString() + "%")

            val rs = ps.executeQuery()

            return buildSet {

                while (rs.next()) {
                    add(Path(rs.getString(1)))
                }
            }
        }
    }

    fun albumKeyByPath(path: Path): String? {

        conn.prepareStatement(
            "SELECT album_key FROM audio WHERE path=?"
        ).use { ps ->

            ps.setString(1, path.toString())

            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getString(1) else null
            }
        }
    }

    fun hasAlbumKey(albumKey: String): Boolean {

        conn.prepareStatement(
            "SELECT 1 FROM audio WHERE album_key=? LIMIT 1"
        ).use { ps ->

            ps.setString(1, albumKey)

            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    // PLAYLISTS

    fun createPlaylist(name: String): Long {

        conn.prepareStatement(
            "INSERT INTO playlists(name,created_at) VALUES (?,?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        ).use { ps ->

            ps.setString(1, name)
            ps.setLong(2, System.currentTimeMillis())

            ps.executeUpdate()

            ps.generatedKeys.use { rs ->
                if (rs.next()) return rs.getLong(1)
            }
        }

        error("Failed to create playlist")
    }

    fun deletePlaylist(id: Long) {

        conn.prepareStatement(
            "DELETE FROM playlists WHERE id=?"
        ).use {

            it.setLong(1, id)
            it.executeUpdate()
        }
    }

    fun renamePlaylist(id: Long, name: String) {

        conn.prepareStatement(
            "UPDATE playlists SET name=? WHERE id=?"
        ).use {

            it.setString(1, name)
            it.setLong(2, id)
            it.executeUpdate()
        }
    }

    fun loadPlaylists(): List<Pair<Long, String>> {

        conn.createStatement().use { st ->

            val rs = st.executeQuery(
                """
                SELECT id,name
                FROM playlists
                ORDER BY created_at
                """
            )

            return buildList {

                while (rs.next()) {

                    add(
                        rs.getLong("id") to rs.getString("name")
                    )
                }
            }
        }
    }

    fun playlistTrackCount(playlistId: Long): Int {

        conn.prepareStatement(
            "SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id=?"
        ).use { ps ->

            ps.setLong(1, playlistId)

            ps.executeQuery().use { rs ->

                rs.next()
                return rs.getInt(1)
            }
        }
    }

    fun addTrackToPlaylist(
        playlistId: Long,
        path: Path,
        position: Int
    ) {

        conn.prepareStatement(
            """
            INSERT OR REPLACE INTO playlist_tracks
            (playlist_id,track_path,position)
            VALUES (?,?,?)
            """
        ).use { ps ->

            ps.setLong(1, playlistId)
            ps.setString(2, path.toString())
            ps.setInt(3, position)

            ps.executeUpdate()
        }
    }

    fun removeTrackFromPlaylist(
        playlistId: Long,
        path: Path
    ) {

        conn.prepareStatement(
            """
            DELETE FROM playlist_tracks
            WHERE playlist_id=? AND track_path=?
            """
        ).use { ps ->

            ps.setLong(1, playlistId)
            ps.setString(2, path.toString())

            ps.executeUpdate()
        }
    }

    fun hasPlaylistId(playlistId: Long): Boolean {
        conn.prepareStatement( """ SELECT 1 FROM playlists WHERE id = ? LIMIT 1 """
        ).use {
            ps -> ps.setLong(1, playlistId)
            ps.executeQuery().use { rs -> return rs.next() }
        }
    }

    fun tracksInPlaylist(playlistId: Long): List<ScannedAudio> {

        val list = ArrayList<ScannedAudio>(64)

        conn.prepareStatement(
            """
            SELECT a.*
            FROM playlist_tracks pt
            JOIN audio a ON a.path = pt.track_path
            WHERE pt.playlist_id=?
            ORDER BY pt.position
            """
        ).use { ps ->

            ps.setLong(1, playlistId)

            val rs = ps.executeQuery()

            while (rs.next()) {

                list.add(
                    ScannedAudio(
                        path = Path(rs.getString("path")),
                        title = rs.getString("title"),
                        artist = rs.getString("artist"),
                        album = rs.getString("album"),
                        year = rs.getString("year"),
                        pos = rs.getString("pos"),
                        disc = rs.getString("disc"),
                        artworkPath = rs.getString("artwork_path")?.let { Path(it) }
                    )
                )
            }
        }

        return list
    }
}