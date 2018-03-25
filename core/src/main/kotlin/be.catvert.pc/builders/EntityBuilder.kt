package be.catvert.pc.builders

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.RemoveEntityAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size

/**
 * Builder permettant de construire une entité
 * @see Entity
 */
class EntityBuilder(val tag: EntityTag, val defaultSize: Size, val name: String = tag) {
    private val defaultState = EntityState("default")
    private val states = mutableListOf<EntityState>()

    private var outOfMapAction: Action = RemoveEntityAction()

    private var layer = Constants.defaultLayer

    fun withDefaultState(stateBuilder: StateBuilder.() -> Unit): EntityBuilder {
        StateBuilder(defaultState).stateBuilder()
        return this
    }

    fun withState(name: String, stateBuilder: StateBuilder.() -> Unit): EntityBuilder {
        states.add(StateBuilder(EntityState(name)).apply(stateBuilder).build())
        return this
    }

    fun withOutOfMap(action: Action): EntityBuilder {
        outOfMapAction = action
        return this
    }

    fun withLayer(layer: Int): EntityBuilder {
        this.layer = layer
        return this
    }

    fun build() = Entity(tag, name, Rect(size = defaultSize), defaultState, null, *states.toTypedArray()).apply {
        this.onOutOfMapAction = this@EntityBuilder.outOfMapAction
        this.layer = this@EntityBuilder.layer
    }

    fun build(pos: Point, entityContainer: EntityContainer) = build().apply {
        box.position = pos
        entityContainer.addEntity(this)
    }
}

/**
 * Builder permettant de construire un état d'une entité
 * @see EntityState
 */
class StateBuilder(val state: EntityState) {
    fun withStartAction(action: Action): StateBuilder {
        state.startAction = action
        return this
    }

    fun withComponent(component: Component): StateBuilder {
        state.addComponent(component)
        return this
    }

    fun build(): EntityState = state
}