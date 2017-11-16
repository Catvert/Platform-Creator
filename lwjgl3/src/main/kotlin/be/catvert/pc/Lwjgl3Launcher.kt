package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import imgui.impl.LwjglGL3
import org.lwjgl.glfw.GLFW
import uno.glfw.GlfwWindow

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    private val configuration: Triple<Lwjgl3ApplicationConfiguration, Boolean, Float>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()
            val (width, height, vsync, fullscreen, soundVolume) = Utility.loadGameConfig()

            configuration.setTitle(Constants.gameTitle)
            if(fullscreen) {
                val primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setFullscreenMode(primaryMode)
            }
            else
                configuration.setWindowedMode(width, height)

            configuration.setResizable(false)
            configuration.useVsync(vsync)

            return Triple(configuration, vsync, soundVolume)
        }

    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(PCGame(configuration.second, configuration.third), configuration.first)
    }
}