package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.GameObjectTag
import be.catvert.pc.Tags
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.TagAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import imgui.ImGui
import imgui.functionalProgramming

@Description("Permet d'effectuer une action quand un game object précis est au-dessus d'une autre game object")
class SensorComponent(var sensors: ArrayList<SensorData>) : Component(), Updeatable, CustomEditorImpl {
    constructor(vararg sensors: SensorData) : this(arrayListOf(*sensors))
    @JsonCreator private constructor() : this(arrayListOf())

    private var level: Level? = null

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    abstract class SensorData(@ExposeEditor(description = "Action appelée quand le game object ciblé passe devant/derrière(overlaps) ce game object") var sensorIn: Action = EmptyAction(), @ExposeEditor(description = "Action appelée quand le game object ciblé quitte ce game object") var sensorOut: Action = EmptyAction()) {
        protected val sensorOverlaps: MutableSet<GameObject> = mutableSetOf()

        abstract fun checkSensorOverlaps(gameObject: GameObject, level: Level)
    }

    class TagSensorData(@ExposeEditor(customType = CustomType.TAG_STRING) var target: GameObjectTag = Tags.Player.tag, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut) {
        override fun checkSensorOverlaps(gameObject: GameObject, level: Level) {
            val checkedGameObject = mutableSetOf<GameObject>()

            level.getAllGameObjectsInCells(gameObject.box).filter { it !== gameObject && it.tag == target && gameObject.box.overlaps(it.box) }.forEach {
                if (!sensorOverlaps.contains(it)) {
                    sensorIn(gameObject)
                    sensorOverlaps += it
                }

                checkedGameObject += it
            }

            sensorOverlaps.filter { !checkedGameObject.contains(it) }.forEach {
                sensorOut(gameObject)
                sensorOverlaps.remove(it)
            }
        }

        override fun toString(): String = ""
    }

    class GameObjectSensorData(var target: GameObject?, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut), CustomEditorImpl {
        override fun checkSensorOverlaps(gameObject: GameObject, level: Level) {
            if (target != null) {
                val target = target!!

                if (gameObject.box.overlaps(target.box)) {
                    if (!sensorOverlaps.contains(target)) {
                        sensorIn(gameObject)
                        sensorOverlaps += target
                    }
                } else if (sensorOverlaps.contains(target)) {
                    sensorOut(gameObject)
                    sensorOverlaps.remove(target)
                }
            }
        }

        override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            ImGuiHelper.gameObject(::target, editorSceneUI, "target")
        }
    }

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        level = container.cast()
    }

    override fun update() {
        if (level != null) {
            sensors.forEach {
                it.checkSensorOverlaps(gameObject, level!!)
            }
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("sensors", sensors, { "sensor" }, { GameObjectSensorData(null) }, {
            val typeIndex = intArrayOf(if(it.obj is GameObjectSensorData) 0 else 1)
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                if (ImGui.combo("target type", typeIndex, listOf("GameObject", "Tag"))) {
                    it.obj = when (typeIndex[0]) {
                        0 -> GameObjectSensorData(null)
                        else -> TagSensorData(Tags.Player.tag)
                    }
                }
            }
            ImGui.separator()
            ImGuiHelper.insertImguiExposeEditorFields(it.obj, gameObject, level, editorSceneUI)
        })
    }
}