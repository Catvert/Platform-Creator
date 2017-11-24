package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

class TweenAction(var tweenIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<TweenComponent>()?.startTween(gameObject, tweenIndex)
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            val tweens = gameObject.getCurrentState().getComponent<TweenComponent>()?.tweenData ?: arrayOf()
            combo("tween", this@TweenAction::tweenIndex, tweens.map { it.name })
        }
    }
}