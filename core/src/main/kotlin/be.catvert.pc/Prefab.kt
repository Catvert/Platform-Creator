package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point

/**
 * Représente un gameObject "préfait", permettant "d'enregistrer" l'état d'un gameObject avec ses components et de réutiliser ce gameObject à plusieurs endroits dans le niveau en copiant celui-ci.
 * Exemple de prefab : ennemis, joueurs, blocs spéciaux..
 */
class Prefab(val name: String, val prefabGO: GameObject) {
    fun create(position: Point, container: GameObjectContainer? = null) = SerializationFactory.copy(prefabGO).apply {
                this.box.position = position
                this.name = this@Prefab.name
                container?.addGameObject(this)
            }

    override fun toString(): String = this.name
}