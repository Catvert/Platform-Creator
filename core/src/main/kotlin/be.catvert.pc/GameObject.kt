package be.catvert.pc

import be.catvert.pc.actions.Action
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import imgui.ImGui
import imgui.functionalProgramming


/**
 * Classe représentant un objet en jeu
 * @param box Représente le box de l'objet dans l'espace (position + taille)
 * @param container Item dans lequel l'objet va être implémenté
 */
@JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.IntSequenceGenerator::class)
class GameObject(@ExposeEditor(customType = CustomType.TAG_STRING) var tag: GameObjectTag,
                 @ExposeEditor var name: String,
                 @ExposeEditor var box: Rect,
                 defaultState: GameObjectState = GameObjectState("default"),
                 container: GameObjectContainer? = null,
                 vararg otherStates: GameObjectState = arrayOf()) : Updeatable, Renderable, ResourceLoader, CustomEditorImpl, CustomEditorTextImpl {

    @ExposeEditor(min = -100f, max = 100f)
    var layer: Int = 0
        set(value) {
            if (value in Constants.minLayerIndex until Constants.maxLayerIndex) field = value
        }

    @ExposeEditor(description = "Action appelée quand le game object à une position y < 0", customName = "on out of map")
    var onOutOfMapAction: Action = RemoveGOAction()

    @JsonProperty("states")
    private val states: MutableSet<GameObjectState> = mutableSetOf(defaultState, *otherStates)

    @JsonProperty("currentState")
    private var currentState: Int = 0
        set(value) {
            if (value in states.indices) {
                field = value
            }
        }

    var initialState: Int = 0
        set(value) {
            if (value in states.indices) {
                field = value
            }
        }

    @JsonIgnore
    val onRemoveFromParent = Signal<GameObject>()

    @JsonIgnore
    var gridCells: MutableList<GridCell> = mutableListOf()

    @JsonIgnore
    var container: GameObjectContainer? = container
        set(value) {
            field = value
            if (value != null) {
                states.forEach { it.onAddToContainer(this) }
                setState(initialState, true)
            }
        }


    @JsonIgnore
    fun getCurrentState() = states.elementAt(currentState)

    @JsonIgnore
    fun getCurrentStateIndex() = currentState

    @JsonIgnore
    fun getStates() = states.toSet()

    fun position() = box.position
    fun size() = box.size

    fun removeFromParent() {
        onRemoveFromParent(this)
        container?.removeGameObject(this)
    }

    fun addState(state: GameObjectState): Int {
        if (container != null)
            state.onAddToContainer(this)
        states.add(state)
        return states.size - 1
    }

    fun addState(name: String, initState: GameObjectState.() -> Unit): Int {
        val state = GameObjectState(name)
        state.initState()
        return addState(state)
    }

    fun removeState(stateIndex: Int) {
        states.remove(states.elementAt(stateIndex))
    }

    fun setState(stateIndex: Int, triggerStartAction: Boolean) {
        if (stateIndex in states.indices) {
            currentState = stateIndex
            getCurrentState().toggleActive(container!!, triggerStartAction)
        }
    }

    fun getStateOrDefault(stateIndex: Int) = getStates().elementAtOrNull(stateIndex) ?: getStates().elementAt(0)

    override fun update() {
        getCurrentState().update()
    }

    override fun render(batch: Batch) {
        getCurrentState().render(batch)
    }

    override fun loadResources() {
        states.forEach { it.loadResources() }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                if (combo("State initial", ::initialState, getStates().map { it.name }))
                    setState(initialState, false)
            }
        }
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.ORANGE, name)
        ImGuiHelper.textPropertyColored(Color.CORAL, "state actuel :", getCurrentState().name)
    }
}