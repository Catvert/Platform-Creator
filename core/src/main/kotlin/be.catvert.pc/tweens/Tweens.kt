package be.catvert.pc.tweens

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Point
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui
import imgui.functionalProgramming
import kotlin.math.roundToInt
import kotlin.reflect.KClass

enum class Tweens(val tween: KClass<out Tween>) {
    Empty(EmptyTween::class),
    Move(MoveTween::class),
    AlphaAtlas(AlphaAtlasTween::class)
}

abstract class Tween(var duration: Float = 1f, @JsonIgnore val interpolation: Interpolation) : CustomEditorImpl {
    var nextTween: Tween? = null
    var endAction: Action = EmptyAction()

    var useTweenState: Boolean = false

    private var elapsedTime: Float = 0f

    protected var progress: Float = 0f
        private set

    open fun init(gameObject: GameObject) {
        elapsedTime = 0f
    }

    fun update(gameObject: GameObject): Boolean {
        elapsedTime += Gdx.graphics.deltaTime
        progress = Math.min(1f, elapsedTime / duration)

        perform(gameObject)
        return elapsedTime >= duration
    }

    abstract fun perform(gameObject: GameObject)

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            functionalProgramming.withItemWidth(100f) {
                sliderFloat("duration", ::duration, 0f, 10f, "%.1f")
            }
            ImguiHelper.action("end action", ::endAction, gameObject, level, editorSceneUI)

            checkbox("tween state", ::useTweenState)
        }
    }
}

class EmptyTween : Tween(0f, Interpolation.linear) {
    override fun perform(gameObject: GameObject) {}
}

class MoveTween(duration: Float = 0f, var moveX: Int = 0, var moveY: Int = 0) : Tween(duration, Interpolation.linear) {
    private var initialPosX = 0f
    private var initialPosY = 0f

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialPosX = gameObject.position().x.toFloat()
        initialPosY = gameObject.position().y.toFloat()
    }

    override fun perform(gameObject: GameObject) {
        gameObject.box.position = Point(interpolation.apply(initialPosX, initialPosX + moveX, progress).roundToInt(),
                interpolation.apply(initialPosY, initialPosY + moveY, progress).roundToInt())
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)

        functionalProgramming.withItemWidth(100f) {
            ImGui.inputInt("move x", ::moveX)
            ImGui.inputInt("move y", ::moveY)
        }
    }
}

class AlphaAtlasTween(duration: Float = 0f, var targetAlpha: Float = 0f) : Tween(duration, Interpolation.linear) {
    private var initialAlpha = 0f

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialAlpha = gameObject.getCurrentState().getComponent<AtlasComponent>()?.alpha ?: 0f
    }

    override fun perform(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.apply {
            alpha = interpolation.apply(initialAlpha, targetAlpha, progress)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)
        functionalProgramming.withItemWidth(100f) {
            ImGui.sliderFloat("target alpha", ::targetAlpha, 0f, 1f, "%.1f")
        }
    }
}