package be.catvert.pc

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

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onAddToContainer(gameObject: GameObject) {
        this.gameObject = gameObject
    }

    fun toggleActive(container: GameObjectContainer) {
        components.forEach { it.onStateActive(gameObject, this, container) }
    }

    fun addComponent(component: Component) {
        if (components.none { it.javaClass.isInstance(component) }) {
            components.add(component)
        }
    }

    fun removeComponent(component: Component) {
        components.remove(component)
    }

    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T }.cast()

    inline fun <reified T : Component> hasComponent(): Boolean = getComponent<T>() != null

    override fun render(batch: Batch) {
        components.filter { it is Renderable }.forEach {
                    (it as Renderable).render(batch)
                }
    }

    override fun update() {
        components.filter { it is Updeatable }.forEach {
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