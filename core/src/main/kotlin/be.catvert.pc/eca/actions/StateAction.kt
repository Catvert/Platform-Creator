package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.logics.MoverComponent
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'état d'une entité
 */
@Description("Permet de changer l'état d'une entité")
class StateAction(var stateIndex: Int, var usePreviousMoverDirection: Boolean = false) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(0)

    private fun checkHasMover(entity: Entity) = entity.getCurrentState().hasComponent<MoverComponent>() && entity.getStateOrDefault(stateIndex).hasComponent<MoverComponent>()

    override fun invoke(entity: Entity) {
        if (usePreviousMoverDirection && checkHasMover(entity)) {
            val previousMover = entity.getCurrentState().getComponent<MoverComponent>()!!
            entity.getStateOrDefault(stateIndex).getComponent<MoverComponent>()?.reverse = previousMover.reverse
        }
        entity.setState(stateIndex, true)
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("state", ::stateIndex, entity.getStates().map { it.name })
            }

            if (checkHasMover(entity)) {
                checkbox("utiliser la direction du mover précédent", ::usePreviousMoverDirection)
            }
        }
    }

    override fun toString() = super.toString() + " - { index : $stateIndex ; use previous mover : $usePreviousMoverDirection }"
}