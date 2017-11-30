package be.catvert.pc

import aurelienribon.tweenengine.TweenAccessor
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import imgui.ImGui
import imgui.functionalProgramming
import java.util.*


/**
 * Classe représentant un objet en jeu
 * @param id L'id du gameObject
 * @param box Représente le box de l'objet dans l'espace (position + taille)
 * @param container Item dans lequel l'objet va être implémenté
 */
class GameObject(@ExposeEditor var tag: Tag,
                 @ExposeEditor var box: Rect = Rect(size = Size(1, 1)),
                 container: GameObjectContainer? = null,
                 initDefaultState: GameObjectState.() -> Unit = {}) : Updeatable, Renderable, CustomEditorImpl {

    enum class Tag {
        Sprite, PhysicsSprite, Player, Enemy, Special
    }

    var id: UUID = UUID.randomUUID()

    @ExposeEditor
    var keepActive: Boolean = false

    @ExposeEditor(minInt = -100, maxInt = 100)
    var layer: Int = 0
        set(value) {
            if (value in Constants.minLayerIndex until Constants.maxLayerIndex) field = value
        }

    @ExposeEditor(customName = "action out of map")
    var onOutOfMapAction: Action = RemoveGOAction()

    @JsonProperty("states") private val states: MutableSet<GameObjectState> = mutableSetOf(GameObjectState("default").apply(initDefaultState))

    @JsonProperty("currentState") private var currentState: Int = 0

    var initialState: Int = 0
        set(value) {
            if (value in 0 until states.size) {
                field = value
            }
        }

    @JsonIgnore
    val onRemoveFromParent = Signal<GameObject>()

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

    fun position() = box.position
    fun size() = box.size

    fun removeFromParent() {
        getCurrentState().inactive()
        onRemoveFromParent(this)
        container?.removeGameObject(this)
    }

    fun addState(state: GameObjectState): Int {
        if (container != null)
            state.onAddToContainer(this, container!!)
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

    fun setState(stateIndex: Int) {
        if (stateIndex in 0 until states.size) {
            if (stateIndex != currentState) {
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

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                combo("State initial", ::initialState, getStates().map { it.name })
            }
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
        when (GameObjectTween.fromType(tweenType)) {
            GameObjectTween.NOTHING -> {
            }
            GameObjectTween.POS_X -> {
                gameObject.box.x = Math.round(newValues[0])
            }
            GameObjectTween.POS_Y -> {
                gameObject.box.y = Math.round(newValues[0])
            }
            GameObjectTween.POS_XY -> {
                gameObject.box.position = Point(Math.round(newValues[0]), Math.round(newValues[1]))
            }
            GameObjectTween.SIZE_X -> {
                gameObject.box.width = Math.round(newValues[0])
            }
            GameObjectTween.SIZE_Y -> {
                gameObject.box.height = Math.round(newValues[0])
            }
            GameObjectTween.SIZE_XY -> {
                gameObject.box.size = Size(Math.round(newValues[0]), Math.round(newValues[1]))
            }
            else -> Log.error { "Tween inconnu : $tweenType" }
        }
    }

    override fun getValues(gameObject: GameObject, tweenType: Int, returnValues: FloatArray): Int {
        when (GameObjectTween.fromType(tweenType)) {
            GameObjectTween.NOTHING -> {
                return -1
            }
            GameObjectTween.POS_X -> {
                returnValues[0] = gameObject.box.x.toFloat(); return 1
            }
            GameObjectTween.POS_Y -> {
                returnValues[0] = gameObject.box.y.toFloat(); return 1
            }
            GameObjectTween.POS_XY -> {
                returnValues[0] = gameObject.box.x.toFloat(); returnValues[1] = gameObject.box.y.toFloat(); return 2
            }
            GameObjectTween.SIZE_X -> {
                returnValues[0] = gameObject.box.width.toFloat(); return 1
            }
            GameObjectTween.SIZE_Y -> {
                returnValues[0] = gameObject.box.height.toFloat(); return 1
            }
            GameObjectTween.SIZE_XY -> {
                returnValues[0] = gameObject.box.width.toFloat(); returnValues[1] = gameObject.box.height.toFloat(); return 2
            }
            else -> Log.error { "Tween inconnu : $tweenType" }
        }

        return -1
    }
}