package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.RenderComponent
import be.catvert.plateformcreator.scenes.BaseScene
import be.catvert.plateformcreator.scenes.MainMenuScene
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.JsonWriter
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxGame
import ktx.app.clearScreen
import ktx.app.use
import ktx.assets.loadOnDemand
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import java.io.FileWriter
import java.io.IOException

/**
 * Classe du jeu
 */
class MtrGame(vsync: Boolean) : KtxGame<BaseScene>() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var stageBatch: SpriteBatch
        private set
    lateinit var mainFont: BitmapFont
        private set
    lateinit var defaultProjection: Matrix4
        private set

    var vsync = vsync
        set(value) {
            field = value
            Gdx.graphics.setVSync(vsync)
        }

    val assetManager = AssetManager()
    val entityFactory = EntityFactory(this)

    var clearScreenColor = Triple(186f / 255f, 212f / 255f, 1f)

    val engine = Engine()

    lateinit var background: RenderComponent

    private val textureAtlasList = mutableListOf<Pair<TextureAtlas, String>>()
    fun getTextureAtlasList(): List<Pair<TextureAtlas, String>> = textureAtlasList

    private val animationsList = mutableListOf<Pair<Animation<TextureAtlas.AtlasRegion>, String>>()

    override fun create() {
        VisUI.load(Gdx.files.internal("ui/tinted/x1/tinted.json"))
        batch = SpriteBatch()
        defaultProjection = batch.projectionMatrix.cpy()

        stageBatch = SpriteBatch()

        mainFont = BitmapFont(Gdx.files.internal("fonts/mainFont.fnt"))

        loadGameResources()
        loadAnimations()

        background = getMainBackground()

        setScene(MainMenuScene(this))
    }

    override fun render(delta: Float) {
        clearScreen(clearScreenColor.first, clearScreenColor.second, clearScreenColor.third)

        batch.projectionMatrix = defaultProjection
        batch.use {
            batch.draw(background.getActualAtlasRegion(), 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }
        batch.projectionMatrix = shownScreen.camera.combined

        engine.removeAllEntities()
        shownScreen.entities.forEach {
            if (!engine.entities.contains(it))
                engine.addEntity(it)
        }

        engine.update(delta)

        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        shownScreen.stage.viewport.update(width, height)
        shownScreen.viewport.update(width, height)
        shownScreen.camera.setToOrtho(false, width.toFloat(), height.toFloat())
        defaultProjection.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        engine.removeAllEntities()
        assetManager.dispose()
        mainFont.dispose()
        VisUI.dispose()
    }

    /**
     * Permet de définir la scène actuelle
     * Dispose la scène précédament chargée
     * Supprime les entités chargées dans la scène précédante
     */
    inline fun <reified T : BaseScene> setScene(scene: T) {
        if (shownScreen is BaseScene)
            shownScreen.dispose()
        removeSceneSafely<T>()

        engine.removeAllEntities()
        (engine.systems.size() - 1 downTo 0).asSequence().forEach {
            engine.removeSystem(engine.systems[it])
        }

        for (system in scene.addedSystems)
            engine.addSystem(system)

        scene.entities.forEach {
            if (!engine.entities.contains(it))
                engine.addEntity(it)
        }

        addScreen(scene)
        setScreen<T>()
    }

    /**
     * Permet de charger les animations à partir des atlas chargés précédament
     */
    private fun loadAnimations() {
        val loadedAnimName = mutableListOf<String>()
        textureAtlasList.forEach { (first) ->
            first.regions.forEach {
                /* les animations de Kenney finissent par une lettre puis par exemple 1 donc -> alienGreen_walk1 puis alienGreen_walk2
                mais des autres textures normale tel que foliagePack_001 existe donc on doit vérifier si le nombre avant 1 fini bien par une lettre
                */
                if (!loadedAnimName.contains(it.name) && it.name.endsWith("_0")) {
                    val name = it.name.removeSuffix("_0")

                    var count = 1
                    do {
                        if (first.findRegion(name + "_" + count) == null) {
                            break
                        }
                        ++count
                    } while (true)

                    val frameList = mutableListOf<TextureAtlas.AtlasRegion>()

                    val initialRegion = first.findRegion(name + "_0")

                    for (i in 0..count - 1) {
                        val nameNextFrame = name + "_" + i
                        val region = first.findRegion(nameNextFrame)
                        region.regionWidth = initialRegion.regionWidth
                        region.regionHeight = initialRegion.regionHeight
                        frameList.add(region)
                    }

                    animationsList += Pair(Animation<TextureAtlas.AtlasRegion>(0.33f, frameList.toGdxArray()), it.name.removeSuffix("_0"))
                    loadedAnimName += it.name
                }
                it.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            }
        }
    }

    /**
     * Permet de charger les resources du jeu
     */
    private fun loadGameResources() {
        Utility.getFilesRecursivly(Gdx.files.internal("spritesheets"), "atlas").forEach {
            textureAtlasList += Pair(assetManager.loadOnDemand<TextureAtlas>(it.toString()).asset, it.file().nameWithoutExtension)
        }
    }

    /**
     * Permet de supprimer une scène en vérifiant si la scène était bien chargée.
     */
    inline fun <reified T : BaseScene> removeSceneSafely() {
        if (containsScreen<T>())
            removeScreen<T>()
    }

    /**
     * Permet de retourner le logo du jeu
     */
    fun getLogo(): Entity {
        val (logoWidth, logoHeight) = getLogoSize()
        return entityFactory.createSprite(Rectangle(Gdx.graphics.width / 2f - logoWidth / 2f, Gdx.graphics.height - logoHeight, logoWidth, logoHeight), RenderComponent(listOf(getGameTexture(Gdx.files.internal("game/logo.png")))))
    }

    /**
     * Permet de retourner la taille du logo au cas où la taille de l'écran changerait.
     */
    fun getLogoSize(): Pair<Float, Float> {
        return Pair(Gdx.graphics.width.toFloat() / 3 * 2, Gdx.graphics.height.toFloat() / 4)
    }

    /**
     * Permet de retourner le fond d'écran principal
     */
    fun getMainBackground(): RenderComponent {
        return RenderComponent(listOf(getGameTexture(Gdx.files.internal("game/mainmenu.png"))))
    }

    /**
     * Permet de charger et/ou de retourner une texture du dossier game
     */
    fun getGameTexture(path: FileHandle): TextureInfo {
        try {
            if (!path.exists())
                throw Exception("La chemin n'existe pas")
            if (assetManager.isLoaded(path.path())) {
                val texture = assetManager.get(path.path(), Texture::class.java)
                return TextureInfo(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height), texturePath = path.path())
            } else {
                val texture = assetManager.loadOnDemand<Texture>(path.path()).asset
                return TextureInfo(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height), texturePath = path.path())
            }
        } catch(e: Exception) {
            println("Erreur lors du chargement de la texture : $path : $e")
        }

        return TextureInfo(TextureAtlas.AtlasRegion(Texture(1, 1, Pixmap.Format.Alpha), 0, 0, 1, 1))
    }

    /**
     * Permet de retourner la région ayant le nom spécifié d'un spriteSheet spécifié
     */
    fun getSpriteSheetTexture(spriteSheet: String, textureName: String): TextureInfo {
        try {
            val texture = textureAtlasList.first { it.second.equals(spriteSheet, true) }.first.findRegion(textureName)
            return TextureInfo(texture, spriteSheet, textureName)
        } catch(e: Exception) {
            throw Exception("Erreur lors du chargement de la texture, le spriteSheet ayant le nom : $spriteSheet n'existe pas ! $e")
        }
    }

    /**
     * Permet de retourner une animation portant le nom spécifié
     * frameDuration : La durée de chaque frame
     */
    fun getAnimation(animationName: String, frameDuration: Float): Animation<TextureAtlas.AtlasRegion> {
        try {
            val anim = animationsList.first { (it.second == animationName) }.first
            anim.frameDuration = frameDuration
            return anim
        } catch(e: NoSuchElementException) {
            throw Exception("Impossible de trouver l'animation portant le nom : $animationName")
        }
    }

    /**
     * Permet de créer une animation à partir de plusieurs régions.
     */
    fun createAnimationFromRegions(regions: GdxArray<out TextureAtlas.AtlasRegion>, frameDuration: Float): Animation<TextureAtlas.AtlasRegion> {
        return Animation(frameDuration, regions)
    }

    /**
     * Permet de sauvegarder la configuration des touches
     */
    fun saveKeysConfig(): Boolean  {
        try {
            val writer = JsonWriter(FileWriter(Gdx.files.internal("keysConfig.json").path(), false))
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
            return false
        }
    }

    /**
     * Permet de sauvegarder la configuration du jeu
     */
    fun saveGameConfig(): Boolean {
        try {
            val writer = JsonWriter(FileWriter(Gdx.files.internal("config.json").path(), false))
            writer.setOutputType(JsonWriter.OutputType.json)

            writer.`object`()

            writer.name("width")
            writer.value(Gdx.graphics.width)

            writer.name("height")
            writer.value(Gdx.graphics.height)

            writer.name("vsync")
            writer.value(vsync)

            writer.name("fullscreen")
            writer.value(Gdx.graphics.isFullscreen)

            writer.pop()

            writer.flush()
            writer.close()

            return true
        } catch (e: IOException) {
            return false
        }
    }
}
