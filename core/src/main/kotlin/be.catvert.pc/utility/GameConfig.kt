package be.catvert.pc.utility

import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

data class GameConfig(val screenWidth: Int, val screenHeight: Int, val fullScreen: Boolean, val soundVolume: Float, val darkUI: Boolean) {
    companion object {
        /**
         * Charge le fichier de configuration du jeu
         */
        fun loadGameConfig(): GameConfig {
            if (File(Constants.configPath).exists()) {
                try {
                    val root = JsonReader().parse(FileReader(Constants.configPath))

                    val screenWidth = root.getInt("width")
                    val screenHeight = root.getInt("height")
                    val fullscreen = root.getBoolean("fullScreen")
                    val soundVolume = root.getFloat("soundVolume")
                    val darkUI = root.getBoolean("darkUI")

                    return GameConfig(screenWidth, screenHeight, fullscreen, soundVolume, darkUI)
                } catch (e: Exception) {
                    System.err.println("Erreur lors du chargement de la configuration du jeu ! Erreur : ${e.message}")
                }
            }

            return GameConfig(1280, 720, true, 1f, false)
        }

        /**
         * Permet de sauvegarder la configuration du jeu
         */
        fun saveGameConfig(): Boolean {
            try {
                val writer = JsonWriter(FileWriter(Constants.configPath, false))
                writer.setOutputType(JsonWriter.OutputType.json)

                writer.`object`()

                writer.name("width").value(Gdx.graphics.width)
                writer.name("height").value(Gdx.graphics.height)
                writer.name("fullScreen").value(Gdx.graphics.isFullscreen)
                writer.name("soundVolume").value(PCGame.soundVolume)
                writer.name("darkUI").value(PCGame.darkUI)
                writer.pop()

                writer.flush()
                writer.close()

                return true
            } catch (e: IOException) {
                return false
            }
        }
    }
}