package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Size

open class Structure(val size: Size, val gameObjects: ArrayList<Pair<GameObject, Point>>) {
    fun add(gameObject: GameObject, position: Point): Structure {
        gameObjects.add(gameObject to position)
        return this
    }

    fun addToContainer(container: GameObjectContainer, structurePosition: Point) {
        gameObjects.forEach {
            container.addGameObject(it.first.apply {
                box.position = Point(structurePosition.x + it.second.x, structurePosition.y + it.second.y)
            })
        }
    }
}

class ExpansibleStructure {

}