package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size
import com.badlogic.gdx.math.Rectangle
import java.util.*


class Prefab(val name: String, val custom: Boolean, val author: String, val size: Size, val components: Set<Component>) {
    fun generate(position: Point, container: GameObjectContainer) = container.createGameObject(Rect(position, size), this) {
        components.forEach { addComponent(SerializationFactory.copy(it)) }
    }

    fun generateWithoutContainer(position: Point) = GameObject(UUID.randomUUID(), components.let {
        it.map { SerializationFactory.copy(it) }.toMutableSet()
    }, Rect(position, size), null, this)
}