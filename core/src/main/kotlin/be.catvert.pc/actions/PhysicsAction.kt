package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Enum permettant de choisir la prochaine "action" physique à appliquer sur l'entité
 */
enum class NextPhysicsActions {
    GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP;

    operator fun unaryMinus(): NextPhysicsActions = when(this) {
        NextPhysicsActions.GO_LEFT -> GO_RIGHT
        NextPhysicsActions.GO_RIGHT -> GO_LEFT
        NextPhysicsActions.GO_UP -> GO_DOWN
        NextPhysicsActions.GO_DOWN -> GO_UP
        NextPhysicsActions.GRAVITY -> GRAVITY
        NextPhysicsActions.JUMP -> JUMP
    }
}

class PhysicsAction(@ExposeEditor var physicsAction: NextPhysicsActions = NextPhysicsActions.GO_LEFT) : Action {
    @JsonCreator private constructor(): this(NextPhysicsActions.GO_LEFT)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<PhysicsComponent>()?.nextActions?.add(physicsAction)
    }
}