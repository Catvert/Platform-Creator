package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Classe représentant un objet en jeu
 * @param id L'id du gameObject
 * @param rectangle Représente le rectangle de l'objet dans l'espace (position + taille)
 * @param container Container dans lequel l'objet va être implémenté
 * @param prefab Le prefab utilisé pour créer l'objet
 */
class GameObject(id: UUID,
                 components: MutableSet<Component> = mutableSetOf(),
                 var rectangle: Rect = Rect(),
                 var tag: Tag,
                 container: GameObjectContainer? = null,
                 @JsonIgnore val prefab: Prefab? = null) : Updeatable, Renderable {

    enum class Tag {
        Sprite, PhysicsSprite, Player, Enemy
    }

    var id: UUID = id
        private set

    @JsonIgnore var active: Boolean = true
        set(value) {
            if(!keepActive)
                field = value
        }

    var keepActive: Boolean = false

    @JsonIgnore
    var gridCells: MutableList<GridCell> = mutableListOf()

    @JsonProperty("comps")
    private val components: MutableSet<Component> = mutableSetOf()

    @JsonIgnore
    private val renderComponents = mutableSetOf<RenderableComponent>()
    @JsonIgnore
    private val updateComponents = mutableSetOf<UpdeatableComponent>()

    @JsonIgnore var container: GameObjectContainer? = container
        set(value) {
            field = value
            if(field != null)
                components.forEach { it.onGOAddToContainer(this) }
        }

    @JsonIgnore var isRemoving = false
        set(value) {
            field = value
            if(value) {
                onRemoveAction?.perform(this)
                container = null
            }
        }

    var onRemoveAction: Action? = null

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun position() = rectangle.position
    fun size() = rectangle.size

    init {
        components.forEach {
            addComponent(it)
        }
    }

    fun addComponent(component: Component) {
        components.add(component)
        component.linkGameObject(this)

        if (component is RenderableComponent)
            renderComponents.add(component)
        else if (component is UpdeatableComponent)
            updateComponents.add(component)
    }

    inline fun <reified T : Component> getComponent(index: Int = 0): T? {
        val filtered = getComponents().filter { it is T }
        return if (filtered.size > index && index > -1) filtered[index] as T else null
    }

    inline fun <reified T : Component> hasComponent(index: Int = 0): Boolean {
        return getComponent<T>(index) != null
    }

    fun removeFromParent() {
        isRemoving = true
    }

    override fun update() {
        if(active)
            updateComponents.forEach { if (it.active) it.update() }
    }

    override fun render(batch: Batch) {
        if(active)
            renderComponents.forEach { if (it.active) it.render(batch) }
    }

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}