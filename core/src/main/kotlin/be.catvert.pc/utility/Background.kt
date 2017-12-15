package be.catvert.pc.utility

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.JsonReader
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.FileReader
import kotlin.math.roundToInt

enum class BackgroundType {
    Standard, Parallax
}

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Background(val type: BackgroundType) : Renderable

class StandardBackground(val backgroundFile: FileWrapper) : Background(BackgroundType.Standard) {
    private val background = ResourceManager.getTexture(backgroundFile.get())

    override fun render(batch: Batch) {
        batch.draw(background, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    }
}

class ParallaxBackground(val parallaxDataFile: FileWrapper) : Background(BackgroundType.Parallax) {
    private data class Layer(val layer: TextureRegion, val applyYOffset: Boolean, val speed: Float, var x: Float = 0f)

    private val layers = mutableListOf<Layer>()

    private var lastCameraPos: Vector3? = null

    private var width = Gdx.graphics.width

    private var yOffset = 0f

    init {
        try {
            val root = JsonReader().parse(FileReader(parallaxDataFile.get().path()))

            root["layers"].forEach {
                val layerFile = it.getString("file")
                val applyYOffset = it.getBoolean("applyYOffset")
                val speed = it.getFloat("speed")
                layers += Layer(TextureRegion(ResourceManager.getTexture(parallaxDataFile.get().parent().child(layerFile)).apply { setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }), applyYOffset, speed)
            }
        } catch (e: Exception) {
            Log.error(e) { "Une erreur s'est produite lors du chargement d'un fond d'écran parallax !" }
        }
    }

    fun updateWidth(width: Int) {
        this.width = width

        layers.forEach {
            it.layer.regionWidth = width
        }
    }

    fun updateCamera(camera: OrthographicCamera) {
        if (lastCameraPos != null && lastCameraPos != camera.position) {
            val deltaX = camera.position.x - lastCameraPos!!.x
            val deltaY = (camera.position.y - lastCameraPos!!.y) / 2f
            yOffset = Math.min(0f, yOffset - deltaY)

            layers.forEach {
                val move = (deltaX * it.speed)
                it.x -= move
                updateWidth(width + move.roundToInt())
            }
        }
        lastCameraPos = camera.position.cpy()
    }

    override fun render(batch: Batch) {
        layers.forEach {
            batch.draw(it.layer, it.x, if (it.applyYOffset) yOffset else 0f, width.toFloat(), Gdx.graphics.height.toFloat())
        }
    }
}
