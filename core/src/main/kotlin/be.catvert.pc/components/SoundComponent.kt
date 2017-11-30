package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile


class SoundComponent(vararg sounds: SoundData) : BasicComponent(), CustomEditorImpl {
    var sounds = arrayOf(*sounds)

    class SoundData(soundFile: FileHandle, var levelResources: Boolean = false) : CustomEditorImpl {
        @JsonCreator private constructor() : this(Constants.defaultSoundPath)

        var soundPath = soundFile.path()

        private var sound: Sound = PCGame.assetManager.loadOnDemand<Sound>(soundPath).asset

        fun play() { sound.play(PCGame.soundVolume) }

        fun updateSound(soundFile: FileHandle = soundPath.toLocalFile()) {
            this.soundPath = soundFile.path()
            sound = PCGame.assetManager.loadOnDemand<Sound>(soundPath).asset
        }

        override fun toString(): String = soundPath.toLocalFile().nameWithoutExtension()

        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(soundPath.toLocalFile()))
                functionalProgramming.withItemWidth(100f) {
                    if (combo("son", index, soundsResources.map { it.nameWithoutExtension() })) {
                        updateSound(soundsResources[index[0]])
                    }

                    sameLine()
                    if(button("jouer")) {
                        play()
                    }

                    sameLine()
                    checkbox("Sons importés", ::levelResources)
                }
            }
        }
    }

    fun playSound(soundIndex: Int) {
        if (soundIndex in sounds.indices)
            sounds[soundIndex].play()
    }

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        super.onAddToContainer(gameObject, container)

        sounds.forEach { it.updateSound() }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("sons", this::sounds, { SoundData(Constants.defaultSoundPath) }, {
            it.obj.insertImgui(it.obj.toString(), gameObject, level)
            false
        })
    }
}