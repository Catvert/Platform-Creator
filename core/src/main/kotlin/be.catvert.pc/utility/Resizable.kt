package be.catvert.pc.utility

/**
 * Interface permettant de spécifier qu'un objet peut-être redimensionné
 */
interface Resizable {
    fun resize(size: Size)
}