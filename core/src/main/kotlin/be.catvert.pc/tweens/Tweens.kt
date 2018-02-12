package be.catvert.pc.tweens

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.LifeComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.utility.*
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.reflect.ClassReflection
import imgui.ImGui
import imgui.functionalProgramming
import net.dermetfan.gdx.math.InterpolationUtils
import kotlin.math.roundToInt
import kotlin.reflect.KClass

enum class Tweens(val tween: KClass<out Tween>) {
    Empty(EmptyTween::class),
    Move(MoveTween::class),
    AlphaAtlas(AlphaAtlasTween::class),
    RepeatAction(RepeatActionTween::class),
    Resize(ResizeTween::class),
    DisableComponent(DisableComponentTween::class)
}

abstract class Tween(var duration: Float = 1f, var interpolationName: String, var authorizeTweenState: Boolean = true) : CustomEditorImpl, PostDeserialization {
    var nextTween: Tween? = null
    var endAction: Action = EmptyAction()

    var useTweenState: Boolean = false

    protected var interpolation: Interpolation = InterpolationUtils.get(interpolationName)

    private var elapsedTime: Float = 0f

    protected var progress: Float = 0f
        private set

    open fun init(gameObject: GameObject) {
        elapsedTime = 0f
    }

    fun update(gameObject: GameObject): Boolean {
        elapsedTime += Utility.getDeltaTime()
        progress = Math.min(1f, elapsedTime / duration)

        perform(gameObject)
        return elapsedTime >= duration
    }

    abstract fun perform(gameObject: GameObject)

    private var currentInterpolationIndex = 0
    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            text("type : ${ReflectionUtility.simpleNameOf(this@Tween).removeSuffix("Tween")}")
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                inputFloat("duration", ::duration, 0.1f, 0f, 1)
            }
            ImGuiHelper.action("end action", ::endAction, gameObject, level, editorSceneUI)

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                if (combo("interpolation", ::currentInterpolationIndex, interpolations.map { it.component1() })) {
                    val (name, interp) = interpolations.entries.elementAt(currentInterpolationIndex)
                    interpolationName = name
                    interpolation = interp
                }
            }
            if (authorizeTweenState)
                checkbox("tween state", ::useTweenState)
        }
    }

    override fun onPostDeserialization() {
        interpolation = InterpolationUtils.get(interpolationName)
        currentInterpolationIndex = interpolations.entries.indexOfFirst { it.component2() === interpolation }
    }

    companion object {
        private val interpolations: Map<String, Interpolation> = ClassReflection.getFields(Interpolation::class.java).filter { ClassReflection.isAssignableFrom(Interpolation::class.java, it.declaringClass) }.associate { it.name to it.get(null) as Interpolation }
        const val linearInterpolation = "linear"
    }
}

class EmptyTween : Tween(0f, linearInterpolation) {
    override fun perform(gameObject: GameObject) {}
}

class MoveTween(duration: Float = 0f, var moveX: Int = 0, var moveY: Int = 0) : Tween(duration, linearInterpolation) {
    private var initialPosX = 0f
    private var initialPosY = 0f

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialPosX = gameObject.position().x
        initialPosY = gameObject.position().y
    }

    override fun perform(gameObject: GameObject) {
        gameObject.box.position = Point(interpolation.apply(initialPosX, initialPosX + moveX, progress),
                interpolation.apply(initialPosY, initialPosY + moveY, progress))
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.inputInt("move x", ::moveX)
            ImGui.inputInt("move y", ::moveY)
        }
    }
}

class AlphaAtlasTween(duration: Float = 0f, var targetAlpha: Float = 0f) : Tween(duration, linearInterpolation) {
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
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.sliderFloat("target alpha", ::targetAlpha, 0f, 1f, "%.1f")
        }
    }
}

class RepeatActionTween(duration: Float = 0f, var repeat: Int = 1, var repeatAction: Action = EmptyAction()) : Tween(duration, linearInterpolation) {
    override fun perform(gameObject: GameObject) {
        val progress = Math.round(interpolation.apply(progress) * 100)

        if (progress != 0 && progress % Math.round(100f / repeat) == 0) {
            repeatAction(gameObject)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.sliderInt("repeat", ::repeat, 1, 100)
        }
        ImGuiHelper.action("repeat action", ::repeatAction, gameObject, level, editorSceneUI)
    }
}

class ResizeTween(duration: Float = 0f, var newWidth: Int = 1, var newHeight: Int = 1) : Tween(duration, linearInterpolation) {
    private var initialWidth = 0
    private var initialHeight = 0

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialWidth = gameObject.size().width
        initialHeight = gameObject.size().height
    }

    override fun perform(gameObject: GameObject) {
        gameObject.box.size = Size(interpolation.apply(initialWidth.toFloat(), newWidth.toFloat(), progress).roundToInt(),
                interpolation.apply(initialHeight.toFloat(), newHeight.toFloat(), progress).roundToInt())
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.inputInt("new width", ::newWidth, 1, Constants.maxGameObjectSize)
            ImGui.inputInt("new height", ::newHeight, 1, Constants.maxGameObjectSize)
        }
    }
}

class DisableComponentTween(var disableComponent: Class<out Component> = LifeComponent::class.java, duration: Float = 0f) : Tween(duration, linearInterpolation, false) {
    private var component: Component? = null

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        component = gameObject.getCurrentState().getComponents().filterIsInstance(disableComponent).firstOrNull()
        component?.active = false
    }

    override fun perform(gameObject: GameObject) {
        if (progress == 1f) {
            component?.active = true
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        super.insertImgui(label, gameObject, level, editorSceneUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            val components = gameObject.getCurrentState().getComponents()
            val index = intArrayOf(components.indexOfFirst { disableComponent.isInstance(it) })
            if (ImGui.combo("disable component", index, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })) {
                disableComponent = components.elementAt(index[0]).javaClass
            }
        }
    }
}