package be.catvert.pc.builders

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.GameObjectTag
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Size

/**
 * Builder permettant de construire un game object
 * @see GameObject
 */
class GameObjectBuilder(val tag: GameObjectTag, val defaultSize: Size, val name: String = tag) {
    private val defaultState = GameObjectState("default")
    private val states = mutableListOf<GameObjectState>()

    private var outOfMapAction: Action = RemoveGOAction()

    private var layer = 0

    fun withDefaultState(stateBuilder: StateBuilder.() -> Unit): GameObjectBuilder {
        StateBuilder(defaultState).stateBuilder()
        return this
    }

    fun withState(name: String, stateBuilder: StateBuilder.() -> Unit): GameObjectBuilder {
        states.add(StateBuilder(GameObjectState(name)).apply(stateBuilder).build())
        return this
    }

    fun withOutOfMap(action: Action): GameObjectBuilder {
        outOfMapAction = action
        return this
    }

    fun withLayer(layer: Int): GameObjectBuilder {
        this.layer = layer
        return this
    }

    fun build() = GameObject(tag, name, Rect(size = defaultSize), defaultState, null, *states.toTypedArray()).apply {
        this.onOutOfMapAction = this@GameObjectBuilder.outOfMapAction
        this.layer = this@GameObjectBuilder.layer
    }

    fun build(pos: Point, gameObjectContainer: GameObjectContainer) = build().apply {
        box.position = pos
        gameObjectContainer.addGameObject(this)
    }
}

/**
 * Builder permettant de construire un état d'un game object
 * @see GameObjectState
 */
class StateBuilder(val state: GameObjectState) {
    fun withStartAction(action: Action): StateBuilder {
        state.startAction = action
        return this
    }

    fun withComponent(component: Component): StateBuilder {
        state.addComponent(component)
        return this
    }

    fun build(): GameObjectState = state
}