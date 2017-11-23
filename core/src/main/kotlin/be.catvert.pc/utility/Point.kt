package be.catvert.pc.utility

/**
 * Classe de donnée représentant un point dans l'espace
 */
data class Point(val x: Float = 0f, val y: Float = 0f) {
    override fun toString(): String = "{ $x ; $y }"
}