import kotlinx.serialization.json.Json
import repro.models.Recording
import repro.models.Speed

fun main() {
    val recording = Recording(plays = Recording.Plays._1, speed = Speed._1, volume = Recording.Volume._0Period5)
    println("encoded: " + Json.encodeToString(Recording.serializer(), recording))

    val decoded = runCatching { Json.decodeFromString(Recording.serializer(), """{"plays":1,"speed":1,"volume":0.5}""") }
    println("decoded: " + decoded.fold({ it.toString() }, { "${it::class.simpleName}: ${it.message?.lineSequence()?.first()}" }))
}
