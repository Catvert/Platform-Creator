package be.catvert.pc.serialization

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectInstanceCreator
import com.badlogic.gdx.files.FileHandle
import com.google.gson.GsonBuilder

object SerializationFactory {
    val gson = GsonBuilder()
            .registerTypeAdapter(GameObject::class.java, GameObjectInstanceCreator())
            .registerTypeAdapterFactory(PostDeserializationAdapterFactory())
            .setPrettyPrinting().create()

    fun<T> serialize(obj: T): String = gson.toJson(obj)
    inline fun <reified T> deserialize(json: String): T = gson.fromJson(json, T::class.java)

    inline fun <reified T> copy(obj: T): T = deserialize(serialize(obj))

    fun <T> serializeToFile(obj: T, file: FileHandle) = file.writeString(serialize(obj), false)
    inline fun<reified T> deserializeFromFile(file: FileHandle) = deserialize<T>(file.readString())
}