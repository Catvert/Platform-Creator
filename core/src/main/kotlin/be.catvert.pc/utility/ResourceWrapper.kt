package be.catvert.pc.utility

import be.catvert.pc.Log
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.serialization.PostDeserialization
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.utils.GdxRuntimeException
import ktx.assets.DelayedAsset

inline fun <reified T> resourceWrapperOf(path: FileWrapper, resourceClass: Class<T> = T::class.java) = ResourceWrapper(path, resourceClass)

class ResourceWrapper<T>(path: FileWrapper, val resourceClass: Class<T>) : PostDeserialization {
    private var asset: DelayedAsset<T>? = null

    var path: FileWrapper = path
        set(value) {
            field = value
            loadAsset()
        }

    init {
        loadAsset()
    }

    operator fun invoke(): T? = getAsset()
    operator fun invoke(defaultValue: T): T = getAsset() ?: defaultValue

    private fun getAsset(): T? {
        try {
            return asset?.asset
        } catch (e: GdxRuntimeException) {
            Log.error(e) { "Impossible de charger la ressource $path !" }
        }
        return null
    }

    private fun loadAsset() {
        asset = DelayedAsset(ResourcesManager.assetManager, AssetDescriptor(path.get(), resourceClass))
    }

    override fun onPostDeserialization() {
        loadAsset()
    }

    override fun toString() = path.toString()
}