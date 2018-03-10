package be.catvert.pc.eca

import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators

/**
 * Représente une entité "préfaite", permettant "d'enregistrer" l'état d'une entité avec ses components et ses propriétés et de réutiliser cette entité à plusieurs endroits dans le niveau en copiant celle-ci.
 * Exemple de prefab : ennemis, joueurs, blocs spéciaux..
 */
@JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.IntSequenceGenerator::class)
class Prefab(val name: String, val prefabEntity: Entity) {
    /**
     * Permet de créer une nouvelle entité en copiant l'entité de base définie dans ce préfab
     */
    fun create(position: Point, container: EntityContainer? = null) = SerializationFactory.copy(prefabEntity).apply {
        this.box.position = position
        this.name = this@Prefab.name
        container?.addEntity(this)
    }

    override fun toString(): String = this.name
}