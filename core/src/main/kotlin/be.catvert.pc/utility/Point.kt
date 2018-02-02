package be.catvert.pc.utility


/**
 * Classe de donnée représentant un point dans l'espace
 */
data class Point(val x: Float = 0f, val y: Float = 0f) {
    constructor(point: Point) : this(point.x, point.y)

    override fun toString(): String = "{ $x ; $y }"
}