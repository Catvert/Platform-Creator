package be.catvert.pc.utility

import be.catvert.pc.Log
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.I18NBundle

object ResourceManager : Disposable {
    private val assetManager = AssetManager()

    lateinit var defaultTexture: Texture
        private set
    lateinit var defaultPack: TextureAtlas
        private set
    lateinit var defaultPackRegion: TextureAtlas.AtlasRegion
        private set

    fun init() {
        defaultTexture = let {
            val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)
            for (x in 0..64) {
                for (y in 0..64) {
                    pixmap.drawPixel(x, y, Color.rgba8888(Color.BLACK))
                }
            }
            val texture = Texture(pixmap)
            pixmap.dispose()
            texture
        }
        defaultPack = TextureAtlas()
        defaultPackRegion = TextureAtlas.AtlasRegion(defaultTexture, 0, 0, 64, 64)
    }

    fun unloadAssets() {
        assetManager.clear()
    }

    fun getTexture(file: FileHandle): Texture = tryLoad(file) ?: defaultTexture

    fun getPack(file: FileHandle): TextureAtlas = tryLoad(file) ?: defaultPack

    fun getPackRegion(file: FileHandle, region: String): TextureAtlas.AtlasRegion = tryLoad<TextureAtlas>(file)?.findRegion(region)
            ?: defaultPackRegion

    fun getSound(file: FileHandle): Sound? = tryLoad(file)

    fun getI18NBundle(file: FileHandle): I18NBundle? = tryLoad(file)

    private inline fun <reified T : Any> tryLoad(file: FileHandle): T? {
        try {
            return if (assetManager.isLoaded(file.path()))
                assetManager.get(file.path())
            else {
                if (file.exists() || T::class == I18NBundle::class)
                    assetManager.loadOnDemand<T>(file).asset
                else {
                    Log.warn { "Ressource non trouvée : ${file.path()}" }
                    null
                }
            }
        } catch (e: GdxRuntimeException) {
            Log.error(e) { "Erreur lors du chargement d'une ressource ! Chemin : ${file.path()}" }
        }
        Log.warn { "Ressource non trouvée : ${file.path()}" }
        return null
    }

    override fun dispose() {
        assetManager.dispose()
    }
}