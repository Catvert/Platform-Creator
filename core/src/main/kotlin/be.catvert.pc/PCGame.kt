package be.catvert.pc

import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.scenes.Scene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Size
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.JsonWriter
import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxApplicationAdapter
import ktx.async.enableKtxCoroutines
import java.io.FileWriter
import java.io.IOException

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class PCGame(private val setFullScreenStartup: Boolean, private val initialVSync: Boolean, private val initialSoundVolume: Float) : KtxApplicationAdapter {
    companion object {
        lateinit var mainBatch: SpriteBatch
            private set

        lateinit var hudBatch: SpriteBatch
            private set

        /**
         * Le font principal du jeu
         */
        lateinit var mainFont: BitmapFont
            private set

        lateinit var defaultProjection: Matrix4
            private set

        var vsync = false
            set(value) {
                field = value
                Gdx.graphics.setVSync(vsync)
            }

        var soundVolume = 1f

        val assetManager = AssetManager()

        private lateinit var currentScene: Scene

        /**
         * Permet de retourner le logo du jeu
         */
        fun generateLogo(container: GameObjectContainer): GameObject {
            return container.createGameObject(getLogoRect()) {
                this += TextureComponent(Gdx.files.local(Constants.gameLogoPath))
            }
        }

        /**
         * Permet de retourner la taille du logo au cas où la taille de l'écran changerait.
         */
        private fun getLogoSize(): Size {
            return Size(Gdx.graphics.width / 3 * 2, Gdx.graphics.height / 4)
        }

        fun getLogoRect(): Rectangle {
            val size = getLogoSize()
            return Rectangle(Gdx.graphics.width / 2f - size.width / 2f, Gdx.graphics.height.toFloat() - size.height.toFloat(), size.width.toFloat(), size.height.toFloat())
        }

        fun setScene(newScene: Scene, disposeCurrentScene: Boolean = true) {
            if(disposeCurrentScene)
                currentScene.dispose()
            currentScene = newScene
        }

        /**
         * Permet de sauvegarder la configuration du jeu
         */
        fun saveGameConfig(): Boolean {
            try {
                val writer = JsonWriter(FileWriter(Gdx.files.local(Constants.configPath).path(), false))
                writer.setOutputType(JsonWriter.OutputType.json)

                writer.`object`()

                writer.name("width").value(Gdx.graphics.width)
                writer.name("height").value(Gdx.graphics.height)
                writer.name("vsync").value(PCGame.vsync)
                writer.name("fullscreen").value(Gdx.graphics.isFullscreen)
                writer.name("soundvolume").value(PCGame.soundVolume)

                writer.pop()

                writer.flush()
                writer.close()

                return true
            } catch (e: IOException) {
                return false
            }
        }

        /**
         * Permet de sauvegarder la configuration des touches
         */
        fun saveKeysConfig(): Boolean {
            try {
                val writer = JsonWriter(FileWriter(Gdx.files.local(Constants.keysConfigPath).path(), false))
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
    }

    override fun create() {
        super.create()
        enableKtxCoroutines(asynchronousExecutorConcurrencyLevel = 1)

        if(setFullScreenStartup) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        VisUI.load(Gdx.files.internal(Constants.UISkinPath))

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.vsync = initialVSync
        PCGame.soundVolume = initialSoundVolume
        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Gdx.files.local(Constants.mainFontPath))


        currentScene = MainMenuScene()
    }

    override fun render() {
        super.render()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        currentScene.update()
        currentScene.render(mainBatch)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        currentScene.resize(Size(width, height))
        defaultProjection.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        super.dispose()

        mainBatch.dispose()
        hudBatch.dispose()

        currentScene.dispose()

        VisUI.dispose()

        Log.dispose()
    }
}