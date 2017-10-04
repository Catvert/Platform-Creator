package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.RenderableComponent
import be.catvert.pc.components.logics.UpdeatableComponent
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.google.gson.InstanceCreator
import java.lang.reflect.Type

class GameObject(val name: String, var rectangle: Rectangle = Rectangle()) : Updeatable, Renderable{

    @Transient
    var container: GameObjectContainer? = null

    private val components = mutableSetOf<Component>()

    @Transient
    private val renderComponents = mutableSetOf<RenderableComponent>()
    @Transient
    private val updateComponents = mutableSetOf<UpdeatableComponent>()

    fun getComponentsData() = components.toSet()

    fun addComponent(component: Component) {
        components.add(component)
        component.linkGameObject(this)

        if(component is RenderableComponent)
            renderComponents.add(component)
        else if(component is UpdeatableComponent)
            updateComponents.add(component)
    }

    inline fun <reified T : Component> getComponent(index: Int = 0): T? {
        val filtered = getComponentsData().filter { it is T }
        return if(filtered.size > index && index > -1) filtered[index] as T else null
    }

    inline fun <reified T : RenderableComponent> hasComponent(index: Int = 0): Boolean {
        return getComponent<T>(index) != null
    }

    override fun update() {
        updateComponents.forEach { it.update() }
    }

    override fun render(batch: Batch) {
        renderComponents.forEach { it.render(batch) }
    }

    fun removeFromParent() {
        container?.removeGameObject(this)
        container = null
    }

    fun reorganizeComponentsDeserialization() {
        val copyComponents = mutableSetOf<Component>()

        copyComponents.addAll(components)

        renderComponents.clear()
        updateComponents.clear()
        components.clear()

        copyComponents.forEach { addComponent(it) }
    }

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}

fun GameObjectContainer.createGameObject(name: String, rectangle: Rectangle = Rectangle(), init: GameObject.() -> Unit = {}): GameObject {
    val go = GameObject(name, rectangle)
    go.init()

    addGameObject(go)

    return go
}

class GameObjectInstanceCreator : InstanceCreator<GameObject> {
    override fun createInstance(type: Type?) = GameObject(name = "uname go")
}