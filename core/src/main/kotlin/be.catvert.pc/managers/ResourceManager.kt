package be.catvert.pc.managers

import be.catvert.pc.Log
import be.catvert.pc.utility.loadOnDemand
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

    fun getTexture(file: FileHandle): Texture = tryLoad(file)
            ?: defaultTexture

    fun getPack(file: FileHandle): TextureAtlas = tryLoad(file)
            ?: defaultPack

    fun getPackRegion(file: FileHandle, region: String): TextureAtlas.AtlasRegion = tryLoad<TextureAtlas>(file)?.findRegion(region)
            ?: defaultPackRegion

    fun getSound(file: FileHandle): Sound? = tryLoad(file)

    private inline fun <reified T : Any> tryLoad(file: FileHandle): T? {
        try {
            return if (assetManager.isLoaded(file.path()))
                assetManager.get(file.path())
            else {
                if (file.exists() || T::class == I18NBundle::class) {
                    val res = assetManager.loadOnDemand<T>(file).asset
                    if (res is TextureAtlas)
                        fixPackBleeding(res)
                    res
                } else {
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

    /**
     * Fix by grimrader22 : https://stackoverflow.com/questions/27391911/white-vertical-lines-and-jittery-horizontal-lines-in-tile-map-movement
     */
    private fun fixPackBleeding(pack: TextureAtlas) {
        pack.regions.forEach { region ->
            val fix = 0.01f

            val x = region.regionX.toFloat()
            val y = region.regionY.toFloat()
            val width = region.regionWidth.toFloat()
            val height = region.regionHeight.toFloat()
            val invTexWidth = 1f / region.texture.width
            val invTexHeight = 1f / region.texture.height
            region.setRegion((x + fix) * invTexWidth, (y + fix) * invTexHeight, (x + width - fix) * invTexWidth, (y + height - fix) * invTexHeight)
        }
    }

    override fun dispose() {
        assetManager.dispose()
    }
}