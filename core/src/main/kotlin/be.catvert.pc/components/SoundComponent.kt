package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile


class SoundComponent(var sounds: Array<SoundData>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(arrayOf())

    data class SoundData(var soundPath: String, var levelResources: Boolean = false) : CustomEditorImpl {
        private var sound: Sound = PCGame.assetManager.loadOnDemand<Sound>(this.soundPath.toLocalFile().path()).asset

        fun play() = sound.play(PCGame.soundVolume)

        fun updateSound(soundFile: FileHandle = this.soundPath.toLocalFile()) {
            this.soundPath = soundFile.path()
            sound = PCGame.assetManager.loadOnDemand<Sound>(this.soundPath).asset
        }

        override fun toString(): String = soundPath.toLocalFile().nameWithoutExtension()

        override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
            with(ImGui) {
                val soundsResources = if(levelResources) editorScene.level.resourcesSounds() else PCGame.gameSounds
                val index = intArrayOf(soundsResources.indexOf(soundPath.toLocalFile()))
                functionalProgramming.withItemWidth(100f) {
                    if (combo("son", index, soundsResources.map { it.nameWithoutExtension() })) {
                        updateSound(soundsResources[index[0]])
                    }
                }
                sameLine()
                checkbox("Sons importés", this@SoundData::levelResources)
            }
        }
    }

    fun playSound(soundIndex: Int) {
        if(soundIndex in sounds.indices)
            sounds[soundIndex].play()
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        editorScene.addImguiWidgetsArray(gameObject, "sons", { sounds }, { sounds = it }, { SoundData(Constants.defaultSoundPath) }, { sounds[it].toString() }, { ExposeEditorFactory.empty })
    }
}