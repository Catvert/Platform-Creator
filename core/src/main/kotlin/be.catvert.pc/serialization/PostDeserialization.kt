package be.catvert.pc.serialization

/**
 * Permet à un objet d'avoir une méthode appelée après la désérialisation de celui-ci
 * @see SerializationFactory
 */
interface PostDeserialization {
    fun onPostDeserialization()
}