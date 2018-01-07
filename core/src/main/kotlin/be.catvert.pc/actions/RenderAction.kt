package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'appliquer une action sur l'AtlasComponent d'un gameObject
 * @see AtlasComponent
 */

@RequiredComponent(AtlasComponent::class)
class RenderAction(@ExposeEditor var action: RenderActions) : Action {
    @JsonCreator private constructor() : this(RenderActions.FLIP_X)

    enum class RenderActions {
        FLIP_X, UNFLIP_X, FLIP_Y, UNFLIP_Y
    }

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.apply {
                    when (action) {
                        RenderAction.RenderActions.FLIP_X -> flipX = true
                        RenderAction.RenderActions.FLIP_Y -> flipY = true
                        RenderAction.RenderActions.UNFLIP_X -> flipX = false
                        RenderAction.RenderActions.UNFLIP_Y -> flipY = false
                    }
                }
    }
}