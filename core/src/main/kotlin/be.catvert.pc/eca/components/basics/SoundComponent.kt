package be.catvert.pc.eca.components.basics

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.ui.UITextImpl
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.FileWrapper
import be.catvert.pc.utility.ResourceLoader
import be.catvert.pc.utility.toFileWrapper
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Component permettant d'ajouter des sons à une entité
 */
@Description("Ajoute la possibilité d'ajouter des sons à une entité")
class SoundComponent(var sounds: ArrayList<SoundData>) : Component(), ResourceLoader, UIImpl, UITextImpl {
    constructor(vararg sounds: SoundData) : this(arrayListOf(*sounds))
    @JsonCreator private constructor() : this(arrayListOf())

    class SoundData(var soundFile: FileWrapper, var levelResources: Boolean = false) : UIImpl, ResourceLoader {
        @JsonCreator private constructor() : this(Constants.defaultSoundPath.toFileWrapper())

        private var sound: Sound? = null

        fun play() {
            sound?.play(PCGame.soundVolume)
        }

        override fun loadResources() {
            sound = ResourceManager.getSound(soundFile.get())
        }

        override fun toString(): String = soundFile.get().nameWithoutExtension()

        override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(soundFile.get()))
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    if (ImGuiHelper.comboWithSettingsButton("son", index, soundsResources.map { it.nameWithoutExtension() }, {
                                checkbox("sons importés", ::levelResources)
                            })) {
                        soundFile = soundsResources[index[0]].toFileWrapper()
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

    override fun loadResources() {
        sounds.forEach { it.loadResources() }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("sounds", sounds, { it.toString() }, { SoundData(Constants.defaultSoundPath.toFileWrapper()) }, entity, level, editorSceneUI)
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.RED, "<-->")
        sounds.forEach {
            ImGuiHelper.textPropertyColored(Color.ORANGE, "son :", it.soundFile.get().nameWithoutExtension())
        }
        ImGuiHelper.textColored(Color.RED, "<-->")
    }
}