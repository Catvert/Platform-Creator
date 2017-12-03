package be.catvert.pc.serialization

import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import java.io.IOException

private class PostDeserializer(private val deserializer: JsonDeserializer<*>) : DelegatingDeserializer(deserializer) {
    override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>): JsonDeserializer<*> = deserializer

    @Throws(IOException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Any {
        val result = _delegatee.deserialize(jp, ctxt)
        (result as? PostDeserialization)?.onPostDeserialization()
        return result
    }
}

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON/Smile
 */
object SerializationFactory {
    val mapper: ObjectMapper = ObjectMapper().registerModules(
            KotlinModule(),
            Jdk8Module(),
            AfterburnerModule(),
            ParameterNamesModule(JsonCreator.Mode.PROPERTIES),
            SimpleModule().apply {
                setDeserializerModifier(object : BeanDeserializerModifier() {
                    override fun modifyDeserializer(config: DeserializationConfig?, beanDesc: BeanDescription?, deserializer: JsonDeserializer<*>): JsonDeserializer<*> = PostDeserializer(deserializer)
                })
            }).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE)

    fun <T> serialize(obj: T): String = mapper.writeValueAsString(obj)

    inline fun <reified T> deserialize(json: String): T = mapper.readValue(json, T::class.java)

    inline fun <reified T> copy(obj: T): T = deserialize(serialize(obj))

    fun <T> serializeToFile(obj: T, file: FileHandle) = file.writeString(serialize(obj), false)
    inline fun <reified T> deserializeFromFile(file: FileHandle) = deserialize<T>(file.readString())
}