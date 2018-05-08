package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.basics.SoundComponent
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
 * @see SoundComponent
 */
@RequiredComponent(SoundComponent::class)
@Description("Permet de jouer un son spécifique")
class SoundAction(var soundIndex: Int) : Action(), UIImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(entity: Entity, container: EntityContainer) {
        playSound(entity)
    }

    private fun playSound(entity: Entity) {
        entity.getCurrentState().getComponent<SoundComponent>()?.playSound(soundIndex)
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            val sounds = entity.getCurrentState().getComponent<SoundComponent>()?.sounds ?: arrayListOf()

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("son", ::soundIndex, sounds.map { it.toString() })

                sameLine()
                if (button("jouer")) {
                    playSound(entity)
                }
            }
        }
    }

    override fun toString() = super.toString() + " - index : $soundIndex"
}