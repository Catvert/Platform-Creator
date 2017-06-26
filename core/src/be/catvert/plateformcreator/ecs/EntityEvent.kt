package be.catvert.plateformcreator.ecs

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.files.FileHandle

/**
 * Created by Catvert on 12/06/17.
 */

/**
 * Cette classe permet d'ajouter une méthode d'ajout et de suppression d'une entité spécifique
 * Elle est spécifiquement utilisée pour les entités ayant le besoin d'ajouter ou supprimer une entité
 */
class EntityEvent(val levelFile: FileHandle) {
    var onEntityRemoved: ((entity: Entity) -> Unit)? = null
    var onEntityAdded: ((entity: Entity) -> Unit)? = null
}
