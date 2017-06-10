package be.catvert.mtrktx

/**
 * Created by arno on 06/06/17.
 */

inline fun <reified T> matrix2d(height: Int, width: Int, init: (Int, Int) -> Array<T>) = Array<Array<T>>(height, { row -> init(row, width) })