package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point


class Prefab(val name: String, val author: String, val prefabGO: GameObject) {
    fun create(position: Point, container: GameObjectContainer? = null) = SerializationFactory.copy(prefabGO).apply {
        this.rectangle.position = position
        container?.addGameObject(this)
    }

    override fun toString(): String {
        return this.name
    }
}