package be.catvert.pc.eca.components.basics

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.ui.UITextImpl
import be.catvert.pc.utility.*
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Component permettant d'ajouter des sons à une entité
 */
@Description("Ajoute la possibilité d'ajouter des sons à une entité")
class SoundComponent(var sounds: ArrayList<SoundData>) : Component(), UIImpl, UITextImpl {
    constructor(vararg sounds: SoundData) : this(arrayListOf(*sounds))
    @JsonCreator private constructor() : this(arrayListOf())

    class SoundData(val sound: ResourceWrapper<Sound>, var levelResources: Boolean = false) : UIImpl {
        @JsonCreator private constructor() : this(resourceWrapperOf(FileWrapper.INVALID))

        fun play() {
            sound()?.play(PCGame.soundVolume)
        }

        override fun toString(): String = sound.path.toString()

        override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(sound))
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    if (ImGuiHelper.comboWithSettingsButton("son", index, soundsResources.map { it.path.toString() }, {
                                checkbox("sons importés", ::levelResources)
                            })) {
                        sound.path = soundsResources[index[0]].path
                    }
                }
            }
        }
    }

    /**
     * Permet de jouer le son spécifié
     */
    fun playSound(soundIndex: Int) {
        if (soundIndex in sounds.indices)
            sounds[soundIndex].play()
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGuiHelper.addImguiWidgetsArray("sounds", sounds, { it.toString() }, { SoundData(resourceWrapperOf(Constants.defaultSoundPath.toFileWrapper())) }, entity, level, editorUI)
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.RED, "<-->")
        sounds.forEach {
            ImGuiHelper.textPropertyColored(Color.ORANGE, "son :", it.sound.path.toString())
        }
        ImGuiHelper.textColored(Color.RED, "<-->")
    }
}