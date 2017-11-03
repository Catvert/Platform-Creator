package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size
import java.util.*


class Prefab(val name: String, val custom: Boolean, val author: String, val tag: GameObject.Tag, val size: Size, val states: Set<GameObjectState>, val init: GameObject.() -> Unit = {}) {
    fun create(position: Point, container: GameObjectContainer) = container.createGameObject(Rect(position, size), tag,this) {
        states.forEach { this.addState(SerializationFactory.copy(it)) }
        init()
    }

    fun createWithoutContainer(position: Point) = GameObject(UUID.randomUUID(), Rect(position, size), tag, states.map { SerializationFactory.copy(it) }.toMutableSet(),null, this)
}