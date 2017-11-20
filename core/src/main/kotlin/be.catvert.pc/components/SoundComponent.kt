package be.catvert.pc.components

import be.catvert.pc.PCGame
import com.badlogic.gdx.audio.Sound
import com.fasterxml.jackson.annotation.JsonCreator
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile


class SoundComponent(val sounds: Array<SoundData>) : BasicComponent() {
    @JsonCreator private constructor(): this(arrayOf())

    data class SoundData(var soundPath: String) {
        private var sound: Sound = PCGame.assetManager.loadOnDemand<Sound>(this.soundPath).asset

        fun play() = sound.play(PCGame.soundVolume)

        override fun toString(): String = soundPath.toLocalFile().nameWithoutExtension()
    }

    fun playSound(soundIndex: Int) {
        if(soundIndex in sounds.indices)
            sounds[soundIndex].play()
    }
}