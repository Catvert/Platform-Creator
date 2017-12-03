package be.catvert.pc.utility

import be.catvert.pc.PCGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import ktx.assets.Asset
import ktx.assets.loadOnDemand
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor


fun Batch.draw(texture: Texture, rect: Rect, flipX: Boolean = false, flipY: Boolean = false) = this.draw(texture, rect.position.x.toFloat(), rect.position.y.toFloat(), rect.size.width.toFloat(), rect.size.height.toFloat(), 0, 0, texture.width, texture.height, flipX, flipY)

fun Batch.draw(textureRegion: TextureRegion, rect: Rect, flipX: Boolean = false, flipY: Boolean = false) {
    if (flipX && !textureRegion.isFlipX || !flipX && textureRegion.isFlipX) {
        textureRegion.flip(true, false)
    }
    if (flipY && !textureRegion.isFlipY || !flipY && textureRegion.isFlipY) {
        textureRegion.flip(false, true)
    }

    this.draw(textureRegion, rect.position.x.toFloat(), rect.position.y.toFloat(), rect.size.width.toFloat(), rect.size.height.toFloat())
}

fun ShapeRenderer.rect(rect: Rect) = this.rect(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat())

fun Vector2.toPoint() = Point(this.x.roundToInt(), this.y.roundToInt())

fun Vector3.toPoint() = Point(this.x.roundToInt(), this.y.roundToInt())

fun Shape2D.contains(point: Point) = this.contains(point.x.toFloat(), point.y.toFloat())

fun Graphics.toSize() = Size(width, height)

inline fun <reified T : Any> AssetManager.loadOnDemand(file: FileWrapper): Asset<T> = this.loadOnDemand(file.get())
inline fun <reified T : Any> AssetManager.loadOnDemand(file: FileHandle): Asset<T> = this.loadOnDemand(file.path())

fun FileHandle.toFileWrapper() = FileWrapper(this)

object Utility {
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

    private val configPath = Constants.assetsDir + "config.json"

    data class GameConfig(val width: Int, val height: Int, val fullscreen: Boolean, val soundVolume: Float)

    /**
     * Charge le fichier de configuration du jeu
     */
    fun loadGameConfig(): GameConfig {
        if (File(configPath).exists()) {
            try {
                val root = JsonReader().parse(FileReader(configPath))

                val screenWidth = root.getInt("width")
                val screenHeight = root.getInt("height")
                val fullscreen = root.getBoolean("fullscreen")
                val soundVolume = root.getFloat("soundvolume")

                return GameConfig(screenWidth, screenHeight, fullscreen, soundVolume)
            } catch (e: Exception) {
                System.err.println("Erreur lors du chargement de la configuration du jeu ! Erreur : ${e.message}")
            }
        }

        return GameConfig(1280, 720, true, 1f)
    }

    /**
     * Permet de sauvegarder la configuration du jeu
     */
    fun saveGameConfig(width: Int, height: Int): Boolean {
        try {
            val writer = JsonWriter(FileWriter(configPath, false))
            writer.setOutputType(JsonWriter.OutputType.json)

            writer.`object`()

            writer.name("width").value(width)
            writer.name("height").value(height)
            writer.name("fullscreen").value(Gdx.graphics.isFullscreen)
            writer.name("soundvolume").value(PCGame.soundVolume)

            writer.pop()

            writer.flush()
            writer.close()

            return true
        } catch (e: IOException) {
            return false
        }
    }
}

object ReflectionUtility {
    inline fun <reified T : Any> hasNoArgConstructor(klass: KClass<out T>) = findNoArgConstructor(klass) != null

    inline fun <reified T : Any> findNoArgConstructor(klass: KClass<out T>): Constructor<T>? {
        klass.constructors.forEach {
            if (it.parameters.isEmpty())
                return it.javaConstructor.apply { it.isAccessible = true }
        }
        return null
    }


    fun getAllFieldsOf(type: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()

        var i: Class<*>? = type

        while (i != null && i != Any::class.java) {
            fields.addAll(i.declaredFields)
            i = i.superclass
        }

        return fields
    }

    fun simpleNameOf(instance: Any) = instance.javaClass.kotlin.simpleName ?: "Nom introuvable"
}