package be.catvert.pc

import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.utils.JsonReader
import com.google.gson.Gson
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const
import java.io.FileReader

/** Launches the desktop (LWJGL3) application.  */

object Lwjgl3Launcher {
    data class GameConfig(val width: Int, val height: Int, val vsync: Boolean, val fullscreen: Boolean, val soundVolume: Float)

    @JvmStatic fun main(arg: Array<String>) {
        val (width, height, vsync, fullscreen, soundVolume) = loadConfig()

        loadKeysConfig()

        val config = Lwjgl3ApplicationConfiguration()

        config.setTitle(Constants.gameTitle)
        config.setResizable(true)
        config.setWindowedMode(width, height)
        config.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
        Lwjgl3Application(PCGame(fullscreen, vsync, soundVolume), config)
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