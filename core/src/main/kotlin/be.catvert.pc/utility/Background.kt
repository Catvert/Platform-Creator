package be.catvert.pc.utility

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.managers.ResourcesManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.JsonReader
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ktx.math.div
import java.io.FileReader
import kotlin.math.roundToInt

/**
 * Énumération des différents types de fond d'écran.
 */
enum class BackgroundType {
    Standard, Parallax, Imported, None;

    override fun toString() = when (this) {
        BackgroundType.Standard -> "Standard"
        BackgroundType.Parallax -> "Parallaxe"
        BackgroundType.Imported -> "Importé"
        BackgroundType.None -> "Aucun"
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Background(val type: BackgroundType) : Renderable

/**
 * Fond d'écran simple, une seule image de fond.
 */
open class StandardBackground(val backgroundFile: FileWrapper) : Background(BackgroundType.Standard) {
    private val background = ResourcesManager.getTexture(backgroundFile.get())

    override fun render(batch: Batch) {
        batch.draw(background, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    }
}

/**
 * Marqueur pour spécifier que le fond d'écran est importé -> même fonction que le standard background
 */
class ImportedBackground(backgroundFile: FileWrapper): StandardBackground(backgroundFile)

/**
 * Fond d'écran à déplacement parallaxe. Plusieurs images sont utilisées et avec une différence de vitesse de déplacement ainsi qu'un système de couche,
 * on réussi à obtenir une impression de profondeur.
 */
class ParallaxBackground(val parallaxDataFile: FileWrapper) : Background(BackgroundType.Parallax) {
    private data class Layer(val layer: TextureRegion, val applyYOffset: Boolean, val speed: Float, var x: Float = 0f)

    private val layers = mutableListOf<Layer>()

    private var lastCameraPos = Vector3(2000f / 2, 2000f / 2, 0f)

    private var xOffset = 0
    private var yOffset = 0f

    init {
        try {
            val root = JsonReader().parse(FileReader(parallaxDataFile.get().path()))

            root["layers"].forEach {
                val layerFile = it.getString("file")
                val applyYOffset = it.getBoolean("applyYOffset")
                val speed = it.getFloat("speed")
                layers += Layer(TextureRegion(ResourcesManager.getTexture(parallaxDataFile.get().parent().child(layerFile)).apply { setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }), applyYOffset, speed)
            }
        } catch (e: Exception) {
            Log.error(e) { "Une erreur s'est produite lors du chargement d'un fond d'écran parallax !" }
        }
    }

    private fun updateXOffset(plusX: Int) {
        xOffset = Math.max(0, xOffset + plusX)

        layers.forEach {
            it.layer.regionWidth = Gdx.graphics.width + xOffset
        }
    }

    fun updateOffsets(camera: OrthographicCamera) {
        if (lastCameraPos != camera.position.cpy() / camera.zoom) {
            val deltaX = (camera.position.x / camera.zoom) - lastCameraPos.x
            val deltaY = ((camera.position.y / camera.zoom) - lastCameraPos.y) / 2
            yOffset = Math.min(0f, yOffset - deltaY)

            layers.forEach {
                val move = (deltaX * it.speed)
                it.x = Math.min(0f, it.x - move)
                updateXOffset(move.roundToInt())
            }
        }
        lastCameraPos = camera.position.cpy() / camera.zoom
    }

    override fun render(batch: Batch) {
        layers.forEach {
            batch.draw(it.layer, it.x, if (it.applyYOffset) yOffset else 0f, Gdx.graphics.width.toFloat() + xOffset, Gdx.graphics.height.toFloat())
        }
    }
}

