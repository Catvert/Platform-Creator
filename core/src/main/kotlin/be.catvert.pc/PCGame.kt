package be.catvert.pc

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.i18n.Locales
import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.scenes.Scene
import be.catvert.pc.scenes.SceneManager
import be.catvert.pc.scenes.SceneTweenAccessor
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.I18NBundle
import imgui.DEBUG
import imgui.Font
import imgui.FontConfig
import imgui.ImGui
import imgui.impl.LwjglGL3
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import ktx.app.KtxApplicationAdapter
import ktx.assets.toAbsoluteFile
import ktx.scene2d.Scene2DSkin
import uno.glfw.GlfwWindow
import java.util.*
import kotlin.reflect.KClass
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle
import com.kotcrab.vis.ui.VisUI
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const
import ktx.async.enableKtxCoroutines
import ktx.async.ktxAsync
import ktx.scene2d.Scene2dDsl
import ktx.style.color
import ktx.style.set
import kotlin.math.roundToInt


/** [com.badlogic.gdx.ApplicationListener, implementation shared by all platforms.  */
class PCGame(private val initialConfig: GameConfig) : KtxApplicationAdapter {
    private fun initializeUI() {
        VisUI.load(Constants.uiDirPath.child("tinted/x1/tinted.json"))

        LwjglGL3.init(GlfwWindow((Gdx.graphics as Lwjgl3Graphics).window.windowHandle), false)
    }

    override fun create() {
        super.create()
        // Permet de supprimer les logs d'imgui
        DEBUG = false

        enableKtxCoroutines(1)

        PCGame.soundVolume = initialConfig.soundVolume
        PCGame.darkUI = initialConfig.darkUI
        PCGame.locale = initialConfig.locale

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        ResourceManager.init()

        Utility.getFilesRecursivly(Locales.menusPath.parent(), "properties").forEach {
                    if(it.name().startsWith("bundle_"))
                        availableLocales.add(Locale.forLanguageTag(it.nameWithoutExtension().substringAfter('_')))
                }

        GameKeys.loadKeysConfig()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Constants.mainFontPath)

        Tween.registerAccessor(GameObject::class.java, GameObjectTweenAccessor())
        Tween.registerAccessor(Scene::class.java, SceneTweenAccessor())

        Utility.getFilesRecursivly(Constants.backgroundsDirPath.child("standard"), *Constants.levelTextureExtension).forEach {
                    standardBackgrounds.add(StandardBackground(it.toFileWrapper()))
                }

        Utility.getFilesRecursivly(Constants.backgroundsDirPath.child("parallax"), "data").forEach {
                    parallaxBackgrounds.add(ParallaxBackground(it.toFileWrapper()))
                }

        mainBackground = StandardBackground(Constants.gameBackgroundMenuPath.toFileWrapper())

        gameAtlas = let {
            val atlas = mutableMapOf<FileHandle, List<FileHandle>>()

            Constants.packsDirPath.list().forEach {
                        if (it.isDirectory) {
                            atlas[it] = Utility.getFilesRecursivly(it, *Constants.levelAtlasExtension)
                        }
                    }

            atlas
        }
        gameTextures = Utility.getFilesRecursivly(Constants.texturesDirPath, *Constants.levelTextureExtension)
        gameSounds = Utility.getFilesRecursivly(Constants.soundsDirPath, *Constants.levelSoundExtension)

        initializeUI()

        sceneManager = SceneManager(MainMenuScene())
    }

    override fun render() {
        super.render()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        sceneManager.update()

        tweenManager.update(Gdx.graphics.deltaTime)

        sceneManager.render(mainBatch)

        sceneManager.currentScene().calcIsUIHover()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        sceneManager.resize(Size(width, height))
        defaultProjection.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        super.dispose()

        GameConfig.saveGameConfig()
        GameKeys.saveKeysConfig()

        mainBatch.dispose()
        hudBatch.dispose()

        sceneManager.dispose()

        mainFont.dispose()

        VisUI.dispose(true)

        ResourceManager.dispose()

        Log.dispose()

        LwjglGL3.shutdown()
    }

    companion object {
        private val standardBackgrounds = mutableListOf<StandardBackground>()
        private val parallaxBackgrounds = mutableListOf<ParallaxBackground>()

        lateinit var mainBatch: SpriteBatch
            private set

        lateinit var hudBatch: SpriteBatch
            private set

        lateinit var sceneManager: SceneManager
            private set

        var locale = Locale.FRENCH
            set(value) {
                field = value
                Locales.load(value)
            }
        /**
         * Le font principal du jeu
         */
        lateinit var mainFont: BitmapFont
            private set

        lateinit var defaultProjection: Matrix4
            private set

        lateinit var gameAtlas: Map<FileHandle, List<FileHandle>>
            private set
        lateinit var gameTextures: List<FileHandle>
            private set
        lateinit var gameSounds: List<FileHandle>
            private set
        lateinit var mainBackground: Background
            private set

        val actionsClasses = let {
            val list = mutableListOf<KClass<out Action>>()
            FastClasspathScanner(Action::class.java.`package`.name).matchClassesImplementing(Action::class.java, { list.add(it.kotlin); }).scan()

            list.removeAll { it.isAbstract || !ReflectionUtility.hasNoArgConstructor(it) }

            list.toList()
        }

        val componentsClasses = let {
            val componentsList = mutableListOf<KClass<out Component>>()
            FastClasspathScanner(Component::class.java.`package`.name).matchSubclassesOf(Component::class.java, { componentsList.add(it.kotlin) }).scan()

            componentsList.removeAll { it.isAbstract || !ReflectionUtility.hasNoArgConstructor(it) }

            componentsList.toList()
        }

        var soundVolume = 1f
            set(value) {
                if (value in 0.0..1.0)
                    field = value
            }

        var darkUI = false
            set(value) {
                field = value
                if (value)
                    ImGui.styleColorsDark()
                else
                    ImGui.styleColorsLight()
            }

        val availableLocales = mutableListOf<Locale>(Locale.FRENCH)

        val tweenManager = TweenManager()

        fun standardBackgrounds() = standardBackgrounds.toList()
        fun parallaxBackgrounds() = parallaxBackgrounds.toList()

        /**
         * Permet de retourner le logo du jeu
         */
        fun generateLogo(container: GameObjectContainer): GameObject {
            return container.createGameObject("logo", "logo", getLogoRect(), {
                this += AtlasComponent(0, AtlasComponent.AtlasData("logo", Constants.gameLogoPath.toFileWrapper()))
            })
        }

        /**
         * Permet de retourner la taille du logo au cas où la taille de l'écran changerait.
         */
        private fun getLogoSize() = Size(600, 125)

        fun getLogoRect(): Rect {
            val size = getLogoSize()
            return Rect(Point(Constants.viewportRatioWidth.roundToInt() / 2 - size.width / 2, Constants.viewportRatioHeight.roundToInt() - size.height * 2), size)
        }
    }
}