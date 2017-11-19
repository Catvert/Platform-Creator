package be.catvert.pc.scenes

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import glm_.vec2.Vec2
import imgui.ImGui
import ktx.app.clearScreen
import ktx.app.use

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene : Renderable, Updeatable, Resizable, Disposable {
    protected open val camera = OrthographicCamera().apply { setToOrtho(false) }
    protected val stage = Stage(ScreenViewport(), PCGame.hudBatch)

    protected val backgroundColors = Triple(0f, 0f, 0f)

    protected var backgroundTexture: Texture? = null
    private var backgroundSize = Gdx.graphics.toSize()

    protected open var gameObjectContainer: GameObjectContainer = object : GameObjectContainer() {}

    protected var isUIHover = false
        private set

    init {
        Gdx.input.inputProcessor = stage
    }

    protected open fun postBatchRender() {}

    fun calcIsUIHover() {
        isUIHover = false

        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.input.y.toFloat()

        stage.actors.filter { it.isVisible }.forEach {
            if ((mouseX >= it.x && mouseX <= it.x + it.width) && (Gdx.graphics.height - mouseY <= it.y + it.height && Gdx.graphics.height - mouseY >= it.y)) {
                isUIHover = true
                return
            }
        }

        isUIHover = imgui.findHoveredWindow(ImGui.mousePos) != null
    }

    protected fun hideUI() {
        stage.actors.forEach { it.isVisible = false }
    }

    protected fun showUI() {
        stage.actors.forEach { it.isVisible = true }
    }

    override fun render(batch: Batch) {
        clearScreen(backgroundColors.first, backgroundColors.second, backgroundColors.third)

        batch.projectionMatrix = PCGame.defaultProjection
        batch.use {
            if (backgroundTexture != null) {
                it.draw(backgroundTexture, 0f, 0f, backgroundSize.width.toFloat(), backgroundSize.height.toFloat())
            }
            it.projectionMatrix = camera.combined
            gameObjectContainer.render(batch)
        }

        postBatchRender()

        stage.draw()
    }

    override fun update() {
        gameObjectContainer.update()
        stage.act(Gdx.graphics.deltaTime)
    }

    override fun resize(size: Size) {
       // camera.setToOrtho(false, size.width.toFloat(), size.height.toFloat())
        stage.viewport.update(size.width, size.height)
        backgroundSize = Gdx.graphics.toSize()
    }

    override fun dispose() {
        stage.dispose()
    }
}