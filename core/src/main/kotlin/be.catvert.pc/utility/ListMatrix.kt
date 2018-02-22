package be.catvert.pc.utility

/**
 * Classe permettant de créer une matrix(tableau 2D) en kotlin, source : https://stackoverflow.com/questions/41941102/instantiating-generic-array-in-kotlin
 * @param height La hauteur de la matrix
 * @param width La largeur de la matrix
 * @param init Méthode pour initialiser la matrix
 */
inline fun <reified T> matrix2d(height: Int, width: Int, init: (Int, Int) -> MutableList<T>) = MutableList(height, { row -> init(row, width) })

/**
 * Cette classe permet de représenter une cellule contenant des entités
 * @see GameObjectMatrixContainer
 * @property x La position x de la cellule
 * @property y La position y de la cellule
 */
data class GridCell(val x: Int, val y: Int)