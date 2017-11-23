package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.containers.GameObjectContainer
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class LifeComponent(onDeathAction: Action, lifePointsActions: Set<Pair<Action, Action>>) : BasicComponent() {
    @JsonCreator private constructor(): this(RemoveGOAction(), setOf())

    @JsonIgnore
    private lateinit var gameObject: GameObject

    @JsonProperty("lpActions")
    private val lifePointsActions = lifePointsActions.toMutableSet().apply { add(EmptyAction() to onDeathAction) }

    @JsonProperty("lifePoint")
    private var lifePoint: Int = this.lifePointsActions.size

    fun removeLifePoint() {
        if(lifePoint > 1)
            --lifePoint
        lifePointsActions.elementAt(lifePoint - 1).second(gameObject)
    }

    fun kill() {
        lifePoint = 1
        lifePointsActions.elementAt(0).second(gameObject)
    }

    fun addLifePoint() {
        if(lifePointsActions.size > lifePoint) {
            ++lifePoint
            lifePointsActions.elementAt(lifePoint - 1).first(gameObject)
        }
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        this.gameObject = gameObject
    }
}