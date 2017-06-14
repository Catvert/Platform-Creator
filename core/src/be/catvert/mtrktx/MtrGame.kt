package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.scenes.BaseScene
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxGame
import ktx.assets.loadOnDemand
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import javax.xml.soap.Text


class MtrGame : KtxGame<BaseScene>() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var stageBatch: SpriteBatch
        private set
    lateinit var mainFont: BitmapFont
        private set
    lateinit var defaultProjection: Matrix4
        private set

    private val textureAtlasList = mutableListOf<Pair<TextureAtlas, FileHandle>>()
    fun getTextureAtlasList(): List<Pair<TextureAtlas, FileHandle>> = textureAtlasList

    val assetManager = AssetManager()

    override fun create() {
        VisUI.load(Gdx.files.internal("ui/tinted/x1/tinted.json"))
        batch = SpriteBatch()
        defaultProjection = batch.projectionMatrix.cpy()

        stageBatch = SpriteBatch()

        mainFont = BitmapFont(Gdx.files.internal("fonts/mainFont.fnt"))

        loadGameResources()

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
        return EntityFactory.createSprite(Rectangle(Gdx.graphics.width / 2f - logoWidth / 2f , Gdx.graphics.height - logoHeight, logoWidth, logoHeight), getGameTexture(Gdx.files.internal("game/logo.png")))
    }

    fun getMainBackground(): Entity {
        return EntityFactory.createSprite(Rectangle(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()), getGameTexture(Gdx.files.internal("game/mainmenu.png")))
    }

    private fun loadGameResources() {
        Files.walkFileTree(Paths.get(Gdx.files.internal("spritesheets").path()), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileVisitResult = super.visitFile(file, attrs)

                if(file.toFile().extension == "atlas") {
                    textureAtlasList += Pair(assetManager.loadOnDemand<TextureAtlas>(file.toString()).asset, Gdx.files.internal(file.toString()))
                }

                return fileVisitResult
            }
        })
    }

    fun getSpriteSheetTexture(spriteSheet: String, textureName: String): TextureInfo {
        try {
            if(assetManager.isLoaded(spriteSheet)) {
                val texture = assetManager.get(spriteSheet, TextureAtlas::class.java).findRegion(textureName)
                return TextureInfo(texture, spriteSheet, textureName)
            }
        } catch(e: Exception) {
            println("Erreur lors du chargement de la texture : $e")
        }

        return TextureInfo(TextureAtlas.AtlasRegion(Texture(1, 1, Pixmap.Format.Alpha), 0, 0, 1, 1))
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

    override fun dispose() {
        batch.dispose()
        assetManager.dispose()
        mainFont.dispose()
        VisUI.dispose()
    }
}
