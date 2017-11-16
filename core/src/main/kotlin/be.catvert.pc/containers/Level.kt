package be.catvert.pc.containers

import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EndLevelScene
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

class Level(val levelPath: String, val levelName: String, val gameVersion: Float, var playerUUID: UUID?, var backgroundPath: String? = null) : GameObjectMatrixContainer() {
    companion object {
        fun newLevel(levelName: String): Level {
            val level = Level(Constants.levelDirPath + levelName + Constants.levelExtension, levelName, Constants.gameVersion, null)

            val player = PrefabFactory.Player.prefab.create(Point(100, 100), level)
            level.setPlayer(player)

            return level
        }

        fun loadFromFile(levelFile: FileHandle): Level? {
            try {
                val level = SerializationFactory.deserializeFromFile<Level>(levelFile)
                return if(level.gameVersion == Constants.gameVersion) level else null
            } catch(e: Exception) {
                Log.error(e) { "Erreur lors du chargement du niveau ! : ${e.message}" }
            }
            return null
        }
    }

    @JsonIgnore var applyGravity = true

    fun setPlayer(gameObject: GameObject) {
        playerUUID = gameObject.id
        followGameObject = gameObject
    }

    @JsonIgnore var exit : (success: Boolean) -> Unit = { PCGame.setScene(EndLevelScene(levelPath, it)) }

    fun moveCameraToFollowGameObject(camera: OrthographicCamera, lerp: Boolean): Boolean {
        if(followGameObject != null) {
            val go = followGameObject!!

            val posX = Math.max(0f + camera.viewportWidth / 2f, go.position().x + go.size().width / 2f)
            val posY = Math.max(0f + camera.viewportHeight / 2f, go.position().y + go.size().height / 2f)

            val lerpX = MathUtils.lerp(camera.position.x, posX, 0.1f)
            val lerpY = MathUtils.lerp(camera.position.y, posY, 0.1f)
            camera.position.set(if (lerp) lerpX else posX, if (lerp) lerpY else posY, 0f)
        }

        return followGameObject != null
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        followGameObject = if(playerUUID != null) findGameObjectByID(playerUUID!!) else null
    }
}
