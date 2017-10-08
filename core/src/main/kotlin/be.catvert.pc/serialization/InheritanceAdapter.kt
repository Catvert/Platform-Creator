package be.catvert.pc.serialization

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

abstract class InheritanceAdapter<T: Any>(var customSerializer: ((src: T, typeOfSrc: Type?, context: JsonSerializationContext) -> JsonElement)? = null,
                                          var customDeserializer: ((json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext) -> T)? = null) : JsonSerializer<T>, JsonDeserializer<T> {
    private val classNameStr = "class"
    private val instanceStr = "instance"

    final override fun serialize(src: T, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        val jsonObj = JsonObject()
        val className = src.javaClass.name

        jsonObj.addProperty(classNameStr, className)
        val element = customSerializer?.invoke(src, typeOfSrc, context) ?: context.serialize(src)
        jsonObj.add(instanceStr, element)

        return jsonObj
    }

    final override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): T {
        val jsonObj = json.asJsonObject
        val primitive = jsonObj.getAsJsonPrimitive(classNameStr)
        val className = primitive.asString

        val klass = try {
            Class.forName(className)
        } catch(e: ClassNotFoundException) {
            e.printStackTrace()
            throw JsonParseException(e.message)
        }

        val jsonAdapter = klass.annotations.find { it.annotationClass == JsonAdapter::class } as? JsonAdapter

        if(jsonAdapter != null) {
            if(jsonAdapter.value.isSubclassOf(InheritanceAdapter::class)) {
                try {
                    val adapter = jsonAdapter.value.createInstance() as InheritanceAdapter<T>
                    customDeserializer = adapter.customDeserializer
                } catch(e: ClassCastException) {
                    e.printStackTrace()
                }
            }
        }

        return customDeserializer?.invoke(jsonObj.get(instanceStr), typeOfT, context) ?: context.deserialize(jsonObj.get(instanceStr), klass)
    }
}