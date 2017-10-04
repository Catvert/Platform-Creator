package be.catvert.pc.factories

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectInstanceCreator
import com.google.gson.GsonBuilder

object SerializationFactory {
    val gson = GsonBuilder()
            .registerTypeAdapter(GameObject::class.java, GameObjectInstanceCreator())
            .setPrettyPrinting().create()

    fun<T> serialize(obj: T): String = gson.toJson(obj)
    inline fun <reified T> deserialize(json: String): T = gson.fromJson(json, T::class.java)
}