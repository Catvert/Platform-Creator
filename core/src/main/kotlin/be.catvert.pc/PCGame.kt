package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.TextureComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.MainMenuScene
import be.catvert.pc.scenes.Scene
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.kotcrab.vis.ui.VisUI
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Col
import imgui.ImGui
import imgui.impl.LwjglGL3
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import ktx.app.KtxApplicationAdapter
import ktx.assets.toLocalFile
import ktx.async.enableKtxCoroutines
import uno.glfw.GlfwWindow
import kotlin.reflect.KClass

/** [com.badlogic.gdx.ApplicationListener, implementation shared by all platforms.  */
class PCGame(private val initialVSync: Boolean, private val initialSoundVolume: Float) : KtxApplicationAdapter {
    override fun create() {
        super.create()
        enableKtxCoroutines(asynchronousExecutorConcurrencyLevel = 1)

        Log.info { "Initialisation en cours.. \n Taille : ${Gdx.graphics.width}x${Gdx.graphics.height}" }

        VisUI.load(Gdx.files.internal(Constants.UISkinPath))

        GameKeys.loadKeysConfig()

        mainBatch = SpriteBatch()
        hudBatch = SpriteBatch()

        PCGame.vsync = initialVSync
        PCGame.soundVolume = initialSoundVolume
        PCGame.defaultProjection = mainBatch.projectionMatrix.cpy()

        PCGame.mainFont = BitmapFont(Constants.mainFontPath.toLocalFile())

        imgui.IO.mouseDrawCursor = true

        Utility.getFilesRecursivly(Constants.backgroundsDirPath.toLocalFile(), *Constants.levelTextureExtension).forEach {
            backgroundsList.add(it)
        }

        loadedAtlas = Utility.getFilesRecursivly(Constants.atlasDirPath.toLocalFile(), *Constants.levelAtlasExtension)

        loadedTextures = Utility.getFilesRecursivly(Constants.texturesDirPath.toLocalFile(), *Constants.levelTextureExtension)

        currentScene = MainMenuScene()

        val handle = (Gdx.graphics as Lwjgl3Graphics).window.let {
            it::class.java.getDeclaredField("windowHandle").apply { isAccessible = true }.getLong(it)
        }
        LwjglGL3.init(GlfwWindow(handle), false)

        setupImguiStyle()
        ImGui.styleColorsDark()
    }

    override fun render() {
        super.render()

        LwjglGL3.newFrame()

        Gdx.graphics.setTitle(Constants.gameTitle + " - ${Gdx.graphics.framesPerSecond} FPS")

        currentScene.update()

        currentScene.render(mainBatch)

        ImGui.render()

        currentScene.calcIsUIHover()
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

        mainFont.dispose()

        VisUI.dispose()

        assetManager.dispose()

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
        private val backgroundsList = mutableListOf<FileHandle>()

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

        lateinit var loadedAtlas: List<FileHandle>
            private set

        lateinit var loadedTextures: List<FileHandle>

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

        var vsync = false
            set(value) {
                field = value
                Gdx.graphics.setVSync(vsync)
            }

        var soundVolume = 1f
            set(value) {
                if(value in 0.0..1.0)
                    field = value
            }

        val assetManager = AssetManager()

        private lateinit var currentScene: Scene

        fun getBackgrounds() = backgroundsList.toList()

        /**
         * Permet de retourner le logo du jeu
         */
        fun generateLogo(container: GameObjectContainer): GameObject {
            return container.createGameObject(getLogoRect(), GameObject.Tag.Sprite, {
                this += TextureComponent(Constants.gameLogoPath.toLocalFile())
            })
        }

        /**
         * Permet de retourner la taille du logo au cas où la taille de l'écran changerait.
         */
        private fun getLogoSize(): Size {
            return Size(Gdx.graphics.width / 3 * 2, Gdx.graphics.height / 4)
        }

        fun getLogoRect(): Rect {
            val size = getLogoSize()
            return Rect(Point(Gdx.graphics.width / 2 - size.width / 2, Gdx.graphics.height - size.height), size)
        }

        fun setScene(newScene: Scene, disposeCurrentScene: Boolean = true) {
            Log.info { "Chargement de la scène : ${ReflectionUtility.simpleNameOf(newScene)}" }
            if (disposeCurrentScene)
                currentScene.dispose()
            currentScene = newScene
        }
    }
}