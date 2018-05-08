package be.catvert.pc.tweens

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.components.logics.LifeComponent
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.*
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.fasterxml.jackson.annotation.JsonTypeInfo
import imgui.ImGui
import imgui.functionalProgramming
import net.dermetfan.gdx.math.InterpolationUtils
import kotlin.math.roundToInt
import kotlin.reflect.KClass

/**
 * Énumération des différents tweens disponibles.
 */
enum class Tweens(val tween: KClass<out Tween>) {
    Empty(EmptyTween::class),
    Move(MoveTween::class),
    AlphaTexture(AlphaTextureTween::class),
    RepeatAction(RepeatActionTween::class),
    Resize(ResizeTween::class),
    DisableComponent(DisableComponentTween::class)
}

/**
 * Un tween permet de rendre un déplacement, un changement de vitesse, ... sur une entité "fluide" grâce à l'interpolation.
 * L'interpolation est une opération mathématique, qui grâce à une fonction, permet de construire une courbe.
 * Par exemple, un changement de vitesse de 10 à 5, grâce à une fonction linéaire, passera d'abord par 9,8,7.. avant d'arriver à la vitesse de 5.
 */
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_ARRAY, use = JsonTypeInfo.Id.MINIMAL_CLASS)
abstract class Tween(var duration: Float = 1f, var interpolationName: String, var authorizeTweenState: Boolean = true) : UIImpl, PostDeserialization {
    var nextTween: Tween? = null
    var endAction: Action = EmptyAction()

    var useTweenState: Boolean = false

    protected var interpolation: Interpolation = InterpolationUtils.get(interpolationName)

    private var elapsedTime: Float = 0f

    protected var progress: Float = 0f
        private set

    open fun init(entity: Entity) {
        elapsedTime = 0f
    }

    fun update(entity: Entity): Boolean {
        elapsedTime += Utility.getDeltaTime()
        progress = Math.min(1f, elapsedTime / duration)

        perform(entity)
        return elapsedTime >= duration
    }

    abstract fun perform(entity: Entity)

    private var currentInterpolationIndex = 0
    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            text("type : ${ReflectionUtility.simpleNameOf(this@Tween).removeSuffix("Tween")}")
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                inputFloat("durée", ::duration, 0.1f, 0f, 1)
            }
            ImGuiHelper.action("action de fin", ::endAction, entity, level, editorUI)

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                if (combo("interpolation", ::currentInterpolationIndex, interpolations.map { it.component1() })) {
                    val (name, interp) = interpolations.entries.elementAt(currentInterpolationIndex)
                    interpolationName = name
                    interpolation = interp
                }
            }
            if (authorizeTweenState) {
                checkbox("état tween", ::useTweenState)

                if (isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text("Permet d'utiliser un état spécial dans lequel seulement la texture est gardée")
                    }
                }
            }
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

/**
 * Un tween vide.
 */
class EmptyTween : Tween(0f, linearInterpolation) {
    override fun perform(entity: Entity) {}
}

/**
 * Un tween réalisant un déplacement x et/ou y sur une entité.
 */
class MoveTween(duration: Float = 0f, var moveX: Int = 0, var moveY: Int = 0) : Tween(duration, linearInterpolation) {
    private var initialPosX = 0f
    private var initialPosY = 0f

    override fun init(entity: Entity) {
        super.init(entity)

        initialPosX = entity.position().x
        initialPosY = entity.position().y
    }

    override fun perform(entity: Entity) {
        entity.box.position = Point(interpolation.apply(initialPosX, initialPosX + moveX, progress),
                interpolation.apply(initialPosY, initialPosY + moveY, progress))
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        super.insertUI(label, entity, level, editorUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.inputInt("déplacement x", ::moveX)
            ImGui.inputInt("déplacement y", ::moveY)
        }
    }
}

/**
 * Un tween permettant de modifier le canal alpha sur une entité.
 */
class AlphaTextureTween(duration: Float = 0f, var targetAlpha: Float = 0f) : Tween(duration, linearInterpolation) {
    private var initialAlpha = 0f

    override fun init(entity: Entity) {
        super.init(entity)

        initialAlpha = entity.getCurrentState().getComponent<TextureComponent>()?.alpha ?: 0f
    }

    override fun perform(entity: Entity) {
        entity.getCurrentState().getComponent<TextureComponent>()?.apply {
            alpha = interpolation.apply(initialAlpha, targetAlpha, progress)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        super.insertUI(label, entity, level, editorUI)
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.sliderFloat("alpha ciblé", ::targetAlpha, 0f, 1f, "%.1f")
        }
    }
}

/**
 * Un tween permettant de répéter le déclenchement d'une action sur une entité sur une durée précise.
 */
class RepeatActionTween(duration: Float = 0f, var repeat: Int = 1, var repeatAction: Action = EmptyAction()) : Tween(duration, linearInterpolation) {
    override fun perform(entity: Entity) {
        val progress = Math.round(interpolation.apply(progress) * 100)

        if (entity.container != null && progress != 0 && progress % Math.round(100f / repeat) == 0) {
            repeatAction(entity, entity.container!!)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        super.insertUI(label, entity, level, editorUI)
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.sliderInt("répétition", ::repeat, 1, 100)
        }
        ImGuiHelper.action("action répétée", ::repeatAction, entity, level, editorUI)
    }
}

/**
 * Un tween permettant de redimensionné une entité.
 */
class ResizeTween(duration: Float = 0f, var newWidth: Int = 1, var newHeight: Int = 1) : Tween(duration, linearInterpolation) {
    private var initialWidth = 0
    private var initialHeight = 0

    override fun init(entity: Entity) {
        super.init(entity)

        initialWidth = entity.size().width
        initialHeight = entity.size().height
    }

    override fun perform(entity: Entity) {
        entity.box.size = Size(interpolation.apply(initialWidth.toFloat(), newWidth.toFloat(), progress).roundToInt(),
                interpolation.apply(initialHeight.toFloat(), newHeight.toFloat(), progress).roundToInt())
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        super.insertUI(label, entity, level, editorUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.inputInt("largeur ciblée", ::newWidth, 1, Constants.maxEntitySize)
            ImGui.inputInt("hauteur ciblée", ::newHeight, 1, Constants.maxEntitySize)
        }
    }
}

/**
 * Un tween permettant de désactivé un component d'une entité.
 */
class DisableComponentTween(var disableComponent: Class<out Component> = LifeComponent::class.java, duration: Float = 0f) : Tween(duration, linearInterpolation, false) {
    private var component: Component? = null

    override fun init(entity: Entity) {
        super.init(entity)

        component = entity.getCurrentState().getComponents().filterIsInstance(disableComponent).firstOrNull()
        component?.active = false
    }

    override fun perform(entity: Entity) {
        if (progress == 1f) {
            component?.active = true
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        super.insertUI(label, entity, level, editorUI)

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            val components = entity.getCurrentState().getComponents()
            val index = intArrayOf(components.indexOfFirst { disableComponent.isInstance(it) })
            if (ImGui.combo("component désactivé", index, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })) {
                disableComponent = components.elementAt(index[0]).javaClass
            }
        }
    }
}