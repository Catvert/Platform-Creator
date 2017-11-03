package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.serialization.PostDeserialization
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
                 var rectangle: Rect = Rect(),
                 var tag: Tag,
                 @JsonProperty("states") val states: MutableSet<GameObjectState> = mutableSetOf(),
                 container: GameObjectContainer? = null,
                 @JsonIgnore val prefab: Prefab? = null) : Updeatable, Renderable {

    enum class Tag {
        Sprite, PhysicsSprite, Player, Enemy
    }

    var id: UUID = id
        private set

    var keepActive: Boolean = false

    var unique: Boolean = false

    var layer: Int = 0

    var fixedSize = false

    var currentState: Int = 0

    @JsonIgnore var active: Boolean = true
        set(value) {
            if(!keepActive)
                field = value
        }

    @JsonIgnore
    var gridCells: MutableList<GridCell> = mutableListOf()

    @JsonIgnore var container: GameObjectContainer? = container
        set(value) {
            field = value
            if(field != null) {
                states.forEach { it.linkGameObject(this) }
            }
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

    fun position() = rectangle.position
    fun size() = rectangle.size

    fun removeFromParent() {
        isRemoving = true
    }

    fun addState(state: GameObjectState) {
        states.add(state)
    }

    fun state(name: String, initState: GameObjectState.() -> Unit) {
        val state = GameObjectState(name, this)
        state.initState()
        addState(state)
    }

    @JsonIgnore fun getCurrentState() = states.elementAt(currentState)

    override fun update() {
        if(active) {
            getCurrentState().update()
        }
    }

    override fun render(batch: Batch) {
        if(active) {
            getCurrentState().render(batch)
        }
    }
}