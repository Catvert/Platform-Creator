package be.catvert.pc

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.i18n.Locales
import be.catvert.pc.scenes.Scene
import be.catvert.pc.scenes.SceneManager
import be.catvert.pc.scenes.SceneTweenAccessor
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Matrix4
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Col
import imgui.ImGui
import imgui.impl.LwjglGL3
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import ktx.app.KtxApplicationAdapter
import uno.glfw.GlfwWindow
import kotlin.reflect.KClass

/** [com.badlogic.gdx.ApplicationListener, implementation shared by all platforms.  */
class PCGame(private val initialSoundVolume: Float) : KtxApplicationAdapter {
    override fun create() {
        super.create()

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        ResourceManager.init()

        Locales.load()

        GameKeys.loadKeysConfig()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.soundVolume = initialSoundVolume
        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Constants.mainFontPath)

        imgui.IO.mouseDrawCursor = true

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

            Constants.atlasDirPath.list().forEach {
                if(it.isDirectory) {
                    atlas[it] = Utility.getFilesRecursivly(it, *Constants.levelAtlasExtension)
                }
            }

            atlas
        }
        gameTextures = Utility.getFilesRecursivly(Constants.texturesDirPath, *Constants.levelTextureExtension)
        gameSounds = Utility.getFilesRecursivly(Constants.soundsDirPath, *Constants.levelSoundExtension)

        val handle = (Gdx.graphics as Lwjgl3Graphics).window.let {
            it::class.java.getDeclaredField("windowHandle").apply { isAccessible = true }.getLong(it)
        }
        LwjglGL3.init(GlfwWindow(handle), false)

        setupImguiStyle()
        ImGui.styleColorsDark()
    }

    override fun render() {
        super.render()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        SceneManager.update()

        tweenManager.update(Gdx.graphics.deltaTime)

        SceneManager.render(mainBatch)

        SceneManager.currentScene().calcIsUIHover()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        SceneManager.resize(Size(width, height))
        defaultProjection.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        super.dispose()

        mainBatch.dispose()
        hudBatch.dispose()

        SceneManager.dispose()

        mainFont.dispose()

        ResourceManager.dispose()

        Log.dispose()

        LwjglGL3.shutdown()
    }


    private fun setupImguiStyle() {
        //Inspiré de https://github.com/ocornut/imgui/issues/1269 -> berkay2578
        with(ImGui) {
            style.windowPadding = Vec2(10.0f, 10.0f)
            style.windowRounding = 5.0f
            style.childRounding = 5.0f
            style.framePadding = Vec2(5.0f, 4.0f)
            style.frameRounding = 5.0f
            style.itemSpacing = Vec2(5.0f, 5.0f)
            style.itemInnerSpacing = Vec2(10.0f, 10.0f)
            style.indentSpacing = 15.0f
            style.scrollbarSize = 16.0f
            style.scrollbarRounding = 5.0f
            style.grabMinSize = 7.0f
            style.grabRounding = 2.0f

            pushStyleColor(Col.Text, Vec4(0.00f, 0.00f, 0.00f, 1.00f))
            pushStyleColor(Col.TextDisabled, Vec4(0.59f, 0.59f, 0.59f, 1.00f))
            pushStyleColor(Col.WindowBg, Vec4(1.00f, 1.00f, 1.00f, 0.90f))
            pushStyleColor(Col.ChildBg, Vec4(0.92f, 0.92f, 0.92f, 1.00f))
            pushStyleColor(Col.PopupBg, Vec4(1.00f, 1.00f, 1.00f, 0.90f))
            pushStyleColor(Col.Border, Vec4(0.00f, 0.00f, 0.00f, 0.80f))
            pushStyleColor(Col.BorderShadow, Vec4(0.00f, 0.00f, 0.00f, 0.00f))
            pushStyleColor(Col.FrameBg, Vec4(0.71f, 0.71f, 0.71f, 0.39f))
            pushStyleColor(Col.FrameBgHovered, Vec4(0.00f, 0.59f, 0.80f, 0.43f))
            pushStyleColor(Col.FrameBgActive, Vec4(0.00f, 0.47f, 0.71f, 0.67f))
            pushStyleColor(Col.TitleBg, Vec4(1.00f, 1.00f, 1.00f, 0.80f))
            pushStyleColor(Col.TitleBgCollapsed, Vec4(0.78f, 0.78f, 0.78f, 0.39f))
            pushStyleColor(Col.TitleBgActive, Vec4(1.00f, 1.00f, 1.00f, 1.00f))
            pushStyleColor(Col.MenuBarBg, Vec4(0.90f, 0.90f, 0.90f, 1.00f))
            pushStyleColor(Col.ScrollbarBg, Vec4(0.20f, 0.25f, 0.30f, 0.60f))
            pushStyleColor(Col.ScrollbarGrab, Vec4(0.00f, 0.00f, 0.00f, 0.39f))
            pushStyleColor(Col.ScrollbarGrabHovered, Vec4(0.00f, 0.00f, 0.00f, 0.59f))
            pushStyleColor(Col.ScrollbarGrabActive, Vec4(0.00f, 0.00f, 0.00f, 0.78f))
            pushStyleColor(Col.CheckMark, Vec4(0.27f, 0.59f, 0.75f, 1.00f))
            pushStyleColor(Col.SliderGrab, Vec4(0.00f, 0.00f, 0.00f, 0.35f))
            pushStyleColor(Col.SliderGrabActive, Vec4(0.00f, 0.00f, 0.00f, 0.59f))
            pushStyleColor(Col.Button, Vec4(0.00f, 0.00f, 0.00f, 0.27f))
            pushStyleColor(Col.ButtonHovered, Vec4(0.00f, 0.59f, 0.80f, 0.43f))
            pushStyleColor(Col.ButtonActive, Vec4(0.00f, 0.47f, 0.71f, 0.67f))
            pushStyleColor(Col.Header, Vec4(0.71f, 0.71f, 0.71f, 0.39f))
            pushStyleColor(Col.HeaderHovered, Vec4(0.20f, 0.51f, 0.67f, 1.00f))
            pushStyleColor(Col.HeaderActive, Vec4(0.08f, 0.39f, 0.55f, 1.00f))
            pushStyleColor(Col.Separator, Vec4(0.00f, 0.00f, 0.00f, 1.00f))
            pushStyleColor(Col.SeparatorHovered, Vec4(0.27f, 0.59f, 0.75f, 1.00f))
            pushStyleColor(Col.SeparatorActive, Vec4(0.08f, 0.39f, 0.55f, 1.00f))
            pushStyleColor(Col.ResizeGrip, Vec4(0.00f, 0.00f, 0.00f, 0.78f))
            pushStyleColor(Col.ResizeGripHovered, Vec4(0.27f, 0.59f, 0.75f, 0.78f))
            pushStyleColor(Col.ResizeGripActive, Vec4(0.08f, 0.39f, 0.55f, 0.78f))
            pushStyleColor(Col.CloseButton, Vec4(0.00f, 0.00f, 0.00f, 0.50f))
            pushStyleColor(Col.CloseButtonHovered, Vec4(0.71f, 0.71f, 0.71f, 0.60f))
            pushStyleColor(Col.CloseButtonActive, Vec4(0.59f, 0.59f, 0.59f, 1.00f))
            pushStyleColor(Col.PlotLines, Vec4(1.00f, 1.00f, 1.00f, 1.00f))
            pushStyleColor(Col.PlotLinesHovered, Vec4(0.90f, 0.70f, 0.00f, 1.00f))
            pushStyleColor(Col.PlotHistogram, Vec4(0.90f, 0.70f, 0.00f, 1.00f))
            pushStyleColor(Col.PlotHistogramHovered, Vec4(1.00f, 0.60f, 0.00f, 1.00f))
            pushStyleColor(Col.TextSelectedBg, Vec4(0.27f, 0.59f, 0.75f, 1.00f))
            pushStyleColor(Col.ModalWindowDarkening, Vec4(0.00f, 0.00f, 0.00f, 0.35f))
        }
    }

    companion object {
        private val standardBackgrounds = mutableListOf<StandardBackground>()
        private val parallaxBackgrounds = mutableListOf<ParallaxBackground>()

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
        private fun getLogoSize() = Size(Gdx.graphics.width / 3, Gdx.graphics.height / 10)

        fun getLogoRect(): Rect {
            val size = getLogoSize()
            return Rect(Point(Gdx.graphics.width / 2 - size.width / 2, Gdx.graphics.height - size.height * 3 / 2), size)
        }
    }
}