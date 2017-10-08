package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Size
import com.badlogic.gdx.math.Rectangle

class Prefab(val path: String, val author: String, val size: Size, val components: Set<Component>) {
    fun generate(position: Point, container: GameObjectContainer) = container.createGameObject(Rectangle(position.x.toFloat(), position.y.toFloat(), size.width.toFloat(), size.height.toFloat()), this) {
        components.forEach { addComponent(SerializationFactory.copy(it)) }
    }
}