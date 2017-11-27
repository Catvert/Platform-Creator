package be.catvert.pc.containers

import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EndLevelScene
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import ktx.assets.toLocalFile
import java.util.*

class Level(val levelPath: String, val gameVersion: Float, var backgroundPath: String? = null) : GameObjectMatrixContainer() {
    private val levelTextures = levelPath.toLocalFile().parent().child("textures") to mutableListOf<FileHandle>()
    private val levelAtlas = levelPath.toLocalFile().parent().child("atlas") to mutableListOf<FileHandle>()
    private val levelSounds = levelPath.toLocalFile().parent().child("sounds") to mutableListOf<FileHandle>()

    @JsonIgnore
    var applyGravity = true

    @JsonIgnore
    var exit: (success: Boolean) -> Unit = { PCGame.setScene(EndLevelScene(levelPath, it)) }

    @JsonIgnore
    var scorePoints = 0

    @JsonIgnore
    var timer = 0
        private set
    private var timerDelta = 0f

    var followGameObjectID: UUID? = null

    init {
        if (!levelTextures.first.exists())
            levelTextures.first.mkdirs()
        else
            levelTextures.second.addAll(Utility.getFilesRecursivly(levelTextures.first, *Constants.levelTextureExtension))

        if (!levelAtlas.first.exists())
            levelAtlas.first.mkdirs()
        else
            levelAtlas.second.addAll(Utility.getFilesRecursivly(levelAtlas.first, *Constants.levelAtlasExtension))

        if (!levelSounds.first.exists())
            levelSounds.first.mkdirs()
        else
            levelSounds.second.addAll(Utility.getFilesRecursivly(levelSounds.first, *Constants.levelSoundExtension))
    }

    fun resourcesTextures() = levelTextures.second.toList()
    fun resourcesAtlas() = levelAtlas.second.toList()
    fun resourcesSounds() = levelSounds.second.toList()

    fun addResources(vararg resources: FileHandle) {
        resources.forEach {
            when {
                Constants.levelTextureExtension.contains(it.extension()) -> {
                    it.copyTo(levelTextures.first)
                    levelTextures.second.add(it)
                }
                Constants.levelAtlasExtension.contains(it.extension()) -> {
                    it.copyTo(levelAtlas.first)
                    it.parent().child(it.nameWithoutExtension() + ".png").copyTo(levelAtlas.first)
                    levelAtlas.second.add(it)
                }
                Constants.levelSoundExtension.contains(it.extension()) -> {
                    it.copyTo(levelSounds.first)
                    levelSounds.second.add(it)
                }
            }
        }
    }

    fun moveCameraToFollowGameObject(camera: OrthographicCamera, lerp: Boolean): Boolean {
        if (followGameObject != null) {
            val go = followGameObject!!

            val posX = Math.max(0f + camera.viewportWidth / 2f, go.position().x + go.size().width / 2f)
            val posY = Math.max(0f + camera.viewportHeight / 2f, go.position().y + go.size().height / 2f)

            val lerpX = MathUtils.lerp(camera.position.x, posX, 0.1f)
            val lerpY = MathUtils.lerp(camera.position.y, posY, 0.1f)

            camera.position.set(
                    if (lerp) Math.round(lerpX).toFloat() else Math.round(posX).toFloat(),
                    if (lerp) Math.round(lerpY).toFloat() else Math.round(posY).toFloat(), 0f)
        }

        return followGameObject != null
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        followGameObject = if (followGameObjectID != null) findGameObjectByID(followGameObjectID!!) else null
    }

    override fun update() {
        super.update()

        if (allowUpdatingGO) {
            timerDelta += Gdx.graphics.deltaTime
            if (timerDelta >= 1f) {
                ++timer
                timerDelta = 0f
            }
        }
    }

    fun deleteFiles() {
        if (!levelPath.toLocalFile().exists()) {
            levelPath.toLocalFile().parent().deleteDirectory()
        }
    }

    companion object {
        fun newLevel(levelName: String): Level {
            val levelDir = (Constants.levelDirPath.child(levelName))
            if (levelDir.exists()) {
                Log.warn { "Un niveau portant le même nom existe déjà !" }
            } else
                levelDir.mkdirs()
            val level = Level(levelDir.child(Constants.levelDataFile).path(), Constants.gameVersion, null)

            val player = PrefabFactory.Player.prefab.create(Point(100, 100), level)
            level.followGameObjectID = player.id

            return level
        }

        fun loadFromFile(levelDir: FileHandle): Level? {
            try {
                val level = SerializationFactory.deserializeFromFile<Level>(levelDir.child(Constants.levelDataFile))
                return if (level.gameVersion == Constants.gameVersion) level else null
            } catch (e: Exception) {
                Log.error(e) { "Erreur lors du chargement du niveau ! : ${e.message}" }
            }
            return null
        }
    }

}
