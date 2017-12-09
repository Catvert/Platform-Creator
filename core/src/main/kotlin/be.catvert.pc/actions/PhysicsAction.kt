package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'appliquer une action physique sur un gameObject ayant le component PhysicsComponent
 * @see PhysicsComponent
 */
class PhysicsAction(@ExposeEditor var physicsAction: PhysicsActions) : Action {
    @JsonCreator private constructor() : this(PhysicsActions.GO_LEFT)

    enum class PhysicsActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP;
    }

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<PhysicsComponent>()?.physicsActions?.add(physicsAction)
    }
}