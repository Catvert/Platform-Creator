package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @see TextureComponent
 */
@RequiredComponent(TextureComponent::class)
@Description("Permet d'appliquer un effet miroir horizontal et/ou vertical sur la texture actuelle d'une entité")
class TextureFlipAction(@UI(customName = "miroir x") var flipX: Boolean, @UI(customName = "miroir y") var flipY: Boolean) : Action() {
    @JsonCreator private constructor() : this(false, false)

    override fun invoke(entity: Entity, container: EntityContainer) {
        entity.getCurrentState().getComponent<TextureComponent>()?.apply {
            this.flipX = this@TextureFlipAction.flipX
            this.flipY = this@TextureFlipAction.flipY
        }
    }
}