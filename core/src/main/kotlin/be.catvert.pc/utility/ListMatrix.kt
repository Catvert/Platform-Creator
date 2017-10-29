package be.catvert.pc.utility

/**
 * Classe permettant de créer une matrix en kotlin, source : https://stackoverflow.com/questions/41941102/instantiating-generic-array-in-kotlin
 * @param height La hauteur de la matrix
 * @param width La larguer de la matrix
 * @param init Méthode pour initialiser la matrix
 */
inline fun <reified T> matrix2d(height: Int, width: Int, init: (Int, Int) -> Array<T>) = Array(height, { row -> init(row, width) })

/**
 * Cette classe permet de représenter une cellule contenant des entités avec une position x et y
 * @property x La position x de la cellule
 * @property y La position y de la cellule
 */
data class GridCell(val x: Int, val y: Int)