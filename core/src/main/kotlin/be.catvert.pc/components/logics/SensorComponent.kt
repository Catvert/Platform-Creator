package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.GameObjectTag
import be.catvert.pc.Tags
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import imgui.functionalProgramming

@Description("Permet d'effectuer une action quand un game object précis est au-dessus d'une autre game object")
class SensorComponent(var sensors: ArrayList<SensorData>) : Component(), Updeatable, CustomEditorImpl {
    constructor(vararg sensors: SensorData) : this(arrayListOf(*sensors))
    @JsonCreator private constructor() : this(arrayListOf())

    private var level: Level? = null

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    abstract class SensorData(var sensorIn: Action = EmptyAction(), var sensorOut: Action = EmptyAction()) : CustomEditorImpl {
        protected val sensorOverlaps: MutableSet<GameObject> = mutableSetOf()

        abstract fun checkSensorOverlaps(gameObject: GameObject, level: Level)

        override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            functionalProgramming.withId("in action") {
                ImGuiHelper.action("in action", ::sensorIn, gameObject, level, editorSceneUI)
            }
            functionalProgramming.withId("out action") {
                ImGuiHelper.action("out action", ::sensorOut, gameObject, level, editorSceneUI)
            }
        }
    }

    class TagSensorData(var target: GameObjectTag = Tags.Player.tag, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut) {
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

        override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            ImGuiHelper.gameObjectTag(::target, level, "sensor target")

            super.insertImgui(label, gameObject, level, editorSceneUI)
        }

        override fun toString(): String = ""
    }

    class GameObjectSensorData(var target: GameObject?, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut) {
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
            super.insertImgui(label, gameObject, level, editorSceneUI)
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
        ImGuiHelper.addImguiWidgetsArray("sensors", sensors, { "sensor" }, { GameObjectSensorData(null) }, gameObject, level, editorSceneUI)
    }
}