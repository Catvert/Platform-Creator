package be.catvert.pc.utility


/**
 * Classe de donnée représentant une taille
 */
data class Size(val width: Int = 0, val height: Int = 0) {
    override fun toString(): String = "{ $width ; $height }"
}