package be.catvert.pc.components

import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import ktx.assets.getValue
import ktx.assets.loadOnDemand

class SoundComponent(val soundPath: String) : BasicComponent() {
    private val sound: Sound by PCGame.assetManager.loadOnDemand(Gdx.files.local(soundPath).path())

    fun playSound() {
        sound.play(PCGame.soundVolume)
    }
}