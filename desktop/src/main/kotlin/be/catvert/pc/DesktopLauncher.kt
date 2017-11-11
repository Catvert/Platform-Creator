package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.utils.JsonReader
import java.io.File
import java.io.FileReader

object DesktopLauncher {


    @JvmStatic fun main(arg: Array<String>) {
        val (width, height, vsync, fullscreen, soundVolume) = Utility.loadConfig()

        Utility.loadKeysConfig()

        val config = LwjglApplicationConfiguration()
        config.width = width
        config.height = height
        config.vSyncEnabled = vsync
        config.fullscreen = fullscreen
        config.title = "Platform Creator"
        config.resizable = false

        LwjglApplication(PCGame(vsync, soundVolume), config)
    }
}