package be.catvert.mtrktx

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Rectangle

/**
 * Created by arno on 06/06/17.
 */

inline fun <reified T> matrix2d(height: Int, width: Int, init: (Int, Int) -> Array<T>) = Array<Array<T>>(height, { row -> init(row, width) })

/*class ListMatrix<T>(val width: Int, val height: Int) {
    private val mutArray: MutableList<MutableList<T>> = mutableListOf()


    init {
        for(x in 0..width) {
            val x = mutableListOf<MutableList<T>>()

            for(y in 0..height) {
                x.add
            }
            mutArray.add(x)


        }
    }

    operator fun get(x: Int, y: Int): MutableList<T> {
        return mutArray[x][y]
    }

    fun iterateMatrix(f: (Int, Int, MutableList<T>) -> Unit) {
        for(x in 0..width) {
            for(y in 0..height) {
                f(x, y, mutArray[x][y])
            }
        }
    }

    fun iterateEachElementMatrix(f: (Int, Int, T) -> Unit) {
        iterateMatrix({ x, y, list ->
            for(v in list)
                f(x, y, v)
        })
    }

    fun iterateAt(x: Int, y: Int, f:(Int, Int, T) -> Unit) {
        for(rect in mutArray[x][y])
            f(x, y, rect)
    }
}*/