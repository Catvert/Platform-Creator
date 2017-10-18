package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.PhysicsComponent

/**
 * Enum permettant de choisir la prochaine "action" physique à appliquer sur l'entité
 */
enum class NextPhysicsActions {
    GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP
}

class PhysicsAction(val physicsAction: NextPhysicsActions) : Action {
    override fun perform(gameObject: GameObject) {
        gameObject.getComponent<PhysicsComponent>()?.nextActions?.add(physicsAction)
    }
}