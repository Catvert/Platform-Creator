package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.components.graphics.AnimationComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

class AnimationAction(var animationIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AnimationComponent>()?.also {
            it.currentAnimation = animationIndex
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            val animations = gameObject.getCurrentState().getComponent<AnimationComponent>()?.animations ?: arrayOf()

            functionalProgramming.withItemWidth(100f) {
                combo("animation", this@AnimationAction::animationIndex, animations.map { it.animationRegionName })
            }
        }
    }
}