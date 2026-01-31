package org.example

import kotlinx.coroutines.launch
import org.example.audioindex.ScannedAudio
import org.example.bass.bassController.PlayerController
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/* ─── ИНТЕРФЕЙСЫ MPRIS2 (Не менять, это стандарт Linux) ─── */

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    fun Raise() {}
    fun Quit() {}
    val CanQuit: Boolean get() = false
    val CanRaise: Boolean get() = false
    val Identity: String get() = "BassPlayer"
    val DesktopEntry: String get() = "bass-player"
    val SupportedUriSchemes: Array<String> get() = arrayOf("file")
    val SupportedMimeTypes: Array<String> get() = arrayOf("audio/mpeg", "audio/flac", "audio/ogg")
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
    fun OpenUri(Uri: String) {}

    val PlaybackStatus: String
    val Metadata: Map<String, Variant<*>>
    val Position: Long
    var Volume: Double
    val CanGoNext: Boolean
    val CanGoPrevious: Boolean
    val CanPlay: Boolean
    val CanPause: Boolean
    val CanSeek: Boolean
    val CanControl: Boolean get() = true
}

/* ─── РЕАЛИЗАЦИЯ СЕРВИСА ─── */

class MprisService(private val controller: PlayerController) : MediaPlayer2, MediaPlayer2Player {

    private val connection: DBusConnection = DBusConnectionBuilder.forSessionBus().build()
    private val objectPath = "/org/mpris/MediaPlayer2"
    private val interfaceName = "org.mpris.MediaPlayer2.Player"

    init {
        try {
            // Регистрируем имя в шине D-Bus
            connection.requestBusName("org.mpris.MediaPlayer2.BassPlayer")
            // Экспортируем наш объект
            connection.exportObject(objectPath, this)
            println("MPRIS2 Service registered successfully")
        } catch (e: Exception) {
            System.err.println("D-Bus registration failed: ${e.message}")
        }
    }

    /* ─── СВОЙСТВА (Чтение состояния плеера системой) ─── */

    override val PlaybackStatus: String
        get() = when {
            controller.state.value.isPlaying -> "Playing"
            controller.state.value.current != null -> "Paused"
            else -> "Stopped"
        }

    override val Metadata: Map<String, Variant<*>>
        get() {
            val item = controller.state.value.current ?: return mapOf()
            val audioInfo = controller.state.value.audioInfo

            val metadata = mutableMapOf<String, Variant<*>>()

            // Обязательный ID (тип "o" - Object Path)
            metadata["mpris:trackid"] = Variant(DBusPath("/org/mpris/MediaPlayer2/Track/${item.trackPath.hashCode()}"))

            // Длительность в микросекундах (тип "x" - Int64)
            metadata["mpris:length"] = Variant((controller.state.value.durationSec * 1_000_000).toLong())

            // Заголовок (тип "s" - String)
            metadata["xesam:title"] = Variant("dsds" ?: item.audioSource)

            // ИСПРАВЛЕНИЕ: Артисты должны быть массивом строк (тип "as")
            // Мы принудительно создаем типизированный массив и указываем сигнатуру "as"
            val artists = arrayOf("dsd" ?: "Unknown Artist")
            metadata["xesam:artist"] = Variant(artists, "as")

            // Альбом (тип "s")
            metadata["xesam:album"] = Variant("dsds" ?: "Unknown Album")

            // Обложка (тип "s" - URL)
            // item.artworkPath?.let { metadata["mpris:artUrl"] = Variant("file://$it") }

            return metadata
        }

    override val Position: Long
        get() = (controller.state.value.positionSec * 1_000_000).toLong()

    override var Volume: Double
        get() = controller.state.value.volume.toDouble()
        set(value) { /* Можно внедрить изменение громкости: controller.setVolume(value.toFloat()) */ }

    override val CanGoNext = true
    override val CanGoPrevious = true
    override val CanPlay = true
    override val CanPause = true
    override val CanSeek = true

    /* ─── МЕТОДЫ (Команды от системы к плееру) ─── */

    override fun Next() {
        controller.scope.launch {
            controller.requestNextItem?.invoke()?.let { controller.play(it) }
        }
    }

    override fun Previous() {
        controller.seek(0.0) // Перемотка в начало или ваша логика "назад"
    }

    override fun Pause() = controller.pause()

    override fun Play() = controller.resume()

    override fun PlayPause() {
        if (controller.state.value.isPlaying) controller.pause() else controller.resume()
    }

    override fun Stop() = controller.stop()

    override fun Seek(Offset: Long) {
        val newPos = controller.state.value.positionSec + (Offset / 1_000_000.0)
        controller.seek(newPos)
    }

    override fun SetPosition(TrackId: DBusPath, Position: Long) {
        controller.seek(Position / 1_000_000.0)
    }

    /* ─── УВЕДОМЛЕНИЯ ОБ ИЗМЕНЕНИЯХ (Вызывать из PlayerController) ─── */

    /**
     * Вызывать в PlayerController.play()
     */
    fun updateFullMetadata() {
        // Явно указываем сигнатуру "a{sv}" для метаданных
        emitPropertyChange("Metadata", Variant(Metadata, "a{sv}"))
        emitPropertyChange("PlaybackStatus", Variant(PlaybackStatus))
    }

    /**
     * Вызывать в PlayerController.pause() / resume()
     */
    fun updatePlaybackStatus() {
        emitPropertyChange("PlaybackStatus", Variant(PlaybackStatus))
    }

    /**
     * Отправляет сигнал PropertiesChanged в D-Bus
     */
    private fun emitPropertyChange(propName: String, value: Variant<*>) {
        try {
            // Проверяем, что соединение живо
            if (!connection.isConnected) return

            val signal = Properties.PropertiesChanged(
                objectPath,
                interfaceName,
                mapOf(propName to value),
                listOf()
            )
            connection.sendMessage(signal)
        } catch (e: Exception) {
            System.err.println("Failed to emit D-Bus signal: ${e.message}")
        }
    }

    override fun isRemote() = false
    override fun getObjectPath() = objectPath
}