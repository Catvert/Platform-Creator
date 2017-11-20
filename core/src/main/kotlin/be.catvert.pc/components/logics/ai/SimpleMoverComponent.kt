package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.components.logics.CollisionSide
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore


class SimpleMoverComponent(orientation: SimpleMoverOrientation, reverse: Boolean) : UpdeatableComponent() {
    @JsonCreator private constructor(): this(SimpleMoverOrientation.HORIZONTAL, false)

    enum class SimpleMoverOrientation {
        HORIZONTAL, VERTICAL
    }

    private var firstAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT)
    private var secondAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT)

    @JsonIgnore private lateinit var gameObject: GameObject

    @ExposeEditor private var reverse = reverse
        set(value) {
            field = value
            if(value)
                onReverseAction(gameObject)
            else
                onUnReverseAction(gameObject)
        }

    @ExposeEditor var onUnReverseAction: Action = EmptyAction()
    @ExposeEditor var onReverseAction: Action = EmptyAction()

    @ExposeEditor var orientation = orientation
        set(value) {
            field = value
            updateActions()
        }

    private fun updateActions() {
        when(orientation) {
            SimpleMoverOrientation.HORIZONTAL -> {
                firstAction.physicsAction = PhysicsAction.PhysicsActions.GO_LEFT
                secondAction.physicsAction = PhysicsAction.PhysicsActions.GO_RIGHT
            }
            SimpleMoverOrientation.VERTICAL -> {
                firstAction.physicsAction = PhysicsAction.PhysicsActions.GO_UP
                secondAction.physicsAction = PhysicsAction.PhysicsActions.GO_DOWN
            }
        }
    }

    init {
        updateActions()
    }

    override fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {
        super.onGOAddToContainer(state, gameObject)

        this.gameObject = gameObject

        val physicsComp: PhysicsComponent = state.getComponent()!!
        physicsComp.onCollisionWith.register {
            when(orientation) {
                SimpleMoverOrientation.HORIZONTAL -> {
                    if(it.side == CollisionSide.OnLeft)
                        reverse = true
                    else if(it.side == CollisionSide.OnRight)
                        reverse = false
                }
                SimpleMoverOrientation.VERTICAL -> {
                    if(it.side == CollisionSide.OnUp)
                        reverse = true
                    else if(it.side == CollisionSide.OnDown)
                        reverse = false
                }
            }
        }
    }

    override fun update(gameObject: GameObject) {
            if(!reverse)
                firstAction(gameObject)
            else
                secondAction(gameObject)
    }
}