package be.catvert.pc.containers

import be.catvert.pc.*
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EndLevelScene
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import ktx.assets.toLocalFile
import kotlin.math.roundToInt


class Level(val levelPath: String, val gameVersion: Float, var background: Background?, var musicPath: FileWrapper?) : GameObjectMatrixContainer() {
    private val levelTextures = levelPath.toLocalFile().parent().child("textures") to mutableListOf<FileHandle>()
    private val levelAtlas = levelPath.toLocalFile().parent().child("atlas") to mutableListOf<FileHandle>()
    private val levelSounds = levelPath.toLocalFile().parent().child("sounds") to mutableListOf<FileHandle>()
    private val levelPrefabs = levelPath.toLocalFile().parent().child("prefabs") to mutableListOf<Prefab>()

    val tags = arrayListOf(*Tags.values().map { it.tag }.toTypedArray())

    val favoris = arrayListOf<GameObject>()

    @JsonIgnore
    var applyGravity = true

    @JsonIgnore
    var exit: (success: Boolean) -> Unit = {
        ResourceManager.getSound(if (it) Constants.gameDirPath.child("game-over-success.wav") else Constants.gameDirPath.child("game-over-fail.wav"))?.play(PCGame.soundVolume)
        PCGame.sceneManager.loadScene(EndLevelScene(this))
    }

    @JsonIgnore
    var scorePoints = 0

    private val timer = Timer(1f)

    var initialZoom = 1f

    @JsonIgnore
    var zoom = 1f

    var gravitySpeed = Constants.defaultGravitySpeed

    var backgroundColor = floatArrayOf(0f, 0f, 0f)

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

        if (!levelPrefabs.first.exists())
            levelPrefabs.first.mkdirs()
        else {
            Utility.getFilesRecursivly(levelPrefabs.first, Constants.prefabExtension).forEach {
                levelPrefabs.second.add(SerializationFactory.deserializeFromFile(it))
            }
        }
    }

    fun resourcesTextures() = levelTextures.second.toList()
    fun resourcesAtlas() = levelAtlas.second.toList()
    fun resourcesSounds() = levelSounds.second.toList()
    fun resourcesPrefabs() = levelPrefabs.second.toList()

    fun addResources(vararg resources: FileHandle) {
        resources.forEach {
            when {
                Constants.levelTextureExtension.contains(it.extension()) -> {
                    it.copyTo(levelTextures.first)
                    levelTextures.second.add(levelTextures.first.child(it.name()))
                }
                Constants.levelAtlasExtension.contains(it.extension()) -> {
                    it.copyTo(levelAtlas.first)
                    it.parent().child(it.nameWithoutExtension() + ".png").copyTo(levelAtlas.first)
                    levelAtlas.second.add(levelAtlas.first.child(it.name()))
                }
                Constants.levelSoundExtension.contains(it.extension()) -> {
                    it.copyTo(levelSounds.first)
                    levelSounds.second.add(levelSounds.first.child(it.name()))
                }
            }
        }
    }

    fun addPrefab(prefab: Prefab) {
        levelPrefabs.second.add(prefab)
        SerializationFactory.serializeToFile(prefab, levelPrefabs.first.child("${prefab.name}.${Constants.prefabExtension}"))
    }

    fun removePrefab(prefab: Prefab) {
        levelPrefabs.second.remove(prefab)
        levelPrefabs.first.child("${prefab.name}.${Constants.prefabExtension}").delete()
    }

    fun updateCamera(camera: OrthographicCamera, lerp: Boolean) {
        camera.zoom = MathUtils.lerp(camera.zoom, zoom, 0.1f)

        val viewportWidth = Constants.viewportRatioWidth * camera.zoom
        val viewportHeight = Constants.viewportRatioHeight * camera.zoom

        activeRect.size = Size((viewportWidth * 1.5f).roundToInt(), (viewportHeight * 1.5f).roundToInt())

        if (followGameObject != null) {
            val go = followGameObject!!

            val posX = MathUtils.clamp(go.position().x + go.size().width / 2f, viewportWidth / 2f, matrixRect.width - viewportWidth / 2f)
            val posY = MathUtils.clamp(go.position().y + go.size().height / 2f, viewportHeight / 2f, matrixRect.height - viewportHeight / 2f)

            val lerpX = MathUtils.lerp(camera.position.x, posX, 0.1f).clamp(viewportWidth / 2f, matrixRect.width - viewportWidth / 2f)
            val lerpY = MathUtils.lerp(camera.position.y, posY, 0.1f).clamp(viewportHeight / 2f, matrixRect.height - viewportHeight / 2f)

            camera.position.set(
                    if (lerp) lerpX else posX,
                    if (lerp) lerpY else posY, 0f)
        }
    }

    @JsonIgnore
    fun getTimer() = timer.timer

    fun deleteFiles() {
        if (!levelPath.toLocalFile().exists()) {
            levelPath.toLocalFile().parent().deleteDirectory()
        }
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        zoom = initialZoom
    }

    override fun update() {
        super.update()

        if (allowUpdatingGO)
            timer.update()
    }

    companion object {
        fun newLevel(levelName: String): Level {
            val levelDir = (Constants.levelDirPath.child(levelName))
            if (levelDir.exists()) {
                Log.warn { "Un niveau portant le même nom existe déjà !" }
                levelDir.deleteDirectory()
            }
            levelDir.mkdirs()
            val level = Level(levelDir.child(Constants.levelDataFile).path(), Constants.gameVersion, PCGame.parallaxBackgrounds().elementAtOrNull(0)
                    ?: PCGame.standardBackgrounds().elementAtOrNull(0), null)

            val ground = PrefabFactory.PhysicsSprite.prefab.create(Point(0f, 0f), level)
            ground.getCurrentState().getComponent<AtlasComponent>()?.data?.elementAtOrNull(0)?.apply {
                repeatRegion = true
                repeatRegionSize = Size(50)
            }
            ground.box.width = 300

            val player = PrefabFactory.Player_Kenney.prefab.create(Point(100f, 50f), level)

            level.followGameObject = player

            level.favoris.add(player)

            level.loadResources()

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