package be.catvert.pc.utility

/**
 * Représente une taille avec une largeur et une hauteur
 */
data class Size(val width: Int, val height: Int) {
    constructor(size: Int) : this(size, size)
    constructor() : this(0, 0)

    override fun toString(): String = "{ $width ; $height }"
}