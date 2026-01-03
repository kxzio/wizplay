import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    var dpiScale : Float = 1f,
    var windowSizeX : Int = 1280,
    var windowSizeY : Int = 720,

    val foldersToScan: List<String> = emptyList(),
    val themeColor: Int = Color(0xFF4CAF50).toArgb()
)

data class LocalConfig(
    var dpiScale: MutableFloatState = mutableFloatStateOf(1f),
    val foldersToScan: SnapshotStateList<String> = mutableStateListOf(),
    val themeColor: MutableState<Color> = mutableStateOf(Color(0xFF4CAF50)),
    var windowSizeX : Int = 500,
    var windowSizeY : Int = 500,

) {

    fun apply(config: Config) {
        dpiScale.value = config.dpiScale
        windowSizeX = config.windowSizeX
        windowSizeY = config.windowSizeY
        themeColor.value = Color(config.themeColor)
        foldersToScan.clear()
        foldersToScan.addAll(config.foldersToScan)
    }

    fun toConfig(): Config =
        Config(
            dpiScale = dpiScale.value,
            windowSizeY = windowSizeY,
            windowSizeX = windowSizeX,
            foldersToScan = foldersToScan.toList(),
            themeColor = themeColor.value.toArgb()
        )
}




private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

fun readConfig(path: String): Config {
    val file = File(path)
    if (!file.exists()) return Config()
    return try {
        val text = file.readText()
        json.decodeFromString<Config>(text)
    } catch (e: Exception) {
        println("Ошибка чтения конфига: ${e.message}")
        Config()
    }
}

fun writeConfig(path: String, config: Config) {
    val file = File(path)
    val text = json.encodeToString(config)
    try {
        file.writeText(text)
    } catch (e: Exception) {
        println("Ошибка записи конфига: ${e.message}")
    }
}

