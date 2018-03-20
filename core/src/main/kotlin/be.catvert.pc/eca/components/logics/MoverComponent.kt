package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.actions.MoveAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.EntityMatrixContainer
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Component permettant d'ajouter la possibilité à une entité de se déplacer automatiquement dans une direction.
 * Si l'entité rencontre un obstacle, elle ira dans la direction opposé
 * @param moveSpeedX Vitesse de déplacement horizontal
 * @param moveSpeedY Vitesse de déplacement vertical
 * @see PhysicsComponent
 */
@RequiredComponent(PhysicsComponent::class)
@Description("Permet de déplacer automatiquement une entité sur un axe")
class MoverComponent(@UI(max = 100f, customName = "vitesse x") var moveSpeedX: Int, @UI(max = 100f, customName = "vitesse y") var moveSpeedY: Int, @UI(customName = "retourné") var reverse: Boolean = false, @UI(customName = "retenir les entités") var holdEntities: Boolean = false) : Component(), Updeatable {
    @JsonCreator private constructor() : this(0, 0)

    @UI(customName = "quand non inversé")
    var onUnReverseAction: Action = EmptyAction()
    @UI(customName = "quand inversé")
    var onReverseAction: Action = EmptyAction()

    private fun reverse() {
        if (nextReverse != !reverse && entity.container != null) {
            nextReverse = !reverse
            if (nextReverse)
                onReverseAction(entity, entity.container!!)
            else
                onUnReverseAction(entity, entity.container!!)
        }
    }

    private var nextReverse = reverse

    override fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        super.onStateActive(entity, state, container)
        this.entity = entity

        state.getComponent<PhysicsComponent>()?.apply {
            onCollisionWith.register {
                if (it.triggerCallCount == 0) {
                    if (moveSpeedX != 0 && (it.side == BoxSide.Left || it.side == BoxSide.Right))
                        reverse()
                    else if (moveSpeedY != 0 && ((it.side == BoxSide.Up || !holdEntities) || it.side == BoxSide.Down))
                        reverse()
                }
            }
        }

        nextReverse = reverse
    }

    override fun update() {
        val physicsComp = entity.getCurrentState().getComponent<PhysicsComponent>()

        val deltaMove = Utility.getDeltaTime() * Constants.physicsDeltaSpeed

        physicsComp?.move(true, if (reverse) (-moveSpeedX * deltaMove) else (moveSpeedX * deltaMove), if (reverse) (-moveSpeedY * deltaMove) else (moveSpeedY * deltaMove))

        if (holdEntities && entity.container != null) {
            physicsComp?.apply {
                // On double l'epsilon pour être sûr de la précision et éviter les problèmes liés au deltatime
                getCollideEntitiesOnSide(entity, BoxSide.Up, Constants.physicsEpsilon * 2f).forEach {
                    MoveAction(if (reverse) -moveSpeedX else moveSpeedX, if (reverse) -moveSpeedY else moveSpeedY, true).invoke(it, entity.container!!)
                }
            }
        }

        entity.container.cast<EntityMatrixContainer>()?.matrixRect?.also {
            if (moveSpeedX != 0) {
                if (entity.box.left() == it.left() || entity.box.right() == it.right())
                    reverse()
            } else if (moveSpeedY != 0) {
                if (entity.box.top() == it.top())
                    reverse()
            }
        }

        reverse = nextReverse
    }
}