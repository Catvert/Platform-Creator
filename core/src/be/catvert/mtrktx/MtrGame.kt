package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.systems.physics.GridCell
import be.catvert.mtrktx.scenes.BaseScene
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import ktx.app.KtxGame
import ktx.assets.loadOnDemand
import ktx.scene2d.Scene2DSkin

class MtrGame : KtxGame<BaseScene>() {
    lateinit var batch: SpriteBatch
        private set

    val assetManager = AssetManager()

    val engine = Engine()

    override fun create() {
        GridCell(20, 20)
        batch = SpriteBatch()
        Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("ui/sgx/skin/sgx-ui.json"))

        addScreen(MainMenuScene(this))
        setScreen<MainMenuScene>()

    }

    inline fun <reified T : BaseScene> setScene(scene: T) {
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
        return EntityFactory.createSprite(Rectangle(Gdx.graphics.width / 2f - logoWidth / 2f , Gdx.graphics.height - logoHeight, logoWidth, logoHeight), assetManager.loadOnDemand<Texture>("game/logo.png").asset)
    }

    fun getMainBackground() : Entity {
       return EntityFactory.createSprite(Rectangle(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()), assetManager.loadOnDemand<Texture>("game/mainmenu.png").asset)
    }

    override fun dispose() {
        batch.dispose()
    }
}
