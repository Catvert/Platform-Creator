package be.catvert.pc

import be.catvert.pc.builders.EntityBuilder
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.i18n.Locales
import be.catvert.pc.managers.MusicsManager
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.managers.ScenesManager
import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.tweens.TweenSystem
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import glm_.c
import glm_.vec4.Vec4
import imgui.*
import imgui.impl.LwjglGL3
import ktx.app.KtxApplicationAdapter
import uno.glfw.GlfwWindow
import java.util.*
import kotlin.collections.set


/** [com.badlogic.gdx.ApplicationListener, implementation shared by all platforms.  */
class PCGame(private val initialConfig: GameConfig) : KtxApplicationAdapter {
    /**
     * Permet d'initialiser ImGui
     */
    private fun initializeUI() {
        LwjglGL3.init(GlfwWindow((Gdx.graphics as Lwjgl3Graphics).window.windowHandle), false)

        PCGame.darkUI = initialConfig.darkUI

        val fontBytes = Constants.imguiFontPath.readBytes()
        imguiDefaultFont = imgui.g.io.fonts.addFontFromMemoryTTF(CharArray(fontBytes.size, { fontBytes[it].c }), 21f, FontConfig(), glyphRanges = imgui.g.io.fonts.glyphRangesDefault)
        imguiBigFont = imgui.g.io.fonts.addFontFromMemoryTTF(CharArray(fontBytes.size, { fontBytes[it].c }), 32f, FontConfig(), glyphRanges = imgui.g.io.fonts.glyphRangesDefault)
    }

    override fun create() {
        super.create()
        // Permet de supprimer les logs d'imgui
        imgui.DEBUG = false

        PCGame.soundVolume = initialConfig.soundVolume
        PCGame.locale = initialConfig.locale

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        Utility.getFilesRecursivly(Locales.menusPath.parent(), "properties").forEach {
            if (it.name().startsWith("bundle_"))
                availableLocales.add(Locale.forLanguageTag(it.nameWithoutExtension().substringAfter('_')))
        }

        GameKeys.loadKeysConfig()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Constants.mainFontPath)

        gamePacks = let {
            val packs = mutableMapOf<FileHandle, List<FileHandle>>()

            Constants.packsDirPath.list().forEach {
                if (it.isDirectory) {
                    packs[it] = Utility.getFilesRecursivly(it, *Constants.levelPackExtension)
                }
            }

            packs
        }
        gameTextures = Utility.getFilesRecursivly(Constants.texturesDirPath, *Constants.levelTextureExtension)
        gameSounds = Utility.getFilesRecursivly(Constants.soundsDirPath, *Constants.levelSoundExtension)
        gameMusics = Utility.getFilesRecursivly(Constants.musicsDirPath, *Constants.levelSoundExtension)

        Gdx.input.inputProcessor = PCInputProcessor

        initializeUI()

        scenesManager = ScenesManager(MainMenuScene(null, true))
    }

    override fun render() {
        super.render()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        scenesManager.update()

        TweenSystem.update()

        MusicsManager.update()

        scenesManager.render(mainBatch)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        scenesManager.resize(Size(width, height))
        defaultProjection.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        super.dispose()

        GameConfig.saveGameConfig()
        GameKeys.saveKeysConfig()

        mainBatch.dispose()
        hudBatch.dispose()

        scenesManager.dispose()

        mainFont.dispose()

        ResourcesManager.dispose()
        MusicsManager.dispose()

        Log.dispose()

        LwjglGL3.shutdown()
        imguiCtx.destroy()
    }

    companion object {
        val imguiCtx = Context()

        lateinit var mainBatch: SpriteBatch
            private set

        lateinit var hudBatch: SpriteBatch
            private set

        lateinit var scenesManager: ScenesManager
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

        lateinit var gamePacks: Map<FileHandle, List<FileHandle>>
            private set
        lateinit var gameTextures: List<FileHandle>
            private set
        lateinit var gameSounds: List<FileHandle>
            private set
        lateinit var gameMusics: List<FileHandle>
            private set

        var soundVolume = 1f
            set(value) {
                if (value in 0.0..1.0)
                    field = value
            }

        var darkUI = false
            set(value) {
                field = value
                if (value) {
                    with(ImGui.style) {
                        colors[Col.WindowBg] = Vec4.fromColor(40, 44, 52, 240)
                        colors[Col.PopupBg] = Vec4.fromColor(34, 42, 53, 250)

                        colors[Col.MenuBarBg] = Vec4.fromColor(34, 42, 53, 240)

                        colors[Col.TitleBg] = Vec4.fromColor(39, 47, 58, 240)
                        colors[Col.TitleBgActive] = Vec4.fromColor(39, 47, 58, 245)
                        colors[Col.TitleBgCollapsed] = Vec4.fromColor(39, 47, 58, 240)

                        colors[Col.Button] = Vec4.fromColor(50, 77, 109, 200)
                        colors[Col.ButtonActive] = Vec4.fromColor(58, 68, 86, 255)
                        colors[Col.ButtonHovered] = Vec4.fromColor(49, 59, 76, 255)

                        colors[Col.FrameBg] = Vec4.fromColor(58, 68, 86)
                        colors[Col.FrameBgActive] = Vec4.fromColor(58, 68, 86)
                        colors[Col.FrameBgHovered] = Vec4.fromColor(49, 59, 76)

                        colors[Col.Header] = Vec4.fromColor(58, 68, 86)
                        colors[Col.HeaderActive] = Vec4.fromColor(58, 68, 86)
                        colors[Col.HeaderHovered] = Vec4.fromColor(49, 59, 76)

                        colors[Col.ScrollbarBg] = Vec4.fromColor(58, 68, 86, 240)
                        colors[Col.ScrollbarGrab] = Vec4.fromColor(40, 44, 52, 200)
                        colors[Col.ScrollbarGrabActive] = Vec4.fromColor(40, 44, 52, 255)
                        colors[Col.ScrollbarGrabHovered] = Vec4.fromColor(34, 42, 53, 255)

                        colors[Col.ResizeGrip] = Vec4.fromColor(40, 44, 52, 200)
                        colors[Col.ResizeGripActive] = Vec4.fromColor(34, 42, 53, 255)
                        colors[Col.ResizeGripHovered] = Vec4.fromColor(40, 44, 52, 255)

                        colors[Col.CheckMark] = Vec4.fromColor(66, 150, 250)

                        colors[Col.CloseButton] = Vec4.fromColor(50, 77, 109, 200)
                        colors[Col.CloseButtonActive] = Vec4.fromColor(58, 68, 86, 255)
                        colors[Col.CloseButtonHovered] = Vec4.fromColor(49, 59, 76, 255)

                        colors[Col.SliderGrab] = Vec4.fromColor(66, 150, 250)
                        colors[Col.SliderGrabActive] = Vec4.fromColor(66, 160, 255)

                        colors[Col.Text] = Vec4(1)
                    }
                } else
                    ImGui.styleColorsLight()

                ImGui.style.frameRounding = 5f
            }

        val availableLocales = mutableListOf<Locale>(Locale.FRENCH)

        fun getStandardBackgrounds() = Utility.getFilesRecursivly(Constants.backgroundsDirPath.child("standard"), *Constants.levelTextureExtension).map { StandardBackground(it.toFileWrapper()) }
        fun getParallaxBackgrounds() = Utility.getFilesRecursivly(Constants.backgroundsDirPath.child("parallax"), "data").map { ParallaxBackground(it.toFileWrapper()) }

        /**
         * Permet de retourner le logo du jeu
         */
        fun generateLogo(container: EntityContainer) = EntityBuilder("logo", getLogoSize())
                .withDefaultState {
                    withComponent(TextureComponent(0, TextureComponent.TextureData("logo", Constants.gameLogoPath.toFileWrapper())))
                }
                .build(getLogoRect().position, container)


        private fun getLogoSize() = Size(600, 125)

        fun getLogoRect(): Rect {
            val size = getLogoSize()
            return Rect(Point(Constants.viewportRatioWidth / 2 - size.width / 2, Constants.viewportRatioHeight - size.height * 2), size)
        }
    }
}