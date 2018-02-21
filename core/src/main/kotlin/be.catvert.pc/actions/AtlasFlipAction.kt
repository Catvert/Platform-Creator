package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de flip horizontalement ou verticalement l'atlas d'un game object
 * @see AtlasComponent
 */
@RequiredComponent(AtlasComponent::class)
@Description("Permet de flip la texture d'une game object")
class AtlasFlipAction(@ExposeEditor var flipX: Boolean, @ExposeEditor var flipY: Boolean) : Action() {
    @JsonCreator private constructor() : this(false, false)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.apply {
            this.flipX = this@AtlasFlipAction.flipX
            this.flipY = this@AtlasFlipAction.flipY
        }
    }
}