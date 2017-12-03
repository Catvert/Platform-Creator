package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de jouer un son à partir d'un gameObject ayant un SoundComponent
 * @see SoundComponent
 */
class SoundAction(var soundIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(gameObject: GameObject) {
        playSound(gameObject)
    }

    private fun playSound(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<SoundComponent>()?.playSound(soundIndex)
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            val sounds = gameObject.getCurrentState().getComponent<SoundComponent>()?.sounds ?: arrayListOf()

            functionalProgramming.withItemWidth(100f) {
                combo("son", ::soundIndex, sounds.map { it.toString() })

                sameLine()
                if (button("jouer")) {
                    playSound(gameObject)
                }
            }
        }
    }
}