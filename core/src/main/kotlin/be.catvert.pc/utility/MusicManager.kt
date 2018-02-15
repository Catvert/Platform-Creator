package be.catvert.pc.utility

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException

object MusicManager : Updeatable, Disposable {
    private var musics = mutableMapOf<FileHandle, Music>()

    private var music: Music? = null

    private val interpolation = Interpolation.fade

    private data class MusicInterpolation(var elapsedTime: Float = 0f, var progress: Float = 0f, val duration: Float = 0.5f)

    private var startMusicInterpolation: MusicInterpolation? = null
    private var stopMusicInterpolation: MusicInterpolation? = null

    private var onStopMusicInterpolationEnd: () -> Unit = {}

    private fun getMaxSoundVolume() = PCGame.soundVolume / 2f

    fun startMusic(path: FileHandle, applyInterpolation: Boolean) {
        if (music == null) {
            try {
                music = musics.getOrPut(path) { Gdx.audio.newMusic(path) }
            } catch (e: GdxRuntimeException) {
                Log.error(e) { "Impossible de charger la musique : $path" }
            }

            if (applyInterpolation) {
                music?.volume = 0f
                startMusicInterpolation = MusicInterpolation()
            } else {
                music?.volume = getMaxSoundVolume()
            }

            music?.play()
        } else {
            fun launchMusic() {
                music = null
                startMusic(path, applyInterpolation)
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

    override fun update() {
        if (startMusicInterpolation != null && startMusicInterpolation?.progress != 1f) {
            val startMusicInterpolation = startMusicInterpolation!!
            startMusicInterpolation.elapsedTime += Utility.getDeltaTime()
            startMusicInterpolation.progress = Math.min(1f, startMusicInterpolation.elapsedTime / startMusicInterpolation.duration)

            music?.volume = interpolation.apply(0f, getMaxSoundVolume(), startMusicInterpolation.progress)
        }
        else if (stopMusicInterpolation != null && stopMusicInterpolation?.progress != 1f) {
            val stopMusicInterpolation = stopMusicInterpolation!!
            stopMusicInterpolation.elapsedTime += Utility.getDeltaTime()
            stopMusicInterpolation.progress = Math.min(1f, stopMusicInterpolation.elapsedTime / stopMusicInterpolation.duration)

            music?.volume = interpolation.apply(getMaxSoundVolume(), 0f, stopMusicInterpolation.progress)

            if (stopMusicInterpolation.progress == 1f) {
                music?.stop()
                onStopMusicInterpolationEnd()
            }
        }
        else {
            music?.volume = getMaxSoundVolume()
        }
    }

    override fun dispose() {
        musics.forEach { it.value.dispose() }
    }
}