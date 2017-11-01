package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import ktx.assets.getValue
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile

class SoundComponent(soundPath: FileHandle) : BasicComponent() {
    @JsonCreator private constructor(): this(Constants.noSoundPath.toLocalFile())

    var soundPath: String = soundPath.path()
        private set

    @JsonIgnore private var sound: Sound = PCGame.assetManager.loadOnDemand<Sound>(this.soundPath).asset

    fun updateSound(soundPath: FileHandle = this.soundPath.toLocalFile()) {
        this.soundPath = soundPath.path()

        sound = PCGame.assetManager.loadOnDemand<Sound>(this.soundPath).asset
    }

    fun playSound() {
        sound.play(PCGame.soundVolume)
    }

    override fun onGOAddToContainer(gameObject: GameObject) {
        super.onGOAddToContainer(gameObject)

        updateSound()
    }
}