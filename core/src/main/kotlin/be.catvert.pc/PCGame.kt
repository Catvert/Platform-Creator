package be.catvert.pc

import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.scenes.Scene
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxApplicationAdapter
import ktx.async.enableKtxCoroutines

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class PCGame : KtxApplicationAdapter {
    companion object {
        lateinit var mainBatch: SpriteBatch
            private set

        lateinit var hudBatch: SpriteBatch
            private set

        val assetManager = AssetManager()

        private lateinit var _currentScene: Scene

        fun setScene(newScene: Scene, disposeCurrentScene: Boolean = true) {
            if(disposeCurrentScene)
                _currentScene.dispose()
            _currentScene = newScene
        }
    }

    override fun create() {
        super.create()
        enableKtxCoroutines(asynchronousExecutorConcurrencyLevel = 1)

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        VisUI.load()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        _currentScene = MainMenuScene()
    }

    override fun render() {
        super.render()

        _currentScene.update()
        _currentScene.render(mainBatch)
    }

    override fun dispose() {
        super.dispose()

        mainBatch.dispose()
        hudBatch.dispose()

        _currentScene.dispose()

        VisUI.dispose()

        Log.dispose()
    }
}