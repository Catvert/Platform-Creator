package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import imgui.ImGui
import imgui.functionalProgramming

@Description("Permet d'effectuer une action quand une entité précise est au-dessus d'une autre entité (couche)")
class SensorComponent(var sensors: ArrayList<SensorData>) : Component(), Updeatable, UIImpl {
    constructor(vararg sensors: SensorData) : this(arrayListOf(*sensors))
    @JsonCreator private constructor() : this(arrayListOf())

    private var level: Level? = null

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    abstract class SensorData(@UI(description = "Action appelée quand l'entité ciblée passe devant/derrière(overlaps) cette entité") var sensorIn: Action = EmptyAction(), @UI(description = "Action appelée quand l'entité ciblée quitte cette entité") var sensorOut: Action = EmptyAction()) {
        protected val sensorOverlaps: MutableSet<Entity> = mutableSetOf()

        abstract fun checkSensorOverlaps(entity: Entity, level: Level)
    }

    class TagSensorData(@UI(customType = CustomType.TAG_STRING) var target: EntityTag = Tags.Player.tag, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut) {
        override fun checkSensorOverlaps(entity: Entity, level: Level) {
            val checkedEntities = mutableSetOf<Entity>()

            level.getAllEntitiesInCells(entity.box).filter { it !== entity && it.tag == target && entity.box.overlaps(it.box) }.forEach {
                if (!sensorOverlaps.contains(it)) {
                    sensorIn(entity)
                    sensorOverlaps += it
                }

                checkedEntities += it
            }

            sensorOverlaps.filter { !checkedEntities.contains(it) }.forEach {
                sensorOut(entity)
                sensorOverlaps.remove(it)
            }
        }

        override fun toString(): String = ""
    }

    class EntitySensorData(var target: Entity?, sensorIn: Action = EmptyAction(), sensorOut: Action = EmptyAction()) : SensorData(sensorIn, sensorOut), UIImpl {
        override fun checkSensorOverlaps(entity: Entity, level: Level) {
            if (target != null) {
                val target = target!!

                if (entity.box.overlaps(target.box)) {
                    if (!sensorOverlaps.contains(target)) {
                        sensorIn(entity)
                        sensorOverlaps += target
                    }
                } else if (sensorOverlaps.contains(target)) {
                    sensorOut(entity)
                    sensorOverlaps.remove(target)
                }
            }
        }

        override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            ImGuiHelper.entity(::target, level, editorSceneUI, "cible")
        }
    }

    override fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        super.onStateActive(entity, state, container)

        level = container.cast()
    }

    override fun update() {
        if (level != null) {
            sensors.forEach {
                it.checkSensorOverlaps(entity, level!!)
            }
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("sensors", sensors, { "sensor" }, { EntitySensorData(null) }, {
            val typeIndex = intArrayOf(if (it.obj is EntitySensorData) 0 else 1)
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                if (ImGui.combo("type de cible", typeIndex, listOf("Entité", "Tag"))) {
                    it.obj = when (typeIndex[0]) {
                        0 -> EntitySensorData(null)
                        else -> TagSensorData(Tags.Player.tag)
                    }
                }
            }
            ImGui.separator()
            ImGuiHelper.insertUIFields(it.obj, entity, level, editorSceneUI)
        })
    }
}