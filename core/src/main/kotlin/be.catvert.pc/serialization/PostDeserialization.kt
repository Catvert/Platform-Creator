package be.catvert.pc.serialization

/**
 * Permet à un objet d'avoir une méthode spécialement appelée après la dé-sérialisation de celui-ci.
 * @see SerializationFactory
 */
interface PostDeserialization {
    fun onPostDeserialization()
}