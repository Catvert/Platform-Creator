package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.StretchViewport
import imgui.ImGui
import ktx.actors.alpha
import ktx.app.use

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene(protected var background: Background) : Renderable, Updeatable, Resizable, Disposable {
    protected val camera = OrthographicCamera()

    public val viewport = StretchViewport(Constants.viewportRatioWidth, Constants.viewportRatioHeight, camera)

    protected val stage = Stage(viewport)

    val backgroundColors = Triple(0f, 0f, 0f)

    protected open var gameObjectContainer: GameObjectContainer = object : GameObjectContainer() {}

    protected var isUIHover = false
        private set

    private val stageClickListener = ClickListener()

    var alpha = 1f
        set(value) {
            field = value
            gameObjectContainer.getGameObjectsData().forEach {
                it.getCurrentState().getComponent<AtlasComponent>()?.alpha = value
            }
        }

    init {
        Gdx.input.inputProcessor = InputMultiplexer(stage, PCInputProcessor)
        stage.addListener(stageClickListener)
    }

    protected open fun postBatchRender() {}

    fun calcIsUIHover() {
        isUIHover = false

        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.input.y.toFloat()

        val mousePos = stage.screenToStageCoordinates(Vector2(mouseX, mouseY))

        isUIHover = imgui.findHoveredWindow(ImGui.mousePos) != null || ImGui.isAnyItemActive || ImGui.isAnyItemHovered || stage.actors.any {
            (it.x <= mousePos.x && it.x + it.width >= mousePos.x) && (it.y <= mousePos.y && it.y + it.height >= mousePos.y)
        }
    }

    override fun render(batch: Batch) {
        batch.projectionMatrix = PCGame.defaultProjection
        batch.use {
            background.render(it)

            it.projectionMatrix = camera.combined
            gameObjectContainer.render(it)
        }

        postBatchRender()

        stage.alpha = alpha
        stage.draw()
    }

    override fun update() {
        gameObjectContainer.update()

        background.cast<ParallaxBackground>()?.updateOffsets(camera)

        stage.act()
    }

    override fun resize(size: Size) {
        viewport.update(size.width, size.height)
    }

    override fun dispose() {
        stage.dispose()
    }
}