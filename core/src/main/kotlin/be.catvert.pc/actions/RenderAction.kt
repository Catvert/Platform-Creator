package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class RenderAction(@ExposeEditor var action: RenderActions) : Action {
    @JsonCreator private constructor(): this(RenderActions.FLIP_X)

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