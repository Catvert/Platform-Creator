package be.catvert.pc.eca.containers

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityID
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

abstract class EntityContainer : Renderable, Updeatable, PostDeserialization {
    @JsonProperty("objects")
    protected val entities: MutableSet<Entity> = mutableSetOf()

    private val entitiesByID = hashMapOf<Int, Entity>()

    protected open val processEntities: Set<Entity>
        get() = entities

    @JsonIgnore
    var allowRendering = true
    @JsonIgnore
    var allowUpdating = true

    @JsonProperty("lastID")
    private var _lastGeneratedID = -1

    private val removeEntities = mutableSetOf<Entity>()

    @JsonIgnore
    fun getEntitiesData() = entities.toSet()

    open fun findEntitiesByTag(tag: EntityTag): List<Entity> = entities.filter { it.tag == tag }.toList()

    fun findEntityByID(id: Int) = if (id == EntityID.INVALID_ID) null else entitiesByID[id]

    open fun removeEntity(entity: Entity) {
        removeEntities.add(entity)
    }

    open fun addEntity(entity: Entity): Entity {
        entities.add(entity)
        entity.container = this
        entitiesByID[entity.entityID.ID] = entity

        return entity
    }

    fun copyEntity(entity: Entity): Entity {
        val copyEntity = SerializationFactory.copy(entity)
        copyEntity.container = this
        copyEntity.entityID.ID = generateID()
        return copyEntity
    }

    fun entitiesInitialStartActions() {
        entities.forEach {
            if (it.getCurrentState().isActive())
                it.getCurrentState().startAction(it, this)
        }
    }

    @JsonIgnore
    fun getNextID() = _lastGeneratedID + 1

    fun generateID() = ++_lastGeneratedID

    protected open fun onRemoveEntity(entity: Entity) {}

    override fun render(batch: Batch) {
        if (allowRendering)
            processEntities.sortedBy { it.layer }.forEach { it.render(batch) }
    }

    override fun update() {
        processEntities.forEach {
            if (allowUpdating)
                it.update()

            if (it.position().y < 0) {
                it.onOutOfMapAction(it, this)
            }
        }

        removeEntities()
    }

    private fun removeEntities() {
        if (removeEntities.isNotEmpty()) {
            removeEntities.forEach {
                onRemoveEntity(it)
                entities.remove(it)
                entitiesByID.remove(it.entityID.ID)
                it.container = null
            }
            removeEntities.clear()
        }
    }

    override fun onPostDeserialization() {
        entities.forEach {
            it.container = this
            entitiesByID[it.entityID.ID] = it
            it.onRemoveFromParent.register {
                removeEntities.add(it)
            }
        }
    }

    operator fun plusAssign(entity: Entity) {
        addEntity(entity)
    }
}
