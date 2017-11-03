package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size
import java.util.*


class Prefab(val name: String, val author: String, val prefabGO: GameObject) {
    fun create(position: Point, container: GameObjectContainer? = null) = SerializationFactory.copy(prefabGO).apply {
        this.rectangle.position = position
        this.container = container
    }

    override fun toString(): String {
        return this.name
    }
}