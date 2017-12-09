package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Timer
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

class TimerComponent(interval: Float, @ExposeEditor var timerAction: Action) : LogicsComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(1f, EmptyAction())

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

    private lateinit var gameObject: GameObject

    override fun update(gameObject: GameObject) {
        this.gameObject = gameObject

        timer.update()
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        functionalProgramming.withItemWidth(100f) {
            ImGui.sliderFloat("interval", ::interval, 0f, 10f)
        }
    }
}