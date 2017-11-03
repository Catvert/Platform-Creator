package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class GameObjectState(var name: String, @JsonIgnore var gameObject: GameObject?, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable {
    @JsonCreator private constructor(): this("State", null)

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    @JsonIgnore private val renderComponents = mutableSetOf<RenderableComponent>()

    @JsonIgnore private val updateComponents = mutableSetOf<UpdeatableComponent>()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun linkGameObject(gameObject: GameObject) {
        this.gameObject = gameObject
        components.forEach {
            addComponent(it)
        }
    }

    fun addComponent(component: Component) {
        components.add(component)

        if(gameObject != null) {
            linkCompToGO(component)
        }

        if (component is RenderableComponent)
            renderComponents.add(component)
        else if (component is UpdeatableComponent)
            updateComponents.add(component)
    }

    fun removeComponent(component: Component) {
        components.remove(component)

        if(component is RenderableComponent)
            renderComponents.remove(component)
        else if(component is UpdeatableComponent)
            updateComponents.remove(component)
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

    private fun linkCompToGO(component: Component) {
        if(gameObject != null) {
            component.linkGameObject(gameObject!!)
            if(gameObject!!.container != null)
                component.onGOAddToContainer(gameObject!!)
        }
    }

    override fun render(batch: Batch) {
        renderComponents.forEach { if (it.active) it.render(batch) }
    }

    override fun update() {
        updateComponents.forEach { if (it.active) it.update() }
    }

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}