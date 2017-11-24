package be.catvert.pc

import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import ktx.assets.toLocalFile
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Enumération des différentes touches utilisées dans le jeu
 */
enum class GameKeys(var key: Int, val description: String) {
    EDITOR_CAMERA_LEFT(Input.Keys.Q, "L'éditeur dirige la caméra vers la gauche"),
    EDITOR_CAMERA_RIGHT(Input.Keys.D, "L'éditeur dirige la caméra vers la droite"),
    EDITOR_CAMERA_UP(Input.Keys.Z, "L'éditeur dirige la caméra vers le haut"),
    EDITOR_CAMERA_DOWN(Input.Keys.S, "L'éditeur dirige la caméra vers le bas"),
    CAMERA_ZOOM_UP(Input.Keys.P, "Zoom de la caméra"),
    CAMERA_ZOOM_DOWN(Input.Keys.M, "Dézoom de la caméra"),
    CAMERA_ZOOM_RESET(Input.Keys.L, "Annuler le zoom/dézoom"),

    EDITOR_REMOVE_ENTITY(Input.Keys.DEL, "Enlever l'entité de l'éditeur"),
    EDITOR_COPY_MODE(Input.Keys.C, "Copier le mode entité dans l'éditeur"),
    EDITOR_GRID_MODE(Input.Keys.G, "Mode Grid dans l'éditeur"),
    EDITOR_APPEND_SELECT_ENTITIES(Input.Keys.CONTROL_LEFT, "Ajouter les entités sélectionnées dans l'éditeur"),
    EDITOR_SMARTCOPY_LEFT(Input.Keys.LEFT, "Copie intelligente de l'éditeur à gauche"),
    EDITOR_SMARTCOPY_RIGHT(Input.Keys.RIGHT, "Copie intelligente de l'éditeur à droite"),
    EDITOR_SMARTCOPY_UP(Input.Keys.UP, "Copie intelligente de l'éditeur en haut"),
    EDITOR_SMARTCOPY_DOWN(Input.Keys.DOWN, "Copie intelligente de l'éditeur en bas"),
    EDITOR_FLIPX(Input.Keys.X, "L'éditeur actionne x"),
    EDITOR_FLIPY(Input.Keys.Y, "L'éditeur actionne y"),
    EDITOR_UP_LAYER(Input.Keys.PLUS, "L'éditeur sélectionne la couche supérieure"),
    EDITOR_DOWN_LAYER(Input.Keys.MINUS, "L'éditeur sélectionne la couche inférieure"),
    EDITOR_SWITCH_RESIZE_MODE(Input.Keys.R, "L'editeur bascule e mode redimensionnement"),
    EDITOR_MOVE_ENTITY_LEFT(Input.Keys.LEFT, "L'éditeur se déplace d'un pixel vers la gauche"),
    EDITOR_MOVE_ENTITY_RIGHT(Input.Keys.RIGHT, "L'éditeur se déplace d'un pixel vers la droite"),
    EDITOR_MOVE_ENTITY_UP(Input.Keys.UP, "L'éditeur se déplace d'un pixel vers le haut"),
    EDITOR_MOVE_ENTITY_DOWN(Input.Keys.DOWN, "L'éditeur se déplace d'un pixel vers le bas"),

    DEBUG_MODE(Input.Keys.F12, "Mode debuggage"),

    GAME_SWITCH_GRAVITY(Input.Keys.G, "Activer/désactiver la gravité"),
    GAME_FOLLOW_CAMERA_PLAYER(Input.Keys.F, "la caméra suit le joueur on/off"),
    GAME_CAMERA_LEFT(Input.Keys.LEFT, "La caméra du jeu se dirige à gauche"),
    GAME_CAMERA_RIGHT(Input.Keys.RIGHT, "La caméra du jeu se dirige à droite"),
    GAME_CAMERA_UP(Input.Keys.UP, "La caméra du jeu se dirige vers le haut"),
    GAME_CAMERA_DOWN(Input.Keys.DOWN, "La caméra du jeu se dirige vers le bas"),

    GAME_PLAYER_LEFT(Input.Keys.Q, "Bouger le joueur à gauche"),
    GAME_PLAYER_RIGHT(Input.Keys.D, "Bouger le joueur à droite"),
    GAME_PLAYER_JUMP(Input.Keys.SPACE, "Saut du joueur"),
    GAME_PLAYER_GOD_UP(Input.Keys.Z, "Dieu dirige le joueur vers le haut"),
    GAME_PLAYER_GOD_DOWN(Input.Keys.S, "Dieu dirige le joueur vers le bas"),

    GAME_EDIT_LEVEL(Input.Keys.F2, "Modifier le niveau dans l'éditeur");

    companion object {
        /**
         * Charge le fichier de configuration des touches du jeu
         */
        fun loadKeysConfig() {
            if(Constants.keysConfigPath.toLocalFile().exists()) {
                try {
                    val root = JsonReader().parse(FileReader(Constants.keysConfigPath))

                    root["keys"].forEach {
                        val name = it.getString("name")
                        val key = it.getInt("key")

                        GameKeys.valueOf(name).key = key
                    }
                } catch (e: Exception) {
                    Log.error(e) { "Erreur lors du chargement du fichier de configuration des touches du jeu !" }
                }
            }
        }

        /**
         * Permet de sauvegarder la configuration des touches
         */
        fun saveKeysConfig(): Boolean {
            try {
                val writer = JsonWriter(FileWriter(Constants.keysConfigPath.toLocalFile().path(), false))
                writer.setOutputType(JsonWriter.OutputType.json)

                writer.`object`()
                writer.array("keys")

                GameKeys.values().forEach {
                    writer.`object`()

                    writer.name("name")
                    writer.value(it.name)

                    writer.name("key")
                    writer.value(it.key)

                    writer.pop()
                }

                writer.pop()
                writer.pop()

                writer.flush()
                writer.close()

                return true
            } catch (e: IOException) {
                Log.error(e) { "Erreur lors de l'enregistrement des configurations de touches !" }
                return false
            }
        }

    }
}