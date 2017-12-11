package be.catvert.pc.components.basics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import ktx.assets.Asset
import ktx.assets.load
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

/**
 * Component permettant d'ajouter des sons à un gameObject
 */
class SoundComponent(var sounds: ArrayList<SoundData>) : BasicComponent(), CustomEditorImpl {
    constructor(vararg sounds: SoundData) : this(arrayListOf(*sounds))
    @JsonCreator private constructor() : this(arrayListOf())

    class SoundData(var soundFile: FileWrapper, var levelResources: Boolean = false) : CustomEditorImpl, ResourceLoader {
        @JsonCreator private constructor() : this(Constants.defaultSoundPath.toFileWrapper())

        private var sound: Sound? = null

        fun play() {
            sound?.play(PCGame.soundVolume)
        }

        override fun loadResources() {
            sound = ResourceManager.getSound(soundFile.get())
        }

        override fun toString(): String = soundFile.get().nameWithoutExtension()

        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(soundFile.get()))
                functionalProgramming.withItemWidth(100f) {
                    if (combo("son", index, soundsResources.map { it.nameWithoutExtension() })) {
                        soundFile = soundsResources[index[0]].toFileWrapper()
                    }

                    sameLine()
                    checkbox("Sons importés", ::levelResources)
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
        super.loadResources()
        sounds.forEach { it.loadResources() }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("sons", sounds, { SoundData(Constants.defaultSoundPath.toFileWrapper()) }, {
            it.obj.insertImgui(it.obj.toString(), gameObject, level)
            false
        })
    }
}