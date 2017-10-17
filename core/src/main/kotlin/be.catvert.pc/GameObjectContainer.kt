package be.catvert.pc

import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import java.util.*

abstract class GameObjectContainer(val useMatrix: Boolean = false) : Renderable, Updeatable {
    val gameObjects: MutableSet<GameObject> = mutableSetOf()

    var allowRendering = true
    var allowUpdating = true

    val matrixWidth = 1000
    val matrixHeight = 1000
    val matrixSizeCell = 300

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

    fun createGameObject(rectangle: Rect = Rect(), prefab: Prefab? = null, init: GameObject.() -> Unit = {}): GameObject {
        val go = GameObject(UUID.randomUUID(), mutableSetOf(), rectangle, this, prefab)
        go.init()

        addGameObject(go)

        return go
    }

    fun addContainer(gameObjectContainer: GameObjectContainer) {
        gameObjects.addAll(gameObjectContainer.gameObjects)
    }

    override fun render(batch: Batch) {
        if (allowRendering)
            gameObjects.forEach { it.render(batch) }
    }

    override fun update() {
        if (allowUpdating)
            gameObjects.forEach { it.update() }
    }


    operator fun plusAssign(gameObject: GameObject) {
        addGameObject(gameObject)
    }
}