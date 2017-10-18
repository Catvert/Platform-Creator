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
import ktx.app.use

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene : Renderable, Updeatable, Resizable, Disposable {
    protected val camera = OrthographicCamera()
    protected val stage = Stage(ScreenViewport(), PCGame.hudBatch)

    protected val backgroundColors = Triple(0f, 0f, 0f)

    protected var backgroundTexture: Texture? = null
    private var backgroundSize = Gdx.graphics.toSize()

    protected open val gameObjectContainer: GameObjectContainer = object : GameObjectContainer() {}

    init {
        Gdx.input.inputProcessor = stage

        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    }


    protected open fun postBatchRender() {}

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
        camera.setToOrtho(false, size.width.toFloat(), size.height.toFloat())
        stage.viewport.update(size.width, size.height)
        backgroundSize = Gdx.graphics.toSize()
    }

    override fun dispose() {
        stage.dispose()
    }
}