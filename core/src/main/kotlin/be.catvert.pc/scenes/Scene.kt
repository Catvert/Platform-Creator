package be.catvert.pc.scenes

import be.catvert.pc.PCGame
import be.catvert.pc.eca.components.graphics.AtlasComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.StretchViewport
import imgui.ImGui
import ktx.app.use

/**
 * Classe abstraite permettant l'implémentation d'une scène
 */
abstract class Scene(protected var background: Background?,
                     var backgroundColors: FloatArray = floatArrayOf(0f, 0f, 0f)) : Renderable, Updeatable, Resizable, Disposable {
    protected val camera = OrthographicCamera()

    val viewport = StretchViewport(Constants.viewportRatioWidth, Constants.viewportRatioHeight, camera)

    protected open var entityContainer: EntityContainer = object : EntityContainer() {}

    protected var isUIHover = false
        private set

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    var alpha = 1f
        set(value) {
            field = value
            entityContainer.getEntitiesData().forEach {
                it.getCurrentState().getComponent<AtlasComponent>()?.alpha = value
            }
        }

    protected open fun postBatchRender() {}

    override fun render(batch: Batch) {
        batch.projectionMatrix = PCGame.defaultProjection
        batch.use {
            background?.render(it)

            it.projectionMatrix = camera.combined
            entityContainer.render(it)
        }

        postBatchRender()
    }

    override fun update() {
        entityContainer.update()

        background.cast<ParallaxBackground>()?.updateOffsets(camera)

        isUIHover = imgui.findHoveredWindow() != null || ImGui.isAnyItemActive || ImGui.isAnyItemHovered
    }

    override fun resize(size: Size) {
        viewport.update(size.width, size.height)
    }

    override fun dispose() {}
}