package be.catvert.pc.containers

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.LifeAction
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

abstract class GameObjectContainer : Renderable, Updeatable, PostDeserialization {
    @JsonProperty("objects") protected val gameObjects: MutableSet<GameObject> = mutableSetOf()

    @JsonIgnore
    var allowRenderingGO = true
    @JsonIgnore
    var allowUpdatingGO = true

    @JsonIgnore
    var removeEntityBelowY0 = true

    @JsonIgnore private val removeGameObjects = mutableSetOf<GameObject>()

    @JsonIgnore
    fun getGameObjectsData() = gameObjects.toSet()

    fun findGameObjectByID(id: UUID): GameObject? = gameObjects.firstOrNull { it.id == id }

    fun findGameObjectsByTag(tag: GameObject.Tag): Set<GameObject> = gameObjects.filter { it.tag == tag }.toSet()

    open fun removeGameObject(gameObject: GameObject) {
        removeGameObjects.add(gameObject)
    }

    open fun addGameObject(gameObject: GameObject): GameObject {
        gameObjects.add(gameObject)
        gameObject.container = this

        return gameObject
    }

    fun createGameObject(rectangle: Rect = Rect(), tag: GameObject.Tag, initDefaultState: GameObjectState.() -> Unit = {}, initGO: GameObject.() -> Unit = {}): GameObject {
        val go = GameObject(tag, UUID.randomUUID(), rectangle, this, initDefaultState).apply(initGO)

        addGameObject(go)

        return go
    }

    protected open fun onRemoveGameObject(gameObject: GameObject) {}

    override fun render(batch: Batch) {
        if (allowRenderingGO)
            gameObjects.sortedBy { it.layer }.forEach { it.render(batch) }
    }

    override fun update() {
        gameObjects.forEach {
            if(allowUpdatingGO)
                it.update()

            if (removeEntityBelowY0 && it.position().y < 0) {
                LifeAction(LifeAction.LifeActions.ONE_SHOT).invoke(it)
                removeGameObject(it)
            }
        }

        if(removeGameObjects.isNotEmpty()) {
            removeGameObjects.forEach {
                onRemoveGameObject(it)
                gameObjects.remove(it)
                it.container = null
            }
            removeGameObjects.clear()
        }
    }

    override fun onPostDeserialization() {
        gameObjects.forEach {
            it.container = this
            it.onRemoveFromParent.register {
                removeGameObjects.add(it)
            }
        }
    }

    operator fun plusAssign(gameObject: GameObject) {
        addGameObject(gameObject)
    }
}
