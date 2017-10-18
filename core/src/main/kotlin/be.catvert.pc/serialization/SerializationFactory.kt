package be.catvert.pc.serialization

import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON/Smile
 */
object SerializationFactory {
    val mapper = ObjectMapper().findAndRegisterModules().enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)

    fun <T> serialize(obj: T): String = mapper.writeValueAsString(obj)

    inline fun <reified T> deserialize(json: String): T {
        val obj = mapper.readValue(json, T::class.java)
        if(obj is PostDeserialization)
            obj.onPostDeserialization()
        return obj
    }

    inline fun <reified T> copy(obj: T): T = deserialize(serialize(obj))

    fun <T> serializeToFile(obj: T, file: FileHandle) = file.writeString(serialize(obj), false)
    inline fun <reified T> deserializeFromFile(file: FileHandle) = deserialize<T>(file.readString())
}