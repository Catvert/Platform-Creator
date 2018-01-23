package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Timer
import be.catvert.pc.utility.Updeatable
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

class TimerComponent(interval: Float, @ExposeEditor var timerAction: Action) : Component(), Updeatable, CustomEditorImpl {
    @JsonCreator private constructor() : this(1f, EmptyAction())

    var interval = interval
        set(value) {
            field = value
            timer.interval = value
        }

    private val timer = Timer(interval).apply {
        onIncrement.register {
            timerAction(gameObject)
        }
    }

    override fun update() {
        timer.update()
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(100f) {
            ImGui.sliderFloat("interval", ::interval, 0f, 10f)
        }
    }

    override fun toString() = "interval : $interval"
}