package be.catvert.pc

import com.badlogic.gdx.math.Rectangle
import java.util.*

interface GameObjectContainer {
    val gameObjects: MutableSet<GameObject>

    fun findGameObjectByID(id: UUID): GameObject? = gameObjects.firstOrNull { it.id == id }

    fun removeGameObject(gameObject: GameObject) {
        gameObjects.remove(gameObject)
        gameObject.container = null
    }

    fun addGameObject(gameObject: GameObject): GameObject {
        gameObjects.add(gameObject)
        gameObject.container = this

        return gameObject
    }

    fun createGameObject(rectangle: Rectangle = Rectangle(), prefab: Prefab? = null, init: GameObject.() -> Unit = {}): GameObject {
        val go = GameObject(UUID.randomUUID(), rectangle, this, prefab)
        go.init()

        addGameObject(go)

        return go
    }

    fun addContainer(gameObjectContainer: GameObjectContainer) {
        gameObjects.addAll(gameObjectContainer.gameObjects)
    }

    operator fun plusAssign(gameObject: GameObject) { addGameObject(gameObject) }
}