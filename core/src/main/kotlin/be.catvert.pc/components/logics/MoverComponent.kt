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
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.fasterxml.jackson.annotation.JsonCreator
import kotlin.math.roundToInt

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
        reverse = !reverse
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
                if(it.side != BoxSide.Up || !holdGameObjects)
                    reverse()
            }
        }
    }

    override fun update() {
        val physicsComp = gameObject.getCurrentState().getComponent<PhysicsComponent>()

        physicsComp?.tryMove(if(reverse) (-moveSpeedX * Gdx.graphics.deltaTime * 60f).roundToInt() else (moveSpeedX * Gdx.graphics.deltaTime * 60f).roundToInt(), if(reverse) (-moveSpeedY * Gdx.graphics.deltaTime * 60f).roundToInt() else (-moveSpeedY * Gdx.graphics.deltaTime * 60f).roundToInt(), gameObject)

        if (holdGameObjects) {
            physicsComp?.apply {
                getCollisionsGameObjectOnSide(gameObject, BoxSide.Up).forEach {
                    MoveAction(if(reverse) -moveSpeedX else moveSpeedX, if(reverse) -moveSpeedY else moveSpeedY, true).invoke(it)
                }
            }
        }

        gameObject.container.cast<GameObjectMatrixContainer>()?.matrixRect?.also {
            if(gameObject.position().x == 0 || gameObject.box.right() == it.right() || gameObject.box.top() == it.top())
                reverse()
        }
    }
}