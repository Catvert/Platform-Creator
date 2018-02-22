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
 * Représente un état d'une entité, cet état contient différents components.
 * Un état est par exemple quand le joueur à sa vie au maximum, et un autre état quand il lui reste 1 point de vie. Différents états permettent d'avoir différentes interactions sur l'entité au cour du temps.
 */
class GameObjectState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable, ResourceLoader {
    @JsonCreator private constructor() : this("State")

    /**
     * Représente l'entité où cet état est implémenté.
     * Injecté lors de l'ajout de l'entité à son conteneur(par exemple le niveau), via la méthode
     * @see onAddToContainer
     */
    private lateinit var gameObject: GameObject

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    private var active = false

    /**
     * Action appelée lorsque cet état devient actif.
     */
    var startAction: Action = EmptyAction()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun onAddToContainer(gameObject: GameObject) {
        this.gameObject = gameObject
    }

    /**
     * Permet d'activer cet état.
     * @param triggerStartAction Permet de spécifier si l'action de départ doit être appelée.
     */
    fun active(container: GameObjectContainer, triggerStartAction: Boolean) {
        components.forEach { it.onStateActive(gameObject, this, container) }

        if (triggerStartAction)
            startAction(gameObject)

        active = true
    }

    /**
     * Permet de désactiver cet état.
     */
    fun disabled() {
        active = false
    }

    /**
     * Permet d'ajouter un component à cet état.
     * @see Component
     */
    fun addComponent(component: Component) {
        if (components.none { it.javaClass.isInstance(component) }) {
            components.add(component)

            if (active) {
                component.onStateActive(gameObject, this, gameObject.container!!)
            }
        }
    }

    /**
     * Permet de supprimer un component de cet état.
     * @see Component
     */
    fun removeComponent(component: Component) {
        components.remove(component)
    }

    /**
     * Permet d'obtenir un component présent dans cet état.
     * @return Le component ou null si le component n'existe pas.
     */
    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T }.cast()

    /**
     * Permet de vérifier si un component est présent dans cet état.
     */
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