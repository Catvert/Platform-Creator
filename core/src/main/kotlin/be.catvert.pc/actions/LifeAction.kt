package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.logics.LifeComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator


/**
 * Une action permettant d'ajouter ou de retirer des points de vies à un gameObject ayant le component LifeComponent
 * @see LifeComponent
 */
@RequiredComponent(LifeComponent::class)
class LifeAction(@ExposeEditor var action: LifeActions) : Action() {
    @JsonCreator private constructor() : this(LifeActions.REMOVE_LP)

    enum class LifeActions {
        ADD_LP, REMOVE_LP, ONE_SHOT
    }

    override fun invoke(gameObject: GameObject) {
        val lifeComponent = gameObject.getCurrentState().getComponent<LifeComponent>()
        when (action) {
            LifeActions.ADD_LP -> lifeComponent?.addLifePoint()
            LifeActions.REMOVE_LP -> lifeComponent?.removeLifePoint()
            LifeActions.ONE_SHOT -> lifeComponent?.kill()
        }
    }

    override fun toString(): String = super.toString() + " - $action"
}