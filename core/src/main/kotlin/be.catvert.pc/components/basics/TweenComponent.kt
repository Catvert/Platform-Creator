package be.catvert.pc.components.basics

import aurelienribon.tweenengine.Tween
import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.PCGame
import be.catvert.pc.actions.Action
import be.catvert.pc.components.Component
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.TweenFactory
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Component permettant d'appliquer un tween précis sur un gameObject
 * Un tween représente l'application d'une interpolation sur une caractéristiques d'un gameObject
 * @see GameObjectTweenAccessor
 */
class TweenComponent(var tweens: ArrayList<TweenData>) : Component(), CustomEditorImpl {
    constructor(vararg tweens: TweenData) : this(arrayListOf(*tweens))
    @JsonCreator private constructor() : this(arrayListOf())

    /**
     * Représente un tween à appliquer sur une caractéristiques d'un gameObject (par exemple la taille du gameObject)
     * @param name Nom du tween
     * @param type Type de tween à appliquer sur le gameObject
     * @param target Valeur à atteindre d'une caractéristique du gameObject
     * @param duration Temps requis pour atteindre la valeur du target
     * @param keepComponents Components gardés lors de l'application du tween
     * @param endAction Action appelée à la fin du tween
     */
    class TweenData(var name: String, var type: GameObjectTweenAccessor.GameObjectTween, var target: FloatArray, var duration: Float, var keepComponents: ArrayList<Class<out Component>>, var endAction: Action, var setLastStateOnFinish: Boolean = true) : CustomEditorImpl {
        private var started = false

        /**
         * Permet d'appliquer le tween sur un gameObject
         */
        fun start(gameObject: GameObject) {
            if (started)
                return

            val tweenState = gameObject.addState("tween-state") {
                gameObject.getCurrentState().getComponents().forEach {
                            if (keepComponents.contains(it.javaClass))
                                addComponent(it)
                        }
            }

            val lastState = gameObject.getCurrentStateIndex()
            gameObject.setState(tweenState)

            Tween.to(gameObject, type.tweenType, duration)
                    .target(*target)
                    .setCallback { _, _ ->
                        started = false
                        if (setLastStateOnFinish) {
                            gameObject.setState(lastState)
                        }
                        endAction(gameObject)
                    }.start(PCGame.tweenManager)
            started = true
        }


        override fun insertImgui(label: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                if (treeNode(label)) {
                    val index = intArrayOf(GameObjectTweenAccessor.GameObjectTween.values().indexOf(type))

                    functionalProgramming.withItemWidth(100f) {
                        if (combo("type", index, GameObjectTweenAccessor.GameObjectTween.values().map { it.name }))
                            type = GameObjectTweenAccessor.GameObjectTween.values()[index[0]]
                        inputFloatN("target", target, target.size, 1, 0)
                        sliderFloat("interval", ::duration, 0f, 10f, "%.1f")
                    }
                    ImguiHelper.addImguiWidgetsArray("keep components", keepComponents, { it.simpleName }, { PCGame.componentsClasses[0].java }, {
                        val index = intArrayOf(PCGame.componentsClasses.indexOf(it.obj.kotlin))
                        functionalProgramming.withItemWidth(150f) {
                            if (combo("", index, PCGame.componentsClasses.map { it.simpleName ?: "Nom inconnu" })) {
                                keepComponents[keepComponents.indexOf(it.obj)] = PCGame.componentsClasses[index[0]].java
                                return@addImguiWidgetsArray true
                            }
                        }
                        false
                    })

                    ImguiHelper.action("end action", ::endAction, gameObject, level)
                    ImGui.checkbox("last state on finish", ::setLastStateOnFinish)

                    treePop()
                }
            }
        }
    }

    /**
     * Permet d'appliquer un tween précis sur un gameObject
     */
    fun startTween(gameObject: GameObject, tweenIndex: Int) {
        if (tweenIndex in tweens.indices)
            tweens[tweenIndex].start(gameObject)
    }

    private val addTweenTitle = "Ajouter un tween"
    private var currentTweenIndex = 0
    override fun insertImgui(label: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            ImguiHelper.addImguiWidgetsArray("tweens", tweens, { it.name }, { TweenFactory.EmptyTween() }, gameObject, level) {
                if (button("Ajouter depuis..", Vec2(-1, 0)))
                    openPopup(addTweenTitle)

                if (beginPopup(addTweenTitle)) {
                    functionalProgramming.withItemWidth(100f) {
                        combo("tweens", ::currentTweenIndex, TweenFactory.values().map { it.name })
                    }
                    if (button("Ajouter", Vec2(-1, 0))) {
                        tweens.add(TweenFactory.values()[currentTweenIndex]())
                        closeCurrentPopup()
                    }

                    endPopup()
                }
            }
        }
    }
}