package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui

/**
 * Action permettant de changer l'état d'un gameObject
 */
class StateAction(var stateIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(0)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().onEndStateAction(gameObject)
        gameObject.currentState = stateIndex
        gameObject.getCurrentState().onStartStateAction(gameObject)
    }

    @JsonIgnore private var currentStateComboIndex = 0
    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            combo("state", this@StateAction::currentStateComboIndex, gameObject.getStates().map { it.name })
        }
    }

}