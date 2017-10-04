package be.catvert.pc

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Launches the desktop (LWJGL3) application.  */

object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Launcher.createApplication()
    }

    fun createApplication(): Lwjgl3Application {
        return Lwjgl3Application(PCGame(), defaultConfiguration)
    }

    private val defaultConfiguration: Lwjgl3ApplicationConfiguration
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()
            configuration.setTitle("Plateform Creator")
            configuration.setWindowedMode(1280, 720)
            configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
            return configuration
        }
}