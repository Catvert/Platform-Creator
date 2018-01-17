package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.logics.MoverComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'état d'un gameObject
 */
class StateAction(var stateIndex: Int, var usePreviousMoverDirection: Boolean = false) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(0)

    private fun checkHasMover(gameObject: GameObject) = gameObject.getCurrentState().hasComponent<MoverComponent>() && gameObject.getStates().elementAtOrNull(stateIndex)?.hasComponent<MoverComponent>() == true

    override fun invoke(gameObject: GameObject) {
        if (usePreviousMoverDirection && checkHasMover(gameObject)) {
            val previousMover = gameObject.getCurrentState().getComponent<MoverComponent>()!!
            gameObject.getStates().elementAtOrNull(stateIndex)?.getComponent<MoverComponent>()?.reverse = previousMover.reverse
        }
        gameObject.setState(stateIndex)
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                combo("state", ::stateIndex, gameObject.getStates().map { it.name })
            }

            if (checkHasMover(gameObject)) {
                checkbox("utiliser la direction du mover précédent", ::usePreviousMoverDirection)
            }
        }
    }

    override fun toString() = super.toString() + " - { index : $stateIndex ; use previous mover : $usePreviousMoverDirection }"


}