package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.GameObjectMatrixContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Component permettant d'ajouter la possibilité à un gameObject de se déplacer automatiquement dans une direction.
 * Si le gameObject rencontre un obstacle, il ira dans la direction opposé
 * @param orientation L'orientation dans laquelle le gameObject se déplacer (verticalement/horizontalement)
 * @see PhysicsComponent
 */
@RequiredComponent(PhysicsComponent::class)
class MoverComponent(orientation: SimpleMoverOrientation, @ExposeEditor var reverse: Boolean = false, @ExposeEditor var holdGameObjects: Boolean = false) : Component(), Updeatable {
    @JsonCreator private constructor() : this(SimpleMoverOrientation.HORIZONTAL)

    enum class SimpleMoverOrientation {
        HORIZONTAL, VERTICAL
    }

    private var firstAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_LEFT)
    private var secondAction = PhysicsAction(PhysicsAction.PhysicsActions.GO_RIGHT)

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

    init {
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

    private fun reverse(value: Boolean) {
        reverse = value
        if (reverse)
            onReverseAction(gameObject)
        else
            onUnReverseAction(gameObject)
    }

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)
        this.gameObject = gameObject

        state.getComponent<PhysicsComponent>()?.apply {
            onCollisionWith.register {
                when (orientation) {
                    SimpleMoverOrientation.HORIZONTAL -> {
                        if (it.side == BoxSide.Left)
                            reverse(true)
                        else if (it.side == BoxSide.Right)
                            reverse(false)
                    }
                    SimpleMoverOrientation.VERTICAL -> {
                        if (it.side == BoxSide.Up)
                            reverse(true)
                        else if (it.side == BoxSide.Down)
                            reverse(false)
                    }
                }
            }
        }
    }

    override fun update() {
        if (holdGameObjects) {
            gameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
                val moveX = when (orientation) {
                    SimpleMoverOrientation.HORIZONTAL -> if (reverse) moveSpeed else -moveSpeed
                    SimpleMoverOrientation.VERTICAL -> 0
                }
                val moveY = when (orientation) {
                    SimpleMoverOrientation.HORIZONTAL -> 0
                    SimpleMoverOrientation.VERTICAL -> if (reverse) moveSpeed else -moveSpeed
                }

                getCollisionsGameObjectOnSide(gameObject, BoxSide.Up).forEach {
                    MoveAction(moveX, moveY, true).invoke(it)
                }
            }
        }

        gameObject.container.cast<GameObjectMatrixContainer>()?.matrixRect?.also {
            when(orientation) {
                MoverComponent.SimpleMoverOrientation.HORIZONTAL -> {
                    if(gameObject.position().x == 0)
                        reverse(true)
                    else if(gameObject.position().x + gameObject.size().width == it.width)
                        reverse(false)
                }
                MoverComponent.SimpleMoverOrientation.VERTICAL -> {
                    if(gameObject.position().y + gameObject.size().height == it.height)
                        reverse(true)
                }
            }
        }

        if (!reverse)
            firstAction(gameObject)
        else
            secondAction(gameObject)

    }
}