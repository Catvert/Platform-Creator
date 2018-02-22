package be.catvert.pc.utility

/**
 * Interface permettant de spécifier que l'objet à besoin d'initialiser des ressources
 */
interface ResourceLoader {
    fun loadResources()
}