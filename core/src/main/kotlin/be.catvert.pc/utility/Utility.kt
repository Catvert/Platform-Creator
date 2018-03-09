package be.catvert.pc.utility

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.esotericsoftware.reflectasm.ClassAccess
import ktx.assets.Asset
import ktx.assets.loadOnDemand
import java.lang.reflect.Field
import kotlin.math.roundToInt

/**
 * Extension permettant de dessiner facilement une texture avec un Rect
 * @see Rect
 */
fun Batch.draw(textureRegion: TextureRegion, rect: Rect, flipX: Boolean = false, flipY: Boolean = false, rotation: Float = 0f) {
    if (flipX && !textureRegion.isFlipX || !flipX && textureRegion.isFlipX) {
        textureRegion.flip(true, false)
    }
    if (flipY && !textureRegion.isFlipY || !flipY && textureRegion.isFlipY) {
        textureRegion.flip(false, true)
    }

    this.draw(textureRegion, rect.x.roundToInt().toFloat(), rect.y.roundToInt().toFloat(), rect.width / 2f, rect.height / 2f, rect.width.toFloat(), rect.height.toFloat(), 1f, 1f, rotation)
}

fun ShapeRenderer.rect(rect: Rect) = this.rect(rect.x, rect.y, rect.width.toFloat(), rect.height.toFloat())

fun Vector2.toPoint() = Point(this.x, this.y)

fun Vector3.toPoint() = Point(this.x, this.y)

fun Shape2D.contains(point: Point) = this.contains(point.x, point.y)

inline fun <reified T : Any> AssetManager.loadOnDemand(file: FileHandle): Asset<T> = this.loadOnDemand(file.path())

fun FileHandle.toFileWrapper() = FileWrapper(this)

fun Texture.toAtlasRegion() = TextureAtlas.AtlasRegion(this, 0, 0, width, height)

fun Float.equalsEpsilon(v: Float, epsilon: Float) = Math.abs(this - v) < epsilon

fun Batch.withColor(color: Color, block: Batch.() -> Unit) {
    val oldColor = this.color.cpy()
    this.color = color
    this.block()
    this.color = oldColor
}

fun ShapeRenderer.withColor(color: Color, block: ShapeRenderer.() -> Unit) {
    val oldColor = this.color.cpy()
    this.color = color
    this.block()
    this.color = oldColor
}

inline fun <reified T : Any> Any?.cast(): T? = this as? T

object Utility {
    /**
     * Permet de parcourir les fichiers d'un dossier récursivement en ne gardant que les fichiers ayant une extension particulière
     */
    fun getFilesRecursivly(dir: FileHandle, vararg fileExt: String = arrayOf()): List<FileHandle> {
        val files = mutableListOf<FileHandle>()

        dir.list().forEach {
            if (it.isDirectory)
                files += getFilesRecursivly(it, *fileExt)
            else {
                if (fileExt.isEmpty() || fileExt.contains(it.extension()))
                    files += it
            }
        }
        return files
    }

    /**
     * Inspiré de : https://github.com/libgdx/libgdx/wiki/Taking-a-Screenshot
     */
    fun getPixmapOfScreen(): Pixmap {
        val pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, true)

        // Permet d'être sûr que l'entièreté du screenshot est opaque
        var i = 4
        while (i < pixels.size) {
            pixels[i - 1] = 255.toByte()
            i += 4
        }

        val pixmap = Pixmap(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, Pixmap.Format.RGBA8888)
        BufferUtils.copy(pixels, 0, pixmap.pixels, pixels.size)

        return pixmap
    }

    /**
     * Permet de retourné l'écart de temps entre 2 rafraîchissement de l'écran en évitant la spirale de la mort
     */
    fun getDeltaTime() = Math.min(Gdx.graphics.deltaTime, 0.1f)
}

object ReflectionUtility {
    /**
     * Permet de créer un object dynamiquement
     */
    fun <T> createInstance(type: Class<T>, vararg args: Any): T {
        val access = ClassAccess.access(type)
        return access.newInstance(*args) as T
    }

    /**
     * Permet d'obtenir tout les champs d'une classe
     */
    fun getAllFieldsOf(type: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()

        var i: Class<*>? = type

        while (i != null && i != Any::class.java) {
            fields.addAll(i.declaredFields)
            i = i.superclass
        }

        return fields
    }

    /**
     * Permet d'obtenir le nom d'une classe
     */
    fun simpleNameOf(instance: Any) = instance.javaClass.kotlin.simpleName ?: "Nom introuvable"
}