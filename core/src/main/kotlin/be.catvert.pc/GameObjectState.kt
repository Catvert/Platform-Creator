package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass

class GameObjectState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable {
    @JsonCreator private constructor(): this("State")

    @JsonIgnore lateinit var gameObject: GameObject

    @ExposeEditor var onActiveAction: Action = EmptyAction()
    @ExposeEditor var onInactiveAction: Action = EmptyAction()

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    @JsonIgnore private val renderComponents = mutableSetOf<RenderableComponent>()

    @JsonIgnore private val updateComponents = mutableSetOf<LogicsComponent>()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        this.gameObject = gameObject
        components.forEach { sortComponent(it); it.onAddToContainer(gameObject, container) }
    }

    fun addComponent(component: Component) {
        if(components.none { it.javaClass.isInstance(component) }) {
            components.add(component)
            sortComponent(component)
        }
    }

    fun removeComponent(component: Component) {
        components.remove(component)

        if(component is RenderableComponent)
            renderComponents.remove(component)
        else if(component is LogicsComponent)
            updateComponents.remove(component)
    }

    private fun sortComponent(component: Component) {
        if (component is RenderableComponent)
            renderComponents.add(component)
        else if (component is LogicsComponent)
            updateComponents.add(component)
    }

    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T } as? T

    inline fun <reified T : Component> hasComponent(klass: KClass<T> = T::class): Boolean = getComponent<T>() != null

    fun active() {
        onActiveAction(gameObject)
        getComponents().forEach { it.onStateActive(gameObject) }
    }

    fun inactive() {
        onInactiveAction(gameObject)
        getComponents().forEach { it.onStateInactive(gameObject) }
    }

    fun setFlipRenderable(x: Boolean, y: Boolean) {
        getComponents().filter { it is RenderableComponent }.forEach {
            val renderComp = it as RenderableComponent
            renderComp.flipX = x
            renderComp.flipY = y
        }
    }

    fun inverseFlipRenderable(inverseX: Boolean, inverseY: Boolean) {
        getComponents().filter { it is RenderableComponent }.forEach {
            val renderComp = it as RenderableComponent
            if(inverseX)
                renderComp.flipX = !renderComp.flipX
            if(inverseY)
                renderComp.flipY = !renderComp.flipY
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

    override fun toString(): String = name

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}