package be.catvert.pc

import be.catvert.pc.utility.Constants
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Enumération des différentes touches utilisées dans le jeu
 */
enum class GameKeys(var key: Int, val description: String) {
    EDITOR_CAMERA_LEFT(Input.Keys.Q, "[Éditeur] Déplacer la caméra vers la gauche"),
    EDITOR_CAMERA_RIGHT(Input.Keys.D, "[Éditeur] Déplacer la caméra vers la droite"),
    EDITOR_CAMERA_UP(Input.Keys.Z, "[Éditeur] Déplacer la caméra vers le haut"),
    EDITOR_CAMERA_DOWN(Input.Keys.S, "[Éditeur] Déplacer la caméra vers le bas"),

    EDITOR_REMOVE_ENTITY(Input.Keys.DEL, "[Éditeur] Supprimer une entité"),
    EDITOR_COPY_MODE(Input.Keys.C, "[Éditeur] Copier une entité"),
    EDITOR_GRID_MODE(Input.Keys.G, "[Éditeur] Mode grille"),
    EDITOR_APPEND_SELECT_ENTITIES(Input.Keys.CONTROL_LEFT, "[Éditeur] Ajouter une entité sélectionnée"),
    EDITOR_SMARTCOPY_LEFT(Input.Keys.LEFT, "[Éditeur] Copie intelligente à gauche"),
    EDITOR_SMARTCOPY_RIGHT(Input.Keys.RIGHT, "[Éditeur] Copie intelligente à droite"),
    EDITOR_SMARTCOPY_UP(Input.Keys.UP, "[Éditeur] Copie intelligente au-dessus"),
    EDITOR_SMARTCOPY_DOWN(Input.Keys.DOWN, "[Éditeur] Copie intelligente en-dessous"),
    EDITOR_FLIPX(Input.Keys.X, "[Éditeur] Miroir X (flip)"),
    EDITOR_FLIPY(Input.Keys.Y, "[Éditeur] Miroir Y (flip)"),
    EDITOR_UP_LAYER(Input.Keys.PLUS, "[Éditeur] Couche +"),
    EDITOR_DOWN_LAYER(Input.Keys.MINUS, "[Éditeur] Couche -"),
    EDITOR_SWITCH_RESIZE_MODE(Input.Keys.R, "[Éditeur] Changer le redimensionnement"),
    EDITOR_MOVE_ENTITY_LEFT(Input.Keys.LEFT, "[Éditeur] Déplacer l'entité vers la gauche"),
    EDITOR_MOVE_ENTITY_RIGHT(Input.Keys.RIGHT, "[Éditeur] Déplacer l'entité vers la droite"),
    EDITOR_MOVE_ENTITY_UP(Input.Keys.UP, "[Éditeur] Déplacer l'entité vers le haut"),
    EDITOR_MOVE_ENTITY_DOWN(Input.Keys.DOWN, "[Éditeur] Déplacer l'entité vers le bas"),
    EDITOR_ATLAS_PREVIOUS_FRAME(Input.Keys.LEFT, "[Éditeur] Atlas région précédente"),
    EDITOR_ATLAS_NEXT_FRAME(Input.Keys.RIGHT, "[Éditeur] Atlas région suivante"),
    EDITOR_TRY_LEVEL(Input.Keys.F2, "[Éditeur] Essayer le niveau"),

    GAME_PLAYER_LEFT(Input.Keys.Q, "[Jeu] Déplacer le joueur vers le gauche"),
    GAME_PLAYER_RIGHT(Input.Keys.D, "[Jeu] Déplacer le joueur vers la droite"),
    GAME_PLAYER_JUMP(Input.Keys.SPACE, "[Jeu] Faire sauter le joueur"),
    GAME_PLAYER_GOD_UP(Input.Keys.Z, "[Jeu] Déplacer le joueur vers le haut"),
    GAME_PLAYER_GOD_DOWN(Input.Keys.S, "[Jeu] Déplacer le joueur vers le bas"),

    GAME_EDIT_LEVEL(Input.Keys.F2, "[Jeu] Éditer le niveau"),

    CAMERA_ZOOM_RESET(Input.Keys.L, "[Caméra] Zoom réinitialiser"),

    DEBUG_MODE(Input.Keys.F12, "Mode débug");

    companion object {
        /**
         * Charge le fichier de configuration des touches du jeu
         */
        fun loadKeysConfig() {
            if (Constants.keysConfigPath.exists()) {
                try {
                    val root = JsonReader().parse(FileReader(Constants.keysConfigPath.path()))

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
                val writer = JsonWriter(FileWriter(Constants.keysConfigPath.path(), false))
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