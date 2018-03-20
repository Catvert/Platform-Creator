package be.catvert.pc.managers

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.utility.ResourceWrapper
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.GdxRuntimeException

/**
 * Permet de gérer la musique
 */
object MusicsManager : Updeatable {
    private var music: Music? = null

    private val interpolation = Interpolation.fade

    private data class MusicInterpolation(var elapsedTime: Float = 0f, var progress: Float = 0f, val duration: Float = 0.5f)

    private var startMusicInterpolation: MusicInterpolation? = null
    private var stopMusicInterpolation: MusicInterpolation? = null

    private var onStopMusicInterpolationEnd: () -> Unit = {}

    private fun getMaxSoundVolume() = PCGame.soundVolume / 2f

    fun startMusic(music: Music, applyInterpolation: Boolean) {
        if (MusicsManager.music == null) {
            MusicsManager.music = music

            if (applyInterpolation) {
                MusicsManager.music?.volume = 0f
                startMusicInterpolation = MusicInterpolation()
            } else {
                MusicsManager.music?.volume = getMaxSoundVolume()
            }

            try {
                MusicsManager.music?.play()
            } catch (e: GdxRuntimeException) {
                Log.error(e) { "Une erreur est survenue lors de la lecture de la musique !" }
            }
        } else {
            fun launchMusic() {
                MusicsManager.music = null
                startMusic(music, applyInterpolation)
            }

            if (applyInterpolation) {
                stopMusicInterpolation = MusicInterpolation()
                onStopMusicInterpolationEnd = {
                    launchMusic()
                }
            } else {
                launchMusic()
            }
        }
    }

    fun startMusic(music: ResourceWrapper<Music>, applyInterpolation: Boolean) {
        music.invoke()?.apply {
            startMusic(this, applyInterpolation)
        }
    }

    override fun update() {
        if (startMusicInterpolation != null && startMusicInterpolation?.progress != 1f) {
            val startMusicInterpolation = startMusicInterpolation!!
            startMusicInterpolation.elapsedTime += Utility.getDeltaTime()
            startMusicInterpolation.progress = Math.min(1f, startMusicInterpolation.elapsedTime / startMusicInterpolation.duration)

            music?.volume = interpolation.apply(0f, getMaxSoundVolume(), startMusicInterpolation.progress)
        } else if (stopMusicInterpolation != null && stopMusicInterpolation?.progress != 1f) {
            val stopMusicInterpolation = stopMusicInterpolation!!
            stopMusicInterpolation.elapsedTime += Utility.getDeltaTime()
            stopMusicInterpolation.progress = Math.min(1f, stopMusicInterpolation.elapsedTime / stopMusicInterpolation.duration)

            music?.volume = interpolation.apply(getMaxSoundVolume(), 0f, stopMusicInterpolation.progress)

            if (stopMusicInterpolation.progress == 1f) {
                music?.stop()
                onStopMusicInterpolationEnd()
            }
        } else {
            music?.volume = getMaxSoundVolume()
        }
    }
}