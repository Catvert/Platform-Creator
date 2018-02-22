package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators

/**
 * Représente une entité "préfaite", permettant "d'enregistrer" l'état d'une entité avec ses components et ses propriétés et de réutiliser cette entité à plusieurs endroits dans le niveau en copiant celle-ci.
 * Exemple de prefab : ennemis, joueurs, blocs spéciaux..
 */
@JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.IntSequenceGenerator::class)
class Prefab(val name: String, val prefabGO: GameObject) {
    /**
     * Permet de créer une nouvelle entité en copiant l'entité de base définie dans ce préfab
     */
    fun create(position: Point, container: GameObjectContainer? = null) = SerializationFactory.copy(prefabGO).apply {
        this.box.position = position
        this.name = this@Prefab.name
        container?.addGameObject(this)
    }

    override fun toString(): String = this.name
}