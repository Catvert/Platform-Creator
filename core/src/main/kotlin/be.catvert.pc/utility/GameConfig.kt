package be.catvert.pc.utility

import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*

data class GameConfig(val screenWidth: Int, val screenHeight: Int, val refreshRate: Int, val fullScreen: Boolean, val soundVolume: Float, val darkUI: Boolean, val locale: Locale) {
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
                    val screenRefreshRate = root.getInt("refreshRate")
                    val fullscreen = root.getBoolean("fullScreen")
                    val soundVolume = root.getFloat("soundVolume")
                    val darkUI = root.getBoolean("darkUI")
                    val locale = root.getString("locale")

                    return GameConfig(screenWidth, screenHeight, screenRefreshRate, fullscreen, soundVolume, darkUI, Locale.forLanguageTag(locale))
                } catch (e: Exception) {
                    System.err.println("Erreur lors du chargement de la configuration du jeu ! Erreur : ${e.message}")
                }
            }

            return GameConfig(1280, 720, 60, false, 1f, false, Locale.ROOT)
        }

        /**
         * Permet de sauvegarder la configuration du jeu
         */
        fun saveGameConfig(): Boolean {
            try {
                val writer = JsonWriter(FileWriter(Constants.configPath, false))
                writer.setOutputType(JsonWriter.OutputType.json)

                writer.`object`()

                val mode = Gdx.graphics.displayMode

                writer.name("width").value(if (Gdx.graphics.isFullscreen) mode.width else Gdx.graphics.width)
                writer.name("height").value(if (Gdx.graphics.isFullscreen) mode.height else Gdx.graphics.height)
                writer.name("refreshRate").value(mode.refreshRate)

                writer.name("fullScreen").value(Gdx.graphics.isFullscreen)
                writer.name("soundVolume").value(PCGame.soundVolume)
                writer.name("darkUI").value(PCGame.darkUI)
                writer.name("locale").value(PCGame.locale)
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