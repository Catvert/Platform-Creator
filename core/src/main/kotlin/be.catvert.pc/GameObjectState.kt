package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.ResourceLoader
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class GameObjectState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable, ResourceLoader {
    @JsonCreator private constructor() : this("State")

    private lateinit var gameObject: GameObject

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    private val renderComponents = mutableSetOf<RenderableComponent>()

    private val updateComponents = mutableSetOf<LogicsComponent>()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        this.gameObject = gameObject
    }

    fun toggleActive(container: GameObjectContainer) {
        components.forEach { sortComponent(it); it.onStateActive(gameObject, this, container) }
    }

    fun addComponent(component: Component) {
        if (components.none { it.javaClass.isInstance(component) }) {
            components.add(component)
            sortComponent(component)
        }
    }

    fun removeComponent(component: Component) {
        components.remove(component)

        if (component is RenderableComponent)
            renderComponents.remove(component)
        else if (component is LogicsComponent)
            updateComponents.remove(component)
    }

    private fun sortComponent(component: Component) {
        if (component is RenderableComponent)
            renderComponents.add(component)
        else if (component is LogicsComponent)
            updateComponents.add(component)
    }

    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T }.cast()

    inline fun <reified T : Component> hasComponent(): Boolean = getComponent<T>() != null

    fun setFlipRenderable(x: Boolean, y: Boolean) {
        renderComponents.forEach {
            it.flipX = x
            it.flipY = y
        }
    }

    fun inverseFlipRenderable(inverseX: Boolean, inverseY: Boolean) {
        renderComponents.forEach {
            if (inverseX)
                it.flipX = !it.flipX
            if (inverseY)
                it.flipY = !it.flipY
        }
    }

    fun setAlphaRenderable(alpha: Float) {
        renderComponents.forEach {
            it.alpha = alpha
        }
    }

    override fun render(batch: Batch) {
        renderComponents.forEach { it.render(gameObject, batch) }
    }

    override fun update() {
        updateComponents.forEach {
            it.update(gameObject)
        }
    }

    override fun loadResources(assetManager: AssetManager) {
        components.forEach { it.loadResources(assetManager) }
    }

    override fun toString(): String = name

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}