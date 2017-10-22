package be.catvert.pc

import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EndLevelScene
import be.catvert.pc.scenes.GameScene
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

class Level(val levelPath: String, val levelName: String, val gameVersion: GameVersion, var playerUUID: UUID?, var backgroundPath: String? = null) : GameObjectMatrixContainer() {
    companion object {
        fun newLevel(levelName: String): Level {
            val level = Level(Constants.levelDirPath + levelName + Constants.levelExtension, levelName, Constants.gameVersion, null)

            val player = PrefabFactory.Player.generate().generate(Point(100, 100), level)
            level.setPlayer(player)

            return level
        }

        fun loadFromFile(levelFile: FileHandle): Level? {
            try {
                val level = SerializationFactory.deserializeFromFile<Level>(levelFile)
                return if(level.gameVersion == Constants.gameVersion) level else null
            } catch(e: Exception) {
                Log.error(e) { "Erreur lors du chargement du niveau !" }
            }
            return null
        }
    }

    @JsonIgnore var applyGravity = true

    fun setPlayer(gameObject: GameObject) {
        playerUUID = gameObject.id
        followGameObject = gameObject
    }

    fun exitLevel(success: Boolean) {
        PCGame.setScene(EndLevelScene(levelPath, success))
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        followGameObject = if(playerUUID != null) findGameObjectByID(playerUUID!!) else null
    }
}
