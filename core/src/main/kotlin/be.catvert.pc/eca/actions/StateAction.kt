package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.logics.MoverComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'état d'une entité
 */
@Description("Permet de changer l'état d'une entité")
class StateAction(var stateIndex: Int, var usePreviousMoverDirection: Boolean = false) : Action(), UIImpl {
    @JsonCreator private constructor() : this(0)

    private fun checkHasMover(entity: Entity) = entity.getCurrentState().hasComponent<MoverComponent>() && entity.getStateOrDefault(stateIndex).hasComponent<MoverComponent>()

    override fun invoke(entity: Entity, container: EntityContainer) {
        if (usePreviousMoverDirection && checkHasMover(entity)) {
            val previousMover = entity.getCurrentState().getComponent<MoverComponent>()!!
            entity.getStateOrDefault(stateIndex).getComponent<MoverComponent>()?.reverse = previousMover.reverse
        }
        entity.setState(stateIndex)
        entity.getCurrentState().startAction(entity, container)
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("état", ::stateIndex, entity.getStates().map { it.name })
            }

            if (checkHasMover(entity)) {
                checkbox("utiliser la direction du mover précédent", ::usePreviousMoverDirection)
            }
        }
    }

    override fun toString() = super.toString() + " - { index : $stateIndex ; use previous mover : $usePreviousMoverDirection }"
}