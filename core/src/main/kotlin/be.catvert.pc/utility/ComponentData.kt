package be.catvert.pc.utility

import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray
import net.dermetfan.gdx.utils.ArrayUtils

class ComponentData<T : Any>(index: Int, var items: Array<T>) {
    var index = if(index in items.indices) index else -1

    fun get() = items.elementAtOrNull(index)
}

inline fun<reified T: Any> componentDataOf(index: Int = 0, vararg items: T) = ComponentData(index, arrayOf(*items))