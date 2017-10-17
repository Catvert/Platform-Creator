package be.catvert.pc.serialization

import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON
 */
object SerializationFactory {
    val mapper = ObjectMapper(SmileFactory()).findAndRegisterModules().enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)

    fun <T> serialize(obj: T): ByteArray = mapper.writeValueAsBytes(obj)
    inline fun <reified T> deserialize(json: ByteArray): T = mapper.readValue(json, T::class.java)

    inline fun <reified T> copy(obj: T): T = deserialize(serialize(obj))

    fun <T> serializeToFile(obj: T, file: FileHandle) = file.writeBytes(serialize(obj), false)
    inline fun <reified T> deserializeFromFile(file: FileHandle) = deserialize<T>(file.readBytes())
}