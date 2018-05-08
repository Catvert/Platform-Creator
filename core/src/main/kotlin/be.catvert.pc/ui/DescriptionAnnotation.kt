package be.catvert.pc.ui

/**
 * Permet de décrire une action ou un component et afficher cette description dans l'éditeur de niveau.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val description: String)
