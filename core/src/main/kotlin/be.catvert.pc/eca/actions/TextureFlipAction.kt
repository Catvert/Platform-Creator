package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de flip horizontalement ou verticalement la texture d'une entité
 * @see TextureComponent
 */
@RequiredComponent(TextureComponent::class)
@Description("Permet d'appliquer un effet de miroir sur la texture actuelle d'une entité")
class TextureFlipAction(@UI var flipX: Boolean, @UI var flipY: Boolean) : Action() {
    @JsonCreator private constructor() : this(false, false)

    override fun invoke(entity: Entity) {
        entity.getCurrentState().getComponent<TextureComponent>()?.apply {
            this.flipX = this@TextureFlipAction.flipX
            this.flipY = this@TextureFlipAction.flipY
        }
    }
}