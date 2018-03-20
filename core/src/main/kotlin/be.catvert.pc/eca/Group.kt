package be.catvert.pc.eca

import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.utility.Point

class EntityDescriptor(val prefab: Prefab, val deltaMainEntity: Point)

class Group(var name: String, val idRefs: ArrayList<EntityID>, var mainEntity: Prefab, vararg entities: EntityDescriptor) {
    val entities = arrayListOf(*entities)

    fun create(mainEntityPos: Point, container: EntityContainer, addContainerToEntities: Boolean): List<Entity> {
        val createEntities = mutableListOf<Entity>()

        idRefs.forEach {
            it.ID = container.getNextID() + it.ID
        }

        createEntities += mainEntity.create(mainEntityPos, if (addContainerToEntities) container else null)

        entities.forEach { it ->
            createEntities += it.prefab.create(Point(it.deltaMainEntity.x - mainEntityPos.x, it.deltaMainEntity.y - mainEntityPos.y), if (addContainerToEntities) container else null)
        }

        return createEntities
    }
}