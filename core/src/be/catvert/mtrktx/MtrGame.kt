package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.components.render.RenderComponent
import be.catvert.mtrktx.scenes.BaseScene
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxGame
import ktx.assets.loadOnDemand
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.collections.toGdxArray


class MtrGame : KtxGame<BaseScene>() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var stageBatch: SpriteBatch
        private set
    lateinit var mainFont: BitmapFont
        private set
    lateinit var defaultProjection: Matrix4
        private set

    private val textureAtlasList = mutableListOf<Pair<TextureAtlas, String>>()
    fun getTextureAtlasList(): List<Pair<TextureAtlas, String>> = textureAtlasList

    private val animationsList = mutableListOf<Pair<Animation<TextureAtlas.AtlasRegion>, String>>()

    val assetManager = AssetManager()

    override fun create() {
        VisUI.load(Gdx.files.internal("ui/tinted/x1/tinted.json"))
        batch = SpriteBatch()
        defaultProjection = batch.projectionMatrix.cpy()

        stageBatch = SpriteBatch()

        mainFont = BitmapFont(Gdx.files.internal("fonts/mainFont.fnt"))

        loadGameResources()
        loadAnimations()

        addScreen(MainMenuScene(this))
        setScreen<MainMenuScene>()
    }

    inline fun <reified T : BaseScene> setScene(scene: T) {
        shownScreen.dispose()
        removeSceneSafely<T>()
        addScreen(scene)
        setScreen<T>()
    }

    inline fun <reified T : BaseScene> removeSceneSafely() {
        if(containsScreen<T>())
            removeScreen<T>()
    }

    fun getLogo(): Entity {
        val (logoWidth, logoHeight) = Pair(Gdx.graphics.width.toFloat() / 3 * 2, Gdx.graphics.height.toFloat() / 4)
        return EntityFactory.createSprite(Rectangle(Gdx.graphics.width / 2f - logoWidth / 2f , Gdx.graphics.height - logoHeight, logoWidth, logoHeight), RenderComponent(listOf(getGameTexture(Gdx.files.internal("game/logo.png")))))
    }

    fun getMainBackground(): Entity {
        return EntityFactory.createSprite(Rectangle(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()), RenderComponent(listOf(getGameTexture(Gdx.files.internal("game/mainmenu.png")))))
    }

    private fun loadAnimations() {
        val loadedAnimName = mutableListOf<String>()
        textureAtlasList.forEach { atlas ->
            atlas.first.regions.forEach {
                /* les animations de Kenney finissent par une lettre puis par exemple 1 donc -> alienGreen_walk1 puis alienGreen_walk2
                mais des autres textures normale tel que foliagePack_001 existe donc on doit vérifier si le nombre avant 1 fini bien par une lettre
                */
                if(!loadedAnimName.contains(it.name) && it.name.endsWith("_0")) {
                    val name = it.name.removeSuffix("_0")

                    var count = 1
                    do {
                        if(atlas.first.findRegion(name + "_" + count) == null) {
                            break
                        }
                        ++count
                    } while(true)

                    val frameList = mutableListOf<TextureAtlas.AtlasRegion>()

                    for (i in 0..count - 1) {
                        val nameNextFrame = name + "_" + i
                        frameList.add(atlas.first.findRegion(nameNextFrame))
                    }

                    animationsList += Pair(Animation<TextureAtlas.AtlasRegion>(0.33f, frameList.toGdxArray()), it.name.removeSuffix("_0"))
                    loadedAnimName += it.name
                }
            }
        }
    }

    private fun loadGameResources() {
        val visitor = object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileVisitResult = super.visitFile(file, attrs)

                if(file.toFile().extension == "atlas") {
                    textureAtlasList += Pair(assetManager.loadOnDemand<TextureAtlas>(file.toString()).asset, file.toFile().nameWithoutExtension)
                }

                return fileVisitResult
            }
        }

        Files.walkFileTree(Paths.get(Gdx.files.internal("spritesheets").path()), visitor)
        Files.walkFileTree(Paths.get(Gdx.files.internal("game").path()), visitor)
    }

    fun getSpriteSheetTexture(spriteSheet: String, textureName: String): TextureInfo {
        try {
            val texture = textureAtlasList.first { it.second.equals(spriteSheet, true) }.first.findRegion(textureName)
            return TextureInfo(texture, spriteSheet, textureName)
        } catch(e: Exception) {
            throw Exception("Erreur lors du chargement de la texture, le spriteSheet ayant le nom : $spriteSheet n'existe pas ! $e")
        }
    }

    fun getGameTexture(path: FileHandle): TextureInfo {
        try {
            if(!path.exists())
                throw Exception("La chemin n'existe pas")
            if(assetManager.isLoaded(path.path())) {
                val texture = assetManager.get(path.path(), Texture::class.java)
                return TextureInfo(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height), texturePath = path.path())
            }
            else {
                val texture = assetManager.loadOnDemand<Texture>(path.path()).asset
                return TextureInfo(TextureAtlas.AtlasRegion(texture, 0, 0, texture.width, texture.height), texturePath = path.path())
            }
        } catch(e: Exception) {
            println("Erreur lors du chargement de la texture : $path : $e")
        }

        return TextureInfo(TextureAtlas.AtlasRegion(Texture(1, 1, Pixmap.Format.Alpha), 0, 0, 1, 1))
    }

    fun getAnimation(animationName: String): Animation<TextureAtlas.AtlasRegion> {
        try {
            return animationsList.first { (it.second == animationName) }.first
        } catch(e: NoSuchElementException) {
            throw Exception("Impossible de trouver l'animation portant le nom : $animationName")
        }
    }

    override fun dispose() {
        batch.dispose()
        assetManager.dispose()
        mainFont.dispose()
        VisUI.dispose()
    }
}
