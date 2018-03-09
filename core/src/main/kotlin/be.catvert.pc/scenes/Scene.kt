package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.StretchViewport
import imgui.ImGui
import ktx.app.use
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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
                it.getCurrentState().getComponent<TextureComponent>()?.alpha = value
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

        if (Gdx.input.isKeyJustPressed(GameKeys.SCREENSHOT.key))
            takeScreenshot()
    }

    override fun resize(size: Size) {
        viewport.update(size.width, size.height)
    }

    override fun dispose() {}

    /**
     * Permet de prendre une capture d'écran
     *
     */
    private fun takeScreenshot() {
        val pixmap = Utility.getPixmapOfScreen()

        val fileName = "screenshot-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"))}.png"

        PixmapIO.writePNG(Gdx.files.local(fileName), pixmap)

        pixmap.dispose()

        Log.info { "Capture d'écran effectué ! -> $fileName" }
    }
}