package be.catvert.pc.scenes

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectContainer
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.clearScreen

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene : Renderable, Updeatable, Resizable, Disposable, GameObjectContainer {
    protected val camera = OrthographicCamera()
    protected val stage = Stage(ScreenViewport(), PCGame.hudBatch)

    protected var allowRendering = true
    protected var allowUpdating = true

    protected val backgroundColors = Triple(0f, 0f, 0f)

    protected var backgroundTexture: Texture? = null
    private var backgroundSize = Gdx.graphics.toSize()

    override val gameObjects: MutableSet<GameObject> = mutableSetOf()

    init {
        Gdx.input.inputProcessor = stage

        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    }

    open fun additionalRender(batch: Batch) {}

    final override fun render(batch: Batch) {
        clearScreen(backgroundColors.first, backgroundColors.second, backgroundColors.third)

        if (allowRendering) {
            batch.begin()

            batch.projectionMatrix = PCGame.defaultProjection
            if (backgroundTexture != null) {
                batch.draw(backgroundTexture, 0f, 0f, backgroundSize.width.toFloat(), backgroundSize.height.toFloat())
            }

            batch.projectionMatrix = camera.combined
            gameObjects.forEach { it.render(batch) }

            additionalRender(batch)

            batch.end()
        }

        stage.draw()
    }

    override fun update() {
        if (allowUpdating)
            gameObjects.forEach { it.update() }

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