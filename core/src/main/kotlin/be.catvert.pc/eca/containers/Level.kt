package be.catvert.pc.eca.containers

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.Group
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.managers.ScriptsManager
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import javax.script.ScriptException
import kotlin.math.roundToInt

class LevelStats(val levelPath: FileWrapper, val timer: Int, val numberOfTries: Int) {
    override fun toString() = levelPath.get().parent().name()
}

/**
 * Permet de regrouper les entités au sein d'un niveau.
 * @see EntityContainer
 * @see EntityMatrixContainer
 */
class Level(val levelPath: FileWrapper, val gameVersion: Float, var background: Background?, var music: ResourceWrapper<Music>?) : EntityMatrixContainer() {
    private val levelTextures = levelPath.get().parent().child("textures") to mutableListOf<ResourceWrapper<Texture>>()
    private val levelPacks = levelPath.get().parent().child("packs") to mutableListOf<ResourceWrapper<TextureAtlas>>()
    private val levelSounds = levelPath.get().parent().child("sounds") to mutableListOf<ResourceWrapper<Sound>>()
    private val levelPrefabs = levelPath.get().parent().child("prefabs") to mutableListOf<Prefab>()
    private val levelGroups = levelPath.get().parent().child("groups") to mutableListOf<Group>()
    private val levelScripts = levelPath.get().parent().child("scripts") to mutableListOf<Script>()

    val tags = arrayListOf(*Tags.values().map { it.tag }.toTypedArray())

    val favoris = arrayListOf<Int>()

    @JsonIgnore
    var applyGravity = true

    @JsonIgnore
    var exit: (success: Boolean) -> Unit = {}

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
            levelTextures.second.addAll(Utility.getFilesRecursivly(levelTextures.first, *Constants.levelTextureExtension).map { resourceWrapperOf<Texture>(it.toFileWrapper()) })

        if (!levelPacks.first.exists())
            levelPacks.first.mkdirs()
        else
            levelPacks.second.addAll(Utility.getFilesRecursivly(levelPacks.first, *Constants.levelPackExtension).map { resourceWrapperOf<TextureAtlas>(it.toFileWrapper()) })

        if (!levelSounds.first.exists())
            levelSounds.first.mkdirs()
        else
            levelSounds.second.addAll(Utility.getFilesRecursivly(levelSounds.first, *Constants.levelSoundExtension).map { resourceWrapperOf<Sound>(it.toFileWrapper()) })

        if (!levelPrefabs.first.exists())
            levelPrefabs.first.mkdirs()
        else {
            Utility.getFilesRecursivly(levelPrefabs.first, Constants.prefabExtension).forEach {
                levelPrefabs.second.add(SerializationFactory.deserializeFromFile(it))
            }
        }

        if (!levelGroups.first.exists())
            levelGroups.first.mkdirs()
        else {
            Utility.getFilesRecursivly(levelGroups.first, Constants.groupExtension).forEach {
                levelGroups.second.add(SerializationFactory.deserializeFromFile(it))
            }
        }

        if (!levelScripts.first.exists())
            levelScripts.first.mkdirs()
        else {
            Utility.getFilesRecursivly(levelScripts.first, *Constants.levelScriptExtension).forEach {
                try {
                    levelScripts.second.add(Script(it, ScriptsManager.compile(it)))
                } catch (e: ScriptException) {
                    Log.error(e) { "Impossible de charger le script : $it" }
                }
            }
        }
    }

    fun resourcesTextures(): List<ResourceWrapper<Texture>> = levelTextures.second
    fun resourcesPacks(): List<ResourceWrapper<TextureAtlas>> = levelPacks.second
    fun resourcesSounds(): List<ResourceWrapper<Sound>> = levelSounds.second
    fun resourcesPrefabs(): List<Prefab> = levelPrefabs.second
    fun resourcesGroups(): List<Group> = levelGroups.second
    fun resourcesScripts(): List<Script> = levelScripts.second

    fun addResources(vararg resources: FileHandle) {
        resources.forEach {
            when {
                Constants.levelTextureExtension.contains(it.extension()) -> {
                    it.copyTo(levelTextures.first)
                    levelTextures.second.add(resourceWrapperOf(levelTextures.first.child(it.name()).toFileWrapper()))
                }
                Constants.levelPackExtension.contains(it.extension()) -> {
                    it.copyTo(levelPacks.first)
                    it.parent().child(it.nameWithoutExtension() + ".png").copyTo(levelPacks.first)
                    levelPacks.second.add(resourceWrapperOf(levelPacks.first.child(it.name()).toFileWrapper()))
                }
                Constants.levelSoundExtension.contains(it.extension()) -> {
                    it.copyTo(levelSounds.first)
                    levelSounds.second.add(resourceWrapperOf(levelSounds.first.child(it.name()).toFileWrapper()))
                }
                Constants.levelScriptExtension.contains(it.extension()) -> {
                    it.copyTo(levelScripts.first)
                    levelScripts.second.add(Script(it, ScriptsManager.compile(it)))
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

        val followEntity = followEntity.entity(this)
        if (followEntity != null) {
            val posX = MathUtils.clamp(followEntity.box.center().x, viewportWidth / 2f, matrixRect.width - viewportWidth / 2f)
            val posY = MathUtils.clamp(followEntity.box.center().y, viewportHeight / 2f, matrixRect.height - viewportHeight / 2f)

            val lerpX = MathUtils.lerp(camera.position.x, posX, 0.1f).clamp(viewportWidth / 2f, matrixRect.width - viewportWidth / 2f)
            val lerpY = MathUtils.lerp(camera.position.y, posY, 0.1f).clamp(viewportHeight / 2f, matrixRect.height - viewportHeight / 2f)

            camera.position.set(
                    if (lerp) lerpX else posX,
                    if (lerp) lerpY else posY, 0f)
        }
    }

    @JsonIgnore
    fun getTimer(): Int = timer.timer

    fun deleteFiles() {
        if (!levelPath.get().exists()) {
            levelPath.get().parent().deleteDirectory()
        }
    }

    fun customBackgroundPath() = levelPath.get().parent().child(Constants.levelCustomBackgroundFile)

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        zoom = initialZoom
    }

    override fun update() {
        super.update()

        if (allowUpdating)
            timer.update()
    }

    override fun onRemoveEntity(entity: Entity) {
        super.onRemoveEntity(entity)

        favoris.remove(entity.id())
    }

    override fun toString() = levelPath.toString()

    companion object {
        fun newLevel(levelName: String): Level {
            val levelDir = (Constants.levelDirPath.child(levelName))
            if (levelDir.exists()) {
                Log.warn { "Un niveau portant le même nom existe déjà !" }
                levelDir.deleteDirectory()
            }
            levelDir.mkdirs()
            val level = Level(levelDir.child(Constants.levelDataFile).toFileWrapper(), Constants.gameVersion, PCGame.getParallaxBackgrounds().elementAtOrNull(0)
                    ?: PCGame.getStandardBackgrounds().elementAtOrNull(0), null)

            val ground = PrefabFactory.PhysicsSprite.prefab.create(Point(0f, 0f), level)
            ground.getCurrentState().getComponent<TextureComponent>()?.groups?.elementAtOrNull(0)?.apply {
                repeatRegion = true
                repeatRegionSize = Size(50)
            }
            ground.box.width = 300

            val player = PrefabFactory.Player_SMC.prefab.create(Point(100f, 50f), level)

            level.followEntity.ID = player.id()
            level.favoris.add(player.id())

            return level
        }

        fun loadFromFile(levelDir: FileHandle): Level? {
            Log.info { "Chargement du niveau : $levelDir" }
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