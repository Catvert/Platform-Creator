package be.catvert.pc.serialization

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.cast
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.smile.SmileFactory
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
        result.cast<PostDeserialization>()?.onPostDeserialization()
        return result
    }
}

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON/Smile
 */
object SerializationFactory {
    enum class MapperType {
        JSON, SMILE
    }

    val mapper: ObjectMapper = ObjectMapper(if (Constants.serializationType == MapperType.JSON) JsonFactory() else SmileFactory()).registerModules(
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

    fun <T> serializeToJson(obj: T): String = mapper.writeValueAsString(obj)
    fun <T> serializeToSmile(obj: T): ByteArray = mapper.writeValueAsBytes(obj)

    inline fun <reified T> deserializeFromJson(data: String): T = mapper.readValue(data, T::class.java)
    inline fun <reified T> deserializeFromSmile(data: ByteArray): T = mapper.readValue(data, T::class.java)

    inline fun <reified T> copy(obj: T): T = when(Constants.serializationType) {
        SerializationFactory.MapperType.JSON -> deserializeFromJson(serializeToJson(obj))
        SerializationFactory.MapperType.SMILE -> deserializeFromSmile(serializeToSmile(obj))
    }


    fun <T> serializeToFile(obj: T, file: FileHandle) = when (Constants.serializationType) {
        SerializationFactory.MapperType.JSON -> file.writeString(serializeToJson(obj), false)
        SerializationFactory.MapperType.SMILE -> file.writeBytes(serializeToSmile(obj), false)
    }

    inline fun <reified T> deserializeFromFile(file: FileHandle): T = when (Constants.serializationType) {
        SerializationFactory.MapperType.JSON -> deserializeFromJson(file.readString())
        SerializationFactory.MapperType.SMILE -> deserializeFromSmile(file.readBytes())
    }

}