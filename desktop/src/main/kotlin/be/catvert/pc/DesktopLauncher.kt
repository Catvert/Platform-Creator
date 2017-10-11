package be.catvert.pc

import be.catvert.pc.utility.Constants
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.utils.JsonReader
import java.io.FileReader


object DesktopLauncher {

    data class GameConfig(val width: Int, val height: Int, val vsync: Boolean, val fullscreen: Boolean, val soundVolume: Float)

    @JvmStatic fun main(arg: Array<String>) {
        val (width, height, vsync, fullscreen, soundVolume) = loadConfig()

        loadKeysConfig()

        val config = LwjglApplicationConfiguration()
        config.width = width
        config.height = height
        config.vSyncEnabled = vsync
        config.fullscreen = fullscreen
        config.title = "Plateform Creator"
        config.resizable = false

        LwjglApplication(PCGame(vsync, soundVolume), config)
    }

    /**
     * Charge le fichier de configuration des touches du jeu
     */
    private fun loadKeysConfig() {
        try {
            val root = JsonReader().parse(FileReader(Constants.keysConfigPath))

            root["keys"].forEach {
                val name = it.getString("name")
                val key = it.getInt("key")

                GameKeys.valueOf(name).key = key
            }
        } catch (e: Exception) {
            System.err.println("Erreur lors du chargement du fichier de configuration des touches du jeu ! Erreur : ${e.message}")
        }
    }

    /**
     * Charge le fichier de configuration du jeu
     */
    private fun loadConfig(): GameConfig {
        try {
            val root = JsonReader().parse(FileReader(Constants.configPath))

            val screenWidth = root.getInt("width")
            val screenHeight = root.getInt("height")
            val vsync = root.getBoolean("vsync")
            val fullscreen = root.getBoolean("fullscreen")
            val soundVolume = root.getFloat("soundvolume")

            return GameConfig(screenWidth, screenHeight, vsync, fullscreen, soundVolume)
        } catch (e: Exception) {
            System.err.println("Erreur lors du chargement de la configuration du jeu ! Erreur : ${e.message}")
        }

        return GameConfig(1280, 720, false, false, 1f)
    }
}