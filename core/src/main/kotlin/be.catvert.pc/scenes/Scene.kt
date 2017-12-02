package be.catvert.pc.scenes

import aurelienribon.tweenengine.BaseTween
import aurelienribon.tweenengine.TweenAccessor
import aurelienribon.tweenengine.TweenCallback
import aurelienribon.tweenengine.TweenUtils
import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import imgui.ImGui
import ktx.actors.alpha
import ktx.app.clearScreen
import ktx.app.use

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene : Renderable, Updeatable, Resizable, Disposable {
    protected open val camera = OrthographicCamera().apply { setToOrtho(false) }
    val stage = Stage(ScreenViewport(), PCGame.hudBatch)

    val backgroundColors = Triple(0f, 0f, 0f)

    protected var backgroundTexture: Texture? = null
    private var backgroundSize = Gdx.graphics.toSize()

    protected open var gameObjectContainer: GameObjectContainer = object : GameObjectContainer() {}

    protected var isUIHover = false
        private set

    var alpha = 1f
        set(value) {
            field = value
            gameObjectContainer.getGameObjectsData().forEach {
                it.getStates().forEach {
                    it.getComponent<RenderableComponent>()?.alpha = value
                }
            }
            stage.alpha = value
        }

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

        isUIHover = imgui.findHoveredWindow(ImGui.mousePos) != null || ImGui.isAnyItemHovered
    }

    protected fun hideUI() {
        stage.actors.forEach { it.isVisible = false }
    }

    protected fun showUI() {
        stage.actors.forEach { it.isVisible = true }
    }

    override fun render(batch: Batch) {
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
        camera.setToOrtho(false, size.width.toFloat(), size.height.toFloat())
        stage.viewport.update(size.width, size.height)
        backgroundSize = Gdx.graphics.toSize()
    }

    override fun dispose() {
        stage.dispose()
    }
}

class SceneTweenAccessor : TweenAccessor<Scene> {
    enum class SceneTween(val tweenType: Int) {
        ALPHA(0);
        companion object {
            fun fromType(tweenType: Int) = values().firstOrNull { it.tweenType == tweenType }
        }
    }

    override fun setValues(scene: Scene, tweenType: Int, newValues: FloatArray) {
        when (SceneTween.fromType(tweenType)) {
            SceneTween.ALPHA -> {
                scene.alpha = newValues[0]
            }
            else -> Log.error { "Tween inconnu : $tweenType" }
        }
    }

    override fun getValues(scene: Scene, tweenType: Int, returnValues: FloatArray): Int {
        when (SceneTween.fromType(tweenType)) {
            SceneTween.ALPHA -> {
                returnValues[0] = scene.alpha; return 1
            }
            else -> Log.error { "Tween inconnu : $tweenType" }
        }

        return -1
    }
}