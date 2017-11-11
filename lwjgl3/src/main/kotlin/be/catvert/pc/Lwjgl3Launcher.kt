package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {



    private val configuration: Triple<Lwjgl3ApplicationConfiguration, Boolean, Float>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()
            val (width, height, vsync, fullscreen, soundVolume) = Utility.loadConfig()

            Utility.loadKeysConfig()

            configuration.setTitle(Constants.gameTitle)
            configuration.setWindowedMode(width, height)
          //  configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
            configuration.setResizable(false)
            configuration.useVsync(vsync)

            return Triple(configuration, vsync, soundVolume)
        }

    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(PCGame(configuration.second, configuration.third), configuration.first)
    }
}