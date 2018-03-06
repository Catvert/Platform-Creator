package be.catvert.pc.eca.containers

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.ResourceLoader
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

abstract class EntityContainer : Renderable, Updeatable, PostDeserialization, ResourceLoader {
    @JsonProperty("objects")
    protected val entities: MutableSet<Entity> = mutableSetOf()

    protected open val processEntities: Set<Entity>
        get() = entities

    @JsonIgnore
    var allowRenderingGO = true
    @JsonIgnore
    var allowUpdatingGO = true

    private val removeEntities = mutableSetOf<Entity>()


    @JsonIgnore
    fun getEntitiesData() = entities.toSet()

    open fun findEntitiesByTag(tag: EntityTag): List<Entity> = entities.filter { it.tag == tag }.toList()

    open fun removeEntity(entity: Entity) {
        removeEntities.add(entity)

    }

    open fun addEntity(entity: Entity): Entity {
        entity.loadResources()

        entities.add(entity)
        entity.container = this

        return entity
    }

    protected open fun onRemoveEntity(entity: Entity) {}

    override fun loadResources() {
        entities.forEach {
            it.loadResources()
        }
    }

    override fun render(batch: Batch) {
        if (allowRenderingGO)
            processEntities.sortedBy { it.layer }.forEach { it.render(batch) }
    }

    override fun update() {
        processEntities.forEach {
            if (allowUpdatingGO)
                it.update()

            if (it.position().y < 0) {
                it.onOutOfMapAction(it)
            }
        }

        removeEntities()
    }

    private fun removeEntities() {
        if (removeEntities.isNotEmpty()) {
            removeEntities.forEach {
                onRemoveEntity(it)
                entities.remove(it)
                it.container = null
            }
            removeEntities.clear()
        }
    }

    override fun onPostDeserialization() {
        entities.forEach {
            it.container = this
            it.onRemoveFromParent.register {
                removeEntities.add(it)
            }
        }

        loadResources()
    }

    operator fun plusAssign(entity: Entity) {
        addEntity(entity)
    }
}
