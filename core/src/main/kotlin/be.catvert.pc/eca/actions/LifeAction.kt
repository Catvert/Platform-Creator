package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.logics.LifeComponent
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator


/**
 * Une action permettant d'ajouter ou de retirer des points de vies à une entité ayant le component LifeComponent
 * @see LifeComponent
 */
@RequiredComponent(LifeComponent::class)
@Description("Permet d'ajouter/supprimer des points de vie à une entité")
class LifeAction(@UI var action: LifeActions) : Action() {
    @JsonCreator private constructor() : this(LifeActions.REMOVE_LP)

    /**
     * Représente les différentes actions possibles au niveau des points de vie.
     */
    enum class LifeActions {
        ADD_LP, REMOVE_LP, ONE_SHOT
    }

    override fun invoke(entity: Entity) {
        entity.getCurrentState().getComponent<LifeComponent>()?.lifeAction(action)
    }

    override fun toString(): String = super.toString() + " - $action"
}