package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.ResourceLoader
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import ktx.assets.Asset
import ktx.assets.getAsset
import ktx.assets.load
import ktx.assets.toLocalFile

/**
 * Component permettant d'ajouter des sons à un gameObject
 */
class SoundComponent(var sounds: ArrayList<SoundData>) : BasicComponent(), CustomEditorImpl {
    constructor(vararg sounds: SoundData) : this(arrayListOf(*sounds))
    @JsonCreator private constructor() : this(arrayListOf())

    class SoundData(soundFile: FileHandle, var levelResources: Boolean = false) : CustomEditorImpl, ResourceLoader {
        @JsonCreator private constructor() : this(Constants.defaultSoundPath)

        var soundPath = soundFile.path()

        private lateinit var sound: Asset<Sound>

        fun play() {
            sound.asset.play(PCGame.soundVolume)
        }

        override fun loadResources(assetManager: AssetManager) {
            sound = assetManager.load(soundPath)
            sound.load()
        }

        override fun toString(): String = soundPath.toLocalFile().nameWithoutExtension()

        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(soundPath.toLocalFile()))
                functionalProgramming.withItemWidth(100f) {
                    if (combo("son", index, soundsResources.map { it.nameWithoutExtension() })) {
                        soundPath = soundsResources[index[0]].path()
                        loadResources()
                    }

                    sameLine()
                    if (button("jouer")) {
                        play()
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

    override fun loadResources(assetManager: AssetManager) {
        super.loadResources(assetManager)
        sounds.forEach { it.loadResources(assetManager) }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("sons", sounds, { SoundData(Constants.defaultSoundPath) }, {
            it.obj.insertImgui(it.obj.toString(), gameObject, level)
            false
        })
    }
}