package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.logics.LifeComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator


/**
 * Une action permettant d'ajouter ou de retirer des points de vies à un gameObject ayant le component LifeComponent
 * @see LifeComponent
 */
@RequiredComponent(LifeComponent::class)
@Description("Permet d'ajouter/supprimer des points de vie à un game object")
class LifeAction(@ExposeEditor var action: LifeActions) : Action() {
    @JsonCreator private constructor() : this(LifeActions.REMOVE_LP)

    /**
     * Représente les différentes actions possibles au niveau des points de vie.
     */
    enum class LifeActions {
        ADD_LP, REMOVE_LP, ONE_SHOT
    }

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<LifeComponent>()?.lifeAction(action)
    }

    override fun toString(): String = super.toString() + " - $action"
}