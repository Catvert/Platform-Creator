package be.catvert.pc

import be.catvert.pc.builders.GameObjectBuilder
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.i18n.Locales
import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.scenes.SceneManager
import be.catvert.pc.tweens.TweenSystem
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import glm_.c
import imgui.*
import imgui.impl.LwjglGL3
import ktx.app.KtxApplicationAdapter
import uno.glfw.GlfwWindow
import java.util.*
import kotlin.collections.set


/** [com.badlogic.gdx.ApplicationListener, implementation shared by all platforms.  */
class PCGame(private val initialConfig: GameConfig) : KtxApplicationAdapter {
    private fun initializeUI() {
        LwjglGL3.init(GlfwWindow((Gdx.graphics as Lwjgl3Graphics).window.windowHandle), false)

        val fontBytes = Constants.imguiFontPath.readBytes()
        imguiDefaultFont = imgui.IO.fonts.addFontFromMemoryTTF(CharArray(fontBytes.size, { fontBytes[it].c }), 20f, FontConfig(), glyphRanges = imgui.IO.fonts.glyphRangesDefault)
        imguiBigFont = imgui.IO.fonts.addFontFromMemoryTTF(CharArray(fontBytes.size, { fontBytes[it].c }), 32f, FontConfig(), glyphRanges = imgui.IO.fonts.glyphRangesDefault)
    }

    override fun create() {
        super.create()
        // Permet de supprimer les logs d'imgui
        DEBUG = false

        PCGame.soundVolume = initialConfig.soundVolume
        PCGame.darkUI = initialConfig.darkUI
        PCGame.locale = initialConfig.locale

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        ResourceManager.init()

        Utility.getFilesRecursivly(Locales.menusPath.parent(), "properties").forEach {
            if (it.name().startsWith("bundle_"))
                availableLocales.add(Locale.forLanguageTag(it.nameWithoutExtension().substringAfter('_')))
        }

        GameKeys.loadKeysConfig()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Constants.mainFontPath)

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
        gameMusics = Utility.getFilesRecursivly(Constants.musicsDirPath, *Constants.levelSoundExtension)

        Gdx.input.inputProcessor = PCInputProcessor

        initializeUI()

        sceneManager = SceneManager(MainMenuScene())
    }

    override fun render() {
        super.render()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        sceneManager.update()

        TweenSystem.update()

        MusicManager.update()

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

        ResourceManager.dispose()
        MusicManager.dispose()

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

        lateinit var imguiDefaultFont: Font
        lateinit var imguiBigFont: Font

        lateinit var defaultProjection: Matrix4
            private set

        lateinit var gameAtlas: Map<FileHandle, List<FileHandle>>
            private set
        lateinit var gameTextures: List<FileHandle>
            private set
        lateinit var gameSounds: List<FileHandle>
            private set
        lateinit var gameMusics: List<FileHandle>
            private set

        lateinit var mainBackground: Background
            private set

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

                ImGui.pushStyleColor(Col.WindowBg, ImGui.getStyleColorVec4(Col.WindowBg).apply { a = 0.9f })
                ImGui.pushStyleColor(Col.PopupBg, ImGui.getStyleColorVec4(Col.PopupBg).apply { a = 0.9f })
                ImGui.pushStyleColor(Col.TitleBg, ImGui.getStyleColorVec4(Col.TitleBg).apply { a = 0.8f })
                ImGui.pushStyleColor(Col.TitleBgActive, ImGui.getStyleColorVec4(Col.TitleBgActive).apply { a = 0.8f })
                ImGui.pushStyleColor(Col.TitleBgCollapsed, ImGui.getStyleColorVec4(Col.TitleBgCollapsed).apply { a = 0.8f })
                ImGui.pushStyleVar(StyleVar.FrameRounding, 3f)
            }

        val availableLocales = mutableListOf<Locale>(Locale.FRENCH)

        fun standardBackgrounds() = standardBackgrounds.toList()
        fun parallaxBackgrounds() = parallaxBackgrounds.toList()

        /**
         * Permet de retourner le logo du jeu
         */
        fun generateLogo(container: GameObjectContainer) = GameObjectBuilder("logo", getLogoSize())
                .withDefaultState {
                    withComponent(AtlasComponent(0, AtlasComponent.AtlasData("logo", Constants.gameLogoPath.toFileWrapper())))
                }
                .build(getLogoRect().position, container)

        /**
         * Permet de retourner la taille du logo au cas où la taille de l'écran changerait.
         */
        private fun getLogoSize() = Size(600, 125)

        fun getLogoRect(): Rect {
            val size = getLogoSize()
            return Rect(Point(Constants.viewportRatioWidth / 2 - size.width / 2, Constants.viewportRatioHeight - size.height * 2), size)
        }
    }
}