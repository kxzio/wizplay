package org.example

import kotlinx.coroutines.launch
import org.example.bass.queue.QueueController
import org.example.bass.bassController.PlayerController
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    fun Raise() {}
    fun Quit() {}

    val CanQuit get() = false
    val CanRaise get() = false
    val Identity get() = "BassPlayer"
    val DesktopEntry get() = "bass-player"
    val SupportedUriSchemes get() = arrayOf("file")
    val SupportedMimeTypes get() = arrayOf(
        "audio/mpeg",
        "audio/flac",
        "audio/ogg",
        "audio/wav"
    )
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MediaPlayer2Player : DBusInterface {

    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(Offset: Long)
    fun SetPosition(TrackId: DBusPath, Position: Long)

    val PlaybackStatus: String
    val Metadata: Map<String, Variant<*>>
    val Position: Long
    var Volume: Double

    val CanGoNext: Boolean
    val CanGoPrevious: Boolean
    val CanPlay: Boolean
    val CanPause: Boolean
    val CanSeek: Boolean
    val CanControl get() = true

    val Rate: Double
    val MinimumRate: Double
    val MaximumRate: Double
}

/* ───────── SERVICE ───────── */

class MprisService(
    private val queue: QueueController,
    private val player: PlayerController
) : MediaPlayer2,
    MediaPlayer2Player,
    Properties {

    override val Rate = 1.0
    override val MinimumRate = 1.0
    override val MaximumRate = 1.0

    private val connection: DBusConnection =
        DBusConnectionBuilder.forSessionBus().build()

    private val objectPath = "/org/mpris/MediaPlayer2"
    private val playerIface = "org.mpris.MediaPlayer2.Player"

    init {
        connection.requestBusName("org.mpris.MediaPlayer2.BassPlayer")
        connection.exportObject(objectPath, this)

        player.onPauseOrResumeAction = {
            updatePlaybackStatus()
        }

        println("MPRIS registered")
    }

    /* ───────── STATUS ───────── */

    override val PlaybackStatus: String
        get() = when {
            player.state.value.isPlaying -> "Playing"
            queue.currentItem() != null -> "Paused"
            else -> "Stopped"
        }

    /* ───────── METADATA ───────── */

    override val Metadata: Map<String, Variant<*>>
        get() {
            val item = queue.currentItem() ?: return emptyMap()
            val info = player.state.value.audioInfo

            val map = mutableMapOf<String, Variant<*>>()

            val trackId = DBusPath(
                "/org/mpris/MediaPlayer2/Track/${item.id.hashCode().coerceAtLeast(0)}"
            )

            map["mpris:trackid"] = Variant(trackId, "o")

            map["mpris:length"] = Variant(
                (player.state.value.durationSec * 1_000_000).toLong(),
                "x"
            )

            map["xesam:title"] =
                Variant(item.track.title ?: item.track.path.toString(), "s")

            val artist =
                item.track.artist ?: "Unknown Artist"

            map["xesam:artist"] = Variant(arrayOf(artist), "as")

            val album =
                item.track.album ?: item.audioSource

            map["xesam:album"] = Variant(album, "s")

            item.track.artworkPath?.let {
                map["mpris:artUrl"] = Variant(it.toUri().toString(), "s")
            }

            return map
        }

    /* ───────── POSITION ───────── */

    override val Position: Long
        get() = (player.state.value.positionSec * 1_000_000).toLong()

    override var Volume: Double
        get() = player.state.value.volume.toDouble()
        set(value) {}

    override val CanGoNext = true
    override val CanGoPrevious = true
    override val CanPlay = true
    override val CanPause = true
    override val CanSeek = true

    /* ───────── COMMANDS ───────── */

    override fun Next() {
        queue.moveNext(false)
        updateAll()
    }

    override fun Previous() {
        queue.movePrev()
        updateAll()
    }

    override fun Pause() {
        player.pause()
        updatePlaybackStatus()
    }

    override fun Play() {
        player.resume()
        updatePlaybackStatus()
    }

    override fun PlayPause() {
        if (player.state.value.isPlaying) Pause() else Play()
    }

    override fun Stop() {
        player.stop()
        updatePlaybackStatus()
    }

    override fun Seek(offset: Long) {
        player.seek(player.state.value.positionSec + offset / 1_000_000.0)
    }

    override fun SetPosition(trackId: DBusPath, position: Long) {
        player.seek(position / 1_000_000.0)
    }

    /* ───────── GENERIC DBUS PROPERTIES (твоя версия API) ───────── */

    override fun <A : Any?> Get(iface: String?, prop: String?): A? {

        val v: Any? = when (iface) {

            "org.mpris.MediaPlayer2" -> when (prop) {
                "CanQuit" -> false
                "CanRaise" -> false
                "Identity" -> "BassPlayer"
                "DesktopEntry" -> "bass-player"
                "SupportedUriSchemes" -> arrayOf("file")
                "SupportedMimeTypes" -> arrayOf("audio/mpeg","audio/flac","audio/ogg","audio/wav")
                else -> null
            }

            playerIface -> when (prop) {
                "PlaybackStatus" -> PlaybackStatus
                "Metadata" -> Metadata
                "Position" -> Position
                "Volume" -> Volume
                "Rate" -> Rate
                "MinimumRate" -> MinimumRate
                "MaximumRate" -> MaximumRate
                "CanGoNext" -> true
                "CanGoPrevious" -> true
                "CanPlay" -> true
                "CanPause" -> true
                "CanSeek" -> true
                "CanControl" -> true
                else -> null
            }

            else -> null
        }

        @Suppress("UNCHECKED_CAST")
        return v as A?
    }

    override fun <A : Any?> Set(iface: String?, prop: String?, value: A?) {
        if (iface != playerIface) return
        if (prop == "Volume" && value is Double) {
            // подключишь если добавишь setVolume
        }
    }

    override fun GetAll(iface: String?): Map<String, Variant<*>> {

        if (iface == "org.mpris.MediaPlayer2") {
            return mapOf(
                "CanQuit" to Variant(false),
                "CanRaise" to Variant(false),
                "Identity" to Variant("BassPlayer"),
                "DesktopEntry" to Variant("bass-player"),
                "SupportedUriSchemes" to Variant(arrayOf("file"), "as"),
                "SupportedMimeTypes" to Variant(arrayOf("audio/mpeg","audio/flac","audio/ogg","audio/wav"), "as")
            )
        }

        if (iface == playerIface) {
            return mapOf(
                "PlaybackStatus" to Variant(PlaybackStatus),
                "Metadata" to Variant(Metadata, "a{sv}"),
                "Position" to Variant(Position),
                "Volume" to Variant(Volume),
                "Rate" to Variant(Rate),
                "MinimumRate" to Variant(MinimumRate),
                "MaximumRate" to Variant(MaximumRate),
                "CanGoNext" to Variant(true),
                "CanGoPrevious" to Variant(true),
                "CanPlay" to Variant(true),
                "CanPause" to Variant(true),
                "CanSeek" to Variant(true),
                "CanControl" to Variant(true)
            )
        }

        return emptyMap()
    }


    /* ───────── SIGNALS ───────── */

    fun updateAll() {
        emit("Metadata", Variant(Metadata, "a{sv}"))
        emit("PlaybackStatus", Variant(PlaybackStatus))
    }

    fun updatePlaybackStatus() {
        emit("PlaybackStatus", Variant(PlaybackStatus))
    }

    private fun emit(name: String, value: Variant<*>) {
        if (!connection.isConnected) return

        val signal = Properties.PropertiesChanged(
            objectPath,          // ← твоя версия API требует path
            playerIface,
            mapOf(name to value),
            listOf()
        )

        connection.sendMessage(signal)
    }

    override fun isRemote() = false
    override fun getObjectPath() = objectPath
}
