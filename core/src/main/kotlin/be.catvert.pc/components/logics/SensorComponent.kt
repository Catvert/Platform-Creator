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
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.functionalProgramming

class SensorComponent(var sensors: ArrayList<SensorData>) : Component(), Updeatable, CustomEditorImpl {
    constructor(vararg sensors: SensorData) : this(arrayListOf(*sensors))
    @JsonCreator private constructor() : this(arrayListOf())

    private var level: Level? = null

    data class SensorData(var target: GameObjectTag = Tags.Player.tag, var sensorIn: Action = EmptyAction(), var sensorOut: Action = EmptyAction()) : CustomEditorImpl {
        val sensorOverlaps: MutableSet<GameObject> = mutableSetOf()

        fun checkSensorOverlaps(gameObject: GameObject, level: Level) {
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
            ImguiHelper.gameObjectTag(::target, level, "sensor target")
            functionalProgramming.withId("in action") {
                ImguiHelper.action("in action", ::sensorIn, gameObject, level, editorSceneUI)
            }
            functionalProgramming.withId("out action") {
                ImguiHelper.action("out action", ::sensorOut, gameObject, level, editorSceneUI)
            }
        }

        override fun toString(): String = ""
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
        ImguiHelper.addImguiWidgetsArray("sensors", sensors, { "sensor" }, { SensorData() }, gameObject, level, editorSceneUI)
    }
}