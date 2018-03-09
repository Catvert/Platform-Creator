package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.AtlasComponent
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de flip horizontalement ou verticalement l'atlas d'une entité
 * @see AtlasComponent
 */
@RequiredComponent(AtlasComponent::class)
@Description("Permet d'appliquer un effet de miroir sur l'atlas actuel d'une entité")
class AtlasFlipAction(@UI var flipX: Boolean, @UI var flipY: Boolean) : Action() {
    @JsonCreator private constructor() : this(false, false)

    override fun invoke(entity: Entity) {
        entity.getCurrentState().getComponent<AtlasComponent>()?.apply {
            this.flipX = this@AtlasFlipAction.flipX
            this.flipY = this@AtlasFlipAction.flipY
        }
    }
}