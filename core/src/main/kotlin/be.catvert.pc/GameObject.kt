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
 * Représente une entité dans le jeu
 * Cette entité est représentée dans l'espace grâce à sa box
 *
 * Une entité a un état de base et un conteneur
 * @see GameObjectState
 * @see GameObjectContainer
 * @see Level
 *
 * @param tag Représente la catégorie à laquelle cette entité appartient
 * @param name Le nom de l'entité
 * @param box La représentation spatiale de l'entité
 * @param defaultState L'état par défaut
 * @param container Le conteneur "contenant" cette entité
 * @param otherStates Les autres états de l'entité
 */
@JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.IntSequenceGenerator::class)
class GameObject(@ExposeEditor(customType = CustomType.TAG_STRING) var tag: GameObjectTag,
                 @ExposeEditor var name: String,
                 @ExposeEditor var box: Rect,
                 defaultState: GameObjectState = GameObjectState("default"),
                 container: GameObjectContainer? = null,
                 vararg otherStates: GameObjectState = arrayOf()) : Updeatable, Renderable, ResourceLoader, CustomEditorImpl, CustomEditorTextImpl {

    /**
     * Représente la "couche" à laquelle cette entité va être affichée
     */
    @ExposeEditor(min = -100f, max = 100f)
    var layer: Int = 0
        set(value) {
            if (value in Constants.minLayerIndex until Constants.maxLayerIndex) field = value
        }

    /**
     * Action spéciale appelée quand le game object sort de la carte verticalement
     */
    @ExposeEditor(description = "Action appelée quand le game object à une position y < 0", customName = "dehors de carte")
    var onOutOfMapAction: Action = RemoveGOAction()

    /**
     * Les différents états de l'entité
     */
    @JsonProperty("states")
    private val states: MutableSet<GameObjectState> = mutableSetOf(defaultState, *otherStates)

    /**
     * État actuel de l'entité
     */
    @JsonProperty("currentState")
    private var currentState: Int = 0
        set(value) {
            if (value in states.indices) {
                field = value
            }
        }

    /**
     * État initial de l'entité
     * @see states -> index
     */
    var initialState: Int = 0
        set(value) {
            if (value in states.indices) {
                field = value
            }
        }

    /**
     * Signal appelé quand cette entité est supprimé de son conteneur
     * @see GameObjectContainer.update
     */
    @JsonIgnore
    val onRemoveFromParent = Signal<GameObject>()

    /**
     * Cellules dans lesquels cette entité est présente
     */
    @JsonIgnore
    var gridCells: MutableList<GridCell> = mutableListOf()

    /**
     * Conteneur dans lequel l'entité est présente
     * @see GameObjectContainer
     * @see Level
     */
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

    /**
     * Supprime cette entité de son conteneur
     */
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
            getCurrentState().disabled()
            currentState = stateIndex

            if (container != null)
                getCurrentState().active(container!!, triggerStartAction)
        }
    }

    /**
     * Permet d'obtenir un état spécifique de l'entité ou dans le cas échéant son état par défaut
     */
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
                if (combo("state initial", ::initialState, getStates().map { it.name }))
                    setState(initialState, false)
            }
        }
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.ORANGE, name)
        ImGuiHelper.textPropertyColored(Color.CORAL, "state actuel :", getCurrentState().name)
    }
}