package be.catvert.pc.serialization

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectInstanceCreator
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.actions.RemoveAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.components.logics.InputComponent
import com.badlogic.gdx.files.FileHandle
import com.google.gson.GsonBuilder

/**
 * Objet permettant de dé/sérialiser n'importe quel classe dé/sérialisable en JSON
 */
object SerializationFactory {
    private val runtimeTypeAdapterComponents = RuntimeTypeAdapterFactory
            .of(Component::class.java, "component")
            .registerSubtype(TextureComponent::class.java, "texture")
            .registerSubtype(AtlasComponent::class.java, "atlas")
            .registerSubtype(InputComponent::class.java, "input")

    private val runtimeTypeAdapterActions = RuntimeTypeAdapterFactory
            .of(Action::class.java, "action")
            .registerSubtype(MoveAction::class.java, "move")
            .registerSubtype(EmptyAction::class.java, "empty")
            .registerSubtype(RemoveAction::class.java, "remove")

    val gson = GsonBuilder()
            .registerTypeAdapter(GameObject::class.java, GameObjectInstanceCreator())
            .registerTypeAdapterFactory(runtimeTypeAdapterComponents)
            .registerTypeAdapterFactory(runtimeTypeAdapterActions)
            .registerTypeAdapterFactory(PostDeserializationAdapterFactory())
            .setPrettyPrinting()
            .create()

    fun <T> serialize(obj: T): String = gson.toJson(obj)
    inline fun <reified T> deserialize(json: String): T = gson.fromJson(json, T::class.java)

    inline fun <reified T> copy(obj: T): T = deserialize(serialize(obj))

    fun <T> serializeToFile(obj: T, file: FileHandle) = file.writeString(serialize(obj), false)
    inline fun <reified T> deserializeFromFile(file: FileHandle) = deserialize<T>(file.readString())
}