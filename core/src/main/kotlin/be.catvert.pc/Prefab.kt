package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size
import java.util.*


class Prefab(val name: String, val custom: Boolean, val author: String, val tag: GameObject.Tag, val size: Size, val components: Set<Component>, val init: GameObject.() -> Unit = {}) {
    fun generate(position: Point, container: GameObjectContainer) = container.createGameObject(Rect(position, size), tag,this) {
        components.forEach { addComponent(SerializationFactory.copy(it)) }
        init()
    }
}