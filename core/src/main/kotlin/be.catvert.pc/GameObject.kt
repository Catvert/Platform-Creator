package be.catvert.pc

import aurelienribon.tweenengine.TweenAccessor
import be.catvert.pc.actions.EmptyAction
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

    @ExposeEditor(customName = "action out of map") var onOutOfMapAction = EmptyAction()

    @JsonProperty("states") private val states: MutableSet<GameObjectState> = mutableSetOf(GameObjectState("default").apply(initDefaultState))

    @JsonProperty("currentState") private var currentState: Int = 0

    var initialState: Int = 0
        set(value) {
            if(value in 0 until states.size) {
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
            if (value != null) {
                states.forEach { it.onAddToContainer(this, value) }
                setState(initialState)
            }
        }

    @JsonIgnore
    fun getCurrentState() = states.elementAt(currentState)

    @JsonIgnore
    fun getCurrentStateIndex() = currentState

    @JsonIgnore
    fun getStates() = states.toSet()

    fun position() = rectangle.position
    fun size() = rectangle.size

    fun removeFromParent() {
        getCurrentState().inactive()
        onRemoveFromParent(this)
        container?.removeGameObject(this)
    }

    fun addState(state: GameObjectState): Int {
        if(container != null)
            state.onAddToContainer(this, container!!)
        states.add(state)
        return states.size - 1
    }

    fun addState(name: String, initState: GameObjectState.() -> Unit): Int {
        val state = GameObjectState(name)
        state.initState()
        return addState(state)
    }

    fun removeState(state: GameObjectState) {
        states.remove(state)
    }

    fun removeState(stateIndex: Int) {
        states.remove(states.elementAt(stateIndex))
    }

    fun setState(stateIndex: Int, callActionsEvent: Boolean = true) {
        if (stateIndex in 0 until states.size) {
            if(stateIndex != currentState) {
                getCurrentState().inactive()
            }
            currentState = stateIndex

            getCurrentState().active()
        }
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
            combo("State initial", this@GameObject::initialState, getStates().map { it.name })
        }
    }
}

class GameObjectTweenAccessor : TweenAccessor<GameObject> {
    enum class GameObjectTween(val tweenType: Int) {
        NOTHING(-1),
        POS_X(0),
        POS_Y(1),
        POS_XY(2),
        SIZE_X(3),
        SIZE_Y(4),
        SIZE_XY(5);

        companion object {
            fun fromType(tweenType: Int) = values().firstOrNull { it.tweenType == tweenType }
        }
    }

    override fun setValues(gameObject: GameObject, tweenType: Int, newValues: FloatArray) {
        when(GameObjectTween.fromType(tweenType)) {
            GameObjectTween.NOTHING -> {}
            GameObjectTween.POS_X -> { gameObject.rectangle.x = newValues[0] }
            GameObjectTween.POS_Y -> { gameObject.rectangle.y = newValues[0] }
            GameObjectTween.POS_XY -> { gameObject.rectangle.position = Point(newValues[0], newValues[1]) }
            GameObjectTween.SIZE_X -> { gameObject.rectangle.width = newValues[0].toInt() }
            GameObjectTween.SIZE_Y -> { gameObject.rectangle.height = newValues[0].toInt() }
            GameObjectTween.SIZE_XY -> { gameObject.rectangle.size = Size(newValues[0].toInt(), newValues[1].toInt())}
            else -> Log.error { "Tween inconnu : $tweenType" }
        }
    }

    override fun getValues(gameObject: GameObject, tweenType: Int, returnValues: FloatArray): Int {
        when(GameObjectTween.fromType(tweenType)) {
            GameObjectTween.NOTHING -> { return -1 }
            GameObjectTween.POS_X -> { returnValues[0] = gameObject.rectangle.x; return 1 }
            GameObjectTween.POS_Y -> { returnValues[0] = gameObject.rectangle.y; return 1 }
            GameObjectTween.POS_XY -> { returnValues[0] = gameObject.rectangle.x; returnValues[1] = gameObject.rectangle.y; return 2 }
            GameObjectTween.SIZE_X -> { returnValues[0] = gameObject.rectangle.width.toFloat(); return 1 }
            GameObjectTween.SIZE_Y -> { returnValues[0] = gameObject.rectangle.height.toFloat(); return 1 }
            GameObjectTween.SIZE_XY -> { returnValues[0] = gameObject.rectangle.width.toFloat(); returnValues[1] = gameObject.rectangle.height.toFloat(); return 2 }
            else -> Log.error { "Tween inconnu : $tweenType" }
        }

        return -1
    }
}