package be.catvert.pc.utility


/**
 * Classe de donnée représentant un point dans l'espace
 */
data class Point(val x: Int = 0, val y: Int = 0) {
    override fun toString(): String = "{ $x ; $y }"
}