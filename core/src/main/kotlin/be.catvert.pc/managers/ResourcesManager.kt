package be.catvert.pc.managers

import be.catvert.pc.Log
import be.catvert.pc.utility.loadOnDemand
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.I18NBundle


/**
 * Permet de gérer les ressources graphiques et sonores.
 */
object ResourcesManager : Disposable {
    val manager = AssetManager()
    val defaultTexture: Texture
    val defaultPack: TextureAtlas
    val defaultPackRegion: TextureAtlas.AtlasRegion

    init {
        // Création d'une texture noir de 64x64 pixels
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

        manager.setLoader(TextureAtlas::class.java, PackLoader())
    }

    /**
     * Permet d'obtenir une texture
     */
    fun getTexture(file: FileHandle): Texture = tryLoad(file)
            ?: defaultTexture

    /**
     * Permet d'obtenir un pack
     */
    fun getPack(file: FileHandle): TextureAtlas = tryLoad(file)
            ?: defaultPack

    /**
     * Permet d'obtenir un son
     */
    fun getSound(file: FileHandle): Sound? = tryLoad(file)

    fun unloadAsset(file: FileHandle) {
        manager.unload(file.path())
    }

    /**
     * Permet d'essayer le chargement d'une ressource si elle n'est déjà pas chargée, dans le cas contraire, elle renvoi la-dite ressource chargée
     */
    private inline fun <reified T : Any> tryLoad(file: FileHandle): T? {
        try {
            return if (manager.isLoaded(file.path()))
                manager.get(file.path())
            else {
                if (file.exists() || T::class == I18NBundle::class) {
                    manager.loadOnDemand<T>(file).asset
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


    override fun dispose() {
        manager.dispose()
    }
}

private class PackLoader : TextureAtlasLoader(LocalFileHandleResolver()) {
    override fun load(assetManager: AssetManager?, fileName: String?, file: FileHandle?, parameter: TextureAtlasParameter?): TextureAtlas {
        val pack = super.load(assetManager, fileName, file, parameter)
        fixPackBleeding(pack)
        return pack
    }

    /**
     * Fix par grimrader22 : https://stackoverflow.com/questions/27391911/white-vertical-lines-and-jittery-horizontal-lines-in-tile-map-movement
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
}