package be.catvert.pc.components.logics.ai

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Component permettant d'ajouter la possibilité à un gameObject de se déplacer automatiquement dans une direction.
 * Si le gameObject rencontre un obstacle, il ira dans la direction opposé
 * @param orientation L'orientation dans laquelle le gameObject se déplacer (verticalement/horizontalement)
 */
class SimpleMoverAIComponent(orientation: SimpleMoverOrientation, reverse: Boolean = false, @ExposeEditor("hold game objects") var holdGameObjects: Boolean = false) : LogicsComponent() {
    @JsonCreator private constructor() : this(SimpleMoverOrientation.HORIZONTAL)

    enum class SimpleMoverOrientation {
        HORIZONTAL, VERTICAL
    }

    private var firstAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT)
    private var secondAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT)

    private lateinit var gameObject: GameObject

    @ExposeEditor private var reverse = reverse
        set(value) {
            field = value
            if (value)
                onReverseAction(gameObject)
            else
                onUnReverseAction(gameObject)
        }

    @ExposeEditor
    var onUnReverseAction: Action = EmptyAction()
    @ExposeEditor
    var onReverseAction: Action = EmptyAction()

    @ExposeEditor
    var orientation = orientation
        set(value) {
            field = value
            updateOrientation()
        }

    /**
     * Permet de mettre à jour les actions physiques selon l'orientation voulue
     */
    private fun updateOrientation() {
        when (orientation) {
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
        updateOrientation()
    }

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)
        this.gameObject = gameObject

        state.getComponent<PhysicsComponent>()?.apply {
            onCollisionWith.register {
                when (orientation) {
                    SimpleMoverOrientation.HORIZONTAL -> {
                        if (it.side == BoxSide.Left)
                            reverse = true
                        else if (it.side == BoxSide.Right)
                            reverse = false
                    }
                    SimpleMoverOrientation.VERTICAL -> {
                        if (it.side == BoxSide.Up)
                            reverse = true
                        else if (it.side == BoxSide.Down)
                            reverse = false
                    }
                }
            }
        }
    }

    override fun update(gameObject: GameObject) {
        if (holdGameObjects) {
            gameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
                val moveX = when(orientation) {
                    SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL -> if(reverse) moveSpeed else -moveSpeed
                    SimpleMoverAIComponent.SimpleMoverOrientation.VERTICAL -> 0
                }
                val moveY = when(orientation) {
                    SimpleMoverAIComponent.SimpleMoverOrientation.HORIZONTAL -> 0
                    SimpleMoverAIComponent.SimpleMoverOrientation.VERTICAL -> if(reverse) moveSpeed else -moveSpeed
                }

                getCollisionsGameObjectOnSide(gameObject, BoxSide.Up).forEach {
                    MoveAction(moveX, moveY, true).invoke(it)
                }
            }
        }

        if (!reverse)
            firstAction(gameObject)
        else
            secondAction(gameObject)

    }
}