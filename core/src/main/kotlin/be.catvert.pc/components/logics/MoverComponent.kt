package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.MoveAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.GameObjectMatrixContainer
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Component permettant d'ajouter la possibilité à un gameObject de se déplacer automatiquement dans une direction.
 * Si le gameObject rencontre un obstacle, il ira dans la direction opposé
 * @param orientation L'orientation dans laquelle le gameObject se déplacer (verticalement/horizontalement)
 * @see PhysicsComponent
 */
@RequiredComponent(PhysicsComponent::class)
@Description("Permet de déplacer automatiquement un game object sur un axe")
class MoverComponent(@ExposeEditor(max = 100f) var moveSpeedX: Int, @ExposeEditor(max = 100f) var moveSpeedY: Int, @ExposeEditor var reverse: Boolean = false, @ExposeEditor var holdGameObjects: Boolean = false) : Component(), Updeatable {
    @JsonCreator private constructor() : this(0, 0)

    @ExposeEditor
    var onUnReverseAction: Action = EmptyAction()
    @ExposeEditor
    var onReverseAction: Action = EmptyAction()

    private fun reverse() {
        if (nextReverse != !reverse) {
            nextReverse = !nextReverse
            if (nextReverse)
                onReverseAction(gameObject)
            else
                onUnReverseAction(gameObject)
        }
    }

    private var nextReverse = reverse

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)
        this.gameObject = gameObject

        state.getComponent<PhysicsComponent>()?.apply {
            onCollisionWith.register {
                if (it.triggerCallCount == 0) {
                    if (moveSpeedX != 0 && (it.side == BoxSide.Left || it.side == BoxSide.Right))
                        reverse()
                    else if (moveSpeedY != 0 && ((it.side == BoxSide.Up || !holdGameObjects) || it.side == BoxSide.Down))
                        reverse()
                }
            }
        }
    }

    override fun update() {
        val physicsComp = gameObject.getCurrentState().getComponent<PhysicsComponent>()

        val deltaMove = Utility.getDeltaTime() * Constants.physicsDeltaSpeed

        physicsComp?.move(true, if (reverse) (-moveSpeedX * deltaMove) else (moveSpeedX * deltaMove), if (reverse) (-moveSpeedY * deltaMove) else (moveSpeedY * deltaMove), gameObject)

        if (holdGameObjects) {
            physicsComp?.apply {
                // On double l'epsilon pour être sûr de la précision et éviter les problèmes liés au deltatime
                getCollisionsGameObjectOnSide(gameObject, BoxSide.Up, Constants.physicsEpsilon * 2f).forEach {
                    MoveAction(if (reverse) -moveSpeedX else moveSpeedX, if (reverse) -moveSpeedY else moveSpeedY, true).invoke(it)
                }
            }
        }

        gameObject.container.cast<GameObjectMatrixContainer>()?.matrixRect?.also {
            if (moveSpeedX != 0) {
                if (gameObject.box.left() == it.left() || gameObject.box.right() == it.right())
                    reverse()
            } else if (moveSpeedY != 0) {
                if (gameObject.box.top() == it.top())
                    reverse()
            }
        }

        reverse = nextReverse
    }
}