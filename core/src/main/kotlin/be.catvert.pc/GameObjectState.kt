package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.ResourceLoader
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass

/**
 * Représente un état d'un gameObject, cet état contient différents components.
 * Un état est par exemple quand le joueur à sa vie au maximum, et un autre état quand il lui reste 1 point de vie. Différents états permettent d'avoir différentes intéractions sur le gameObject au cour du temps.
 */
class GameObjectState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable, ResourceLoader {
    constructor(name: String, initState: GameObjectState.() -> Unit) : this(name) {
        initState()
    }

    @JsonCreator private constructor() : this("State")

    private lateinit var gameObject: GameObject

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    private var active = false

    var startAction: Action = EmptyAction()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onAddToContainer(gameObject: GameObject) {
        this.gameObject = gameObject
    }

    fun active(container: GameObjectContainer, triggerStartAction: Boolean) {
        components.forEach { it.onStateActive(gameObject, this, container) }

        if (triggerStartAction)
            startAction(gameObject)

        active = true
    }

    fun disabled() {
        active = false
    }

    fun addComponent(component: Component) {
        if (components.none { it.javaClass.isInstance(component) }) {
            components.add(component)

            if (active) {
                component.onStateActive(gameObject, this, gameObject.container!!)
            }
        }
    }

    fun removeComponent(component: Component) {
        components.remove(component)
    }

    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T }.cast()

    inline fun <reified T : Component> hasComponent(): Boolean = getComponent<T>() != null

    fun hasComponent(klass: KClass<out Component>) = getComponents().any { klass.isInstance(it) }

    override fun render(batch: Batch) {
        components.filter { it is Renderable && it.active }.forEach {
            (it as Renderable).render(batch)
        }
    }

    override fun update() {
        components.filter { it is Updeatable && it.active }.forEach {
            (it as Updeatable).update()
        }
    }

    override fun loadResources() {
        components.filter { it is ResourceLoader }.forEach {
            (it as ResourceLoader).loadResources()
        }
    }

    override fun toString(): String = name

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}