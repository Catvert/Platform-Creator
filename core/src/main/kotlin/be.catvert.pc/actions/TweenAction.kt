package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

class TweenAction(var tweenIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<TweenComponent>()?.startTween(gameObject, tweenIndex)
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            val tweens = gameObject.getCurrentState().getComponent<TweenComponent>()?.tweens ?: arrayListOf()
            functionalProgramming.withItemWidth(100f) {
                combo("tween", ::tweenIndex, tweens.map { it.name })
            }
        }
    }
}