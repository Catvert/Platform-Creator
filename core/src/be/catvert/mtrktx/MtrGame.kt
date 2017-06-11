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
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxGame
import ktx.assets.loadOnDemand

class MtrGame : KtxGame<BaseScene>() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var mainFont: BitmapFont
        private set
    lateinit var defaultProjection: Matrix4

    val assetManager = AssetManager()

    override fun create() {
        VisUI.load(Gdx.files.internal("ui/tinted/x1/tinted.json"))
        batch = SpriteBatch()
        defaultProjection = batch.projectionMatrix

        mainFont = BitmapFont(Gdx.files.internal("fonts/mainFont.fnt"))

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

    fun getLogo() : Entity {
        val (logoWidth, logoHeight) = Pair(Gdx.graphics.width.toFloat() / 3 * 2, Gdx.graphics.height.toFloat() / 4)
        return EntityFactory.createSprite(Rectangle(Gdx.graphics.width / 2f - logoWidth / 2f , Gdx.graphics.height - logoHeight, logoWidth, logoHeight), getTexture(Gdx.files.internal("game/logo.png")))
    }

    fun getMainBackground() : Entity {
       return EntityFactory.createSprite(Rectangle(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()), getTexture(Gdx.files.internal("game/mainmenu.png")))
    }

    fun getTexture(path: FileHandle): Pair<FileHandle, Texture> {
        try {
            if(!path.exists())
                throw Exception("La chemin n'existe pas")
            return Pair(path, assetManager.loadOnDemand<Texture>(path.path()).asset)
        } catch(e: Exception) {
            println("Erreur lors du chargement de la texture : $path : $e")
        }
        return Pair(FileHandle(""), Texture(1, 1, Pixmap.Format.Alpha))
    }

    override fun dispose() {
        batch.dispose()
        assetManager.dispose()
        mainFont.dispose()
        VisUI.dispose()
    }
}
