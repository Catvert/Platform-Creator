package be.catvert.pc

import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.RenderableComponent
import be.catvert.pc.components.logics.UpdeatableComponent
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.position
import be.catvert.pc.utility.size
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.google.gson.InstanceCreator
import java.lang.reflect.Type
import java.util.*

class GameObject(id: UUID,
                 var rectangle: Rectangle = Rectangle(),
                 @Transient var container: GameObjectContainer? = null,
                 @Transient val prefab: Prefab? = null) : Updeatable, Renderable, PostDeserialization {
    @Transient
    private val renderComponents = mutableSetOf<RenderableComponent>()
    @Transient
    private val updateComponents = mutableSetOf<UpdeatableComponent>()

    private val components = mutableSetOf<Component>()

    var id: UUID = id
        private set

    fun position() = rectangle.position()
    fun size() = rectangle.size()

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

    fun removeFromParent() {
        container?.removeGameObject(this)
        container = null
    }

    override fun update() {
        updateComponents.forEach { it.update() }
    }

    override fun render(batch: Batch) {
        renderComponents.forEach { it.render(batch) }
    }

    override fun postDeserialization() {
        val comps = mutableSetOf<Component>()
        components.forEach {
            comps += it
        }
        components.clear()
        renderComponents.clear()
        updateComponents.clear()

        comps.forEach {
            addComponent(it)
        }
    }

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}

class GameObjectInstanceCreator : InstanceCreator<GameObject> {
    override fun createInstance(type: Type?) = GameObject(UUID.randomUUID())
}