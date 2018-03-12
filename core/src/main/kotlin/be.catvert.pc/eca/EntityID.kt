package be.catvert.pc.eca

import be.catvert.pc.eca.containers.EntityContainer
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore

class EntityID(var ID: Int = INVALID_ID) {
    @JsonCreator private constructor(): this(INVALID_ID)

    @JsonIgnore
    fun entity(entityContainer: EntityContainer) = entityContainer.findEntityByID(ID)

    override fun hashCode(): Int = ID.hashCode()

    companion object {
        const val INVALID_ID = -1
    }
}