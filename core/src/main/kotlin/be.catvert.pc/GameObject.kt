package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import imgui.ImGui
import java.util.*


/**
 * Classe représentant un objet en jeu
 * @param id L'id du gameObject
 * @param rectangle Représente le rectangle de l'objet dans l'espace (position + taille)
 * @param container Container dans lequel l'objet va être implémenté
 */
class GameObject(@ExposeEditor var tag: Tag,
                 id: UUID = UUID.randomUUID(),
                 @ExposeEditor var rectangle: Rect = Rect(size = Size(1, 1)),
                 container: GameObjectContainer? = null,
                 initDefaultState: GameObjectState.() -> Unit = {}) : Updeatable, Renderable, CustomEditorImpl {


    enum class Tag {
        Sprite, PhysicsSprite, Player, Enemy
    }

    var id: UUID = id

    @ExposeEditor
    var keepActive: Boolean = false

    @ExposeEditor(minInt = -100, maxInt = 100)
    var layer: Int = 0
        set(value) {
            if (value in Constants.minLayerIndex until Constants.maxLayerIndex) field = value
        }

    @JsonProperty("states") private val states: MutableSet<GameObjectState> = mutableSetOf(GameObjectState("default").apply(initDefaultState))

    var currentState: Int = 0
        set(value) {
            if (value in 0 until states.size) {
                field = value
            }
        }

    @JsonIgnore val onRemoveFromParent = Signal<GameObject>()

    @JsonIgnore
    var active: Boolean = true
        set(value) {
            if (!keepActive)
                field = value
        }

    @JsonIgnore
    var gridCells: MutableList<GridCell> = mutableListOf()

    @JsonIgnore
    var container: GameObjectContainer? = container
        set(value) {
            field = value
            if (field != null) {
                states.forEach { it.onGOAddToContainer(this) }
                getCurrentState().onStartStateAction(this)
            }
        }

    @JsonIgnore
    fun getCurrentState() = states.elementAt(currentState)

    @JsonIgnore
    fun getStates() = states.toSet()

    fun position() = rectangle.position
    fun size() = rectangle.size

    fun removeFromParent() {
        onRemoveFromParent(this)
        container?.removeGameObject(this)
    }

    fun addState(state: GameObjectState) {
        if(container != null)
            state.onGOAddToContainer(this)
        states.add(state)
    }

    fun addState(name: String, initState: GameObjectState.() -> Unit) {
        val state = GameObjectState(name)
        state.initState()
        addState(state)
    }

    fun removeState(state: GameObjectState) {
        states.remove(state)
    }

    fun removeState(stateIndex: Int) {
        states.remove(states.elementAt(stateIndex))
    }

    override fun update() {
        if (active) {
            getCurrentState().update()
        }
    }

    override fun render(batch: Batch) {
        if (active) {
            getCurrentState().render(batch)
        }
    }

    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            combo("State initial", this@GameObject::currentState, getStates().map { it.name })
        }
    }
}