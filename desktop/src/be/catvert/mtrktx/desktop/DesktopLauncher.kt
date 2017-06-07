package be.catvert.mtrktx.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import be.catvert.mtrktx.MtrGame
import java.io.FileReader
import com.badlogic.gdx.utils.JsonReader
import ktx.log.debug

object DesktopLauncher {

    data class GameConfig(val width: Int, val height: Int, val vsync: Boolean, val fullscreen: Boolean)

    @JvmStatic fun main(arg: Array<String>) {
        val (width, height, vsync, fullscreen) = loadConfig()

        val config = LwjglApplicationConfiguration()
        config.width = width
        config.height = height
        config.vSyncEnabled = vsync
        config.fullscreen = fullscreen
        config.title = "Maryo The Return KTX"

        LwjglApplication(MtrGame(), config)
    }

    private fun loadConfig() : GameConfig {
        try {
            val root = JsonReader().parse(FileReader("config.json"))

            val screenWidth = root.getInt("width")
            val screenHeight = root.getInt("height")
            val vsync = root.getBoolean("vsync")
            val fullscreen = root.getBoolean("fullscreen")

            return GameConfig(screenWidth, screenHeight, vsync, fullscreen)
        } catch (e: Exception) {
           System.err.println("Erreur lors du chargement de la configuration du jeu ! Erreur : ${e.message}")
        }

        return GameConfig(1280, 720, false, false)
    }
}
