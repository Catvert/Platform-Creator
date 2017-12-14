package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'état d'un gameObject
 */
class StateAction(var stateIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor() : this(0)

    override fun invoke(gameObject: GameObject) {
        if(gameObject.getCurrentStateIndex() != stateIndex)
            gameObject.setState(stateIndex)
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                combo("state", ::stateIndex, gameObject.getStates().map { it.name })
            }
        }
    }

}