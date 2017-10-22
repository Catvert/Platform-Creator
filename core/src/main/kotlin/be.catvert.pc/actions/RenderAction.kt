package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RenderableComponent

class RenderAction(val action: RenderActions): Action {
    enum class RenderActions {
        FLIP_X, UNFLIP_X, FLIP_Y, UNFLIP_Y
    }

    override fun perform(gameObject: GameObject) {
        gameObject.getComponents().filter { it is RenderableComponent }.forEach {
            val renderComp = it as RenderableComponent
            when(action) {
                RenderAction.RenderActions.FLIP_X -> renderComp.flipX = true
                RenderAction.RenderActions.FLIP_Y -> renderComp.flipY = true
                RenderAction.RenderActions.UNFLIP_X -> renderComp.flipX = false
                RenderAction.RenderActions.UNFLIP_Y -> renderComp.flipY = false
            }
        }
    }
}