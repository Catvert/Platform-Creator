package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import ktx.assets.toLocalFile

/**
 * Action permettant de jouer un son à partir d'un gameObject ayant un SoundComponent
 * @see SoundComponent
 */
class SoundAction(var soundIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<SoundComponent>()?.playSound(soundIndex)
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            val sounds = let {
                val list = mutableListOf<SoundComponent.SoundData>()
                val soundsData = gameObject.getCurrentState().getComponent<SoundComponent>()?.sounds
                if(soundsData != null)
                    list.addAll(soundsData)
                list.toList()
            }
            combo("son", this@SoundAction::soundIndex, sounds.map { it.toString() })
        }
    }
}