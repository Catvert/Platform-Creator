package be.catvert.pc

import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import java.util.*

abstract class GameObjectContainer : Renderable, Updeatable, PostDeserialization {
    @JsonProperty("objects") protected val gameObjects: MutableSet<GameObject> = mutableSetOf()

    @JsonIgnore var allowRendering = true
    @JsonIgnore var allowUpdating = true

    @JsonIgnore fun getGameObjectsData() = gameObjects.toSet()

    fun findGameObjectByID(id: UUID): GameObject? = gameObjects.firstOrNull { it.id == id }

    open fun removeGameObject(gameObject: GameObject) {
        gameObjects.remove(gameObject)
        gameObject.container = null
    }

    open fun addGameObject(gameObject: GameObject): GameObject {
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

    override fun render(batch: Batch) {
        if (allowRendering)
            gameObjects.forEach { it.render(batch) }
    }

    override fun update() {
        if (allowUpdating)
            gameObjects.forEach { it.update() }
    }

    override fun onPostDeserialization() {
        gameObjects.forEach {
            it.container = this
        }
    }

    operator fun plusAssign(gameObject: GameObject) {
        addGameObject(gameObject)
    }
}
