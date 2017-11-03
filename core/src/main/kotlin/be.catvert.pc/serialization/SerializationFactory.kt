package be.catvert.pc.serialization

import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON/Smile
 */
object SerializationFactory {
    val mapper = ObjectMapper().registerModules(KotlinModule(), Jdk8Module(), AfterburnerModule(), ParameterNamesModule(JsonCreator.Mode.PROPERTIES)).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)

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

