package be.catvert.pc

import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.onChange
import ktx.collections.toGdxArray
import java.util.*


/**
 * Classe représentant un objet en jeu
 * @param id L'id du gameObject
 * @param rectangle Représente le rectangle de l'objet dans l'espace (position + taille)
 * @param container Container dans lequel l'objet va être implémenté
 * @param prefab Le prefab utilisé pour créer l'objet
 */
class GameObject(@ExposeEditor var tag: Tag,
                 id: UUID = UUID.randomUUID(),
                 var rectangle: Rect = Rect(size = Size(1, 1)),
                 container: GameObjectContainer? = null,
                 initDefaultState: GameObjectState.() -> Unit = {}) : Updeatable, Renderable, CustomEditorImpl {

    enum class Tag {
        Sprite, PhysicsSprite, Player, Enemy
    }

    var id: UUID = id

    @ExposeEditor
    var keepActive: Boolean = false

    @ExposeEditor
    var unique: Boolean = false

    @ExposeEditor(minInt = -100, maxInt = 100)
    var layer: Int = 0
        set(value) {
            if (value in Constants.minLayerIndex until Constants.maxLayerIndex) field = value
        }

    @JsonProperty("states") private val states: MutableSet<GameObjectState> = mutableSetOf(GameObjectState("default").apply(initDefaultState))

    var currentState: Int = 0
        set(value) {
            if (value in 0 until states.size - 1) {
                getCurrentState().onEndStateAction(this)
                field = value
                getCurrentState().onStartStateAction(this)
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

    override fun insertChangeProperties(table: VisTable, editorScene: EditorScene) {
        table.add(VisLabel("Pos X : "))
        val posXIntModel = IntSpinnerModel(position().x, 0, editorScene.level.matrixRect.width - size().width)
        table.add(Spinner("", posXIntModel).apply {
            onChange {
                rectangle.x = posXIntModel.value
            }
        })

        table.row()

        table.add(VisLabel("Pos Y : "))
        val posYIntModel = IntSpinnerModel(position().y, 0, editorScene.level.matrixRect.height - size().height)
        table.add(Spinner("", posYIntModel).apply {
            onChange {
                rectangle.y = posYIntModel.value
            }
        })

        table.row()

        table.add(VisLabel("Largeur : "))
        val widthIntModel = IntSpinnerModel(size().width, 1, Constants.maxGameObjectSize)
        table.add(Spinner("", widthIntModel).apply {
            onChange {
                rectangle.width = widthIntModel.value
            }
        })

        table.row()

        table.add(VisLabel("Hauteur : "))
        val heightIntModel = IntSpinnerModel(size().height, 1, Constants.maxGameObjectSize)
        table.add(Spinner("", heightIntModel).apply {
            onChange {
                rectangle.height = heightIntModel.value
            }
        })

        table.row()

        table.add(VisLabel("State initial : "))
        table.add(VisSelectBox<String>().apply {
            fun generateItems() {
                this.items = getStates().map { it.name }.toGdxArray()
            }
            generateItems()

            this.selectedIndex = currentState

            onChange {
                currentState = selectedIndex
            }
        })
    }
}