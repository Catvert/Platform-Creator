package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'appliquer une action physique sur un gameObject ayant le component PhysicsComponent
 * @see PhysicsComponent
 */
@RequiredComponent(PhysicsComponent::class)
@Description("Permet d'effectuer une action physique sur un game object")
class PhysicsAction(@ExposeEditor var physicsAction: PhysicsActions) : Action() {
    @JsonCreator private constructor() : this(PhysicsActions.GO_LEFT)

    /**
     * Représente les différentes actions physiques possible.
     */
    enum class PhysicsActions {
        GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, JUMP, FORCE_JUMP;
    }

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<PhysicsComponent>()?.physicsActions?.add(physicsAction)
    }

    override fun toString() = super.toString() + " - $physicsAction"
}