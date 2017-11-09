package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class GameObjectState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable {
    @JsonCreator private constructor(): this("State")

    @JsonIgnore lateinit var gameObject: GameObject

    @ExposeEditor var onStartStateAction: Action = EmptyAction()
    @ExposeEditor var onEndStateAction: Action = RemoveGOAction()

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    @JsonIgnore private val renderComponents = mutableSetOf<RenderableComponent>()

    @JsonIgnore private val updateComponents = mutableSetOf<UpdeatableComponent>()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onGOAddToContainer(gameObject: GameObject) {
        this.gameObject = gameObject
        components.forEach { sortComponent(it); it.onGOAddToContainer(this, gameObject) }
    }

    fun addComponent(component: Component) {
       // if(components.none { it.javaClass.isInstance(component) }) {
            components.add(component)
            sortComponent(component)
      //  }
    }

    fun removeComponent(component: Component) {
        components.remove(component)

        if(component is RenderableComponent)
            renderComponents.remove(component)
        else if(component is UpdeatableComponent)
            updateComponents.remove(component)
    }

    private fun sortComponent(component: Component) {
        if (component is RenderableComponent)
            renderComponents.add(component)
        else if (component is UpdeatableComponent)
            updateComponents.add(component)
    }

    inline fun <reified T : Component> getComponent(index: Int = 0): T? {
        val filtered = getComponents().filter { it is T }
        return if (filtered.size > index && index > -1) filtered[index] as T else null
    }

    inline fun <reified T : Component> hasComponent(index: Int = 0): Boolean {
        return getComponent<T>(index) != null
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
        updateComponents.forEach { it.update(gameObject) }
    }

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}