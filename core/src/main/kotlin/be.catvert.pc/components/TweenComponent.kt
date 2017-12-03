package be.catvert.pc.components

import aurelienribon.tweenengine.Tween
import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.PCGame
import be.catvert.pc.actions.Action
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.TweenFactory
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming

class TweenComponent(var tweens: ArrayList<TweenData>) : BasicComponent(), CustomEditorImpl {
    constructor(vararg tweens: TweenData) : this(arrayListOf(*tweens))
    @JsonCreator private constructor() : this(arrayListOf())

    class TweenData(var name: String, var type: GameObjectTweenAccessor.GameObjectTween, var target: FloatArray, var duration: Float, var keepComponents: ArrayList<Class<out Component>>, var endAction: Action) : CustomEditorImpl {
        private var started = false
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
                        gameObject.setState(lastState)
                        endAction(gameObject)
                    }.start(PCGame.tweenManager)
            started = true
        }


        override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
            with(ImGui) {
                if (treeNode(labelName)) {
                    val index = intArrayOf(GameObjectTweenAccessor.GameObjectTween.values().indexOf(type))
                    if (combo("type", index, GameObjectTweenAccessor.GameObjectTween.values().map { it.name }))
                        type = GameObjectTweenAccessor.GameObjectTween.values()[index[0]]
                    inputFloatN("target", target, target.size, 1, 0)
                    sliderFloat("duration", ::duration, 0f, 10f, "%.1f")

                    ImguiHelper.addImguiWidgetsArray("keep components", keepComponents, { PCGame.componentsClasses[0].java }, {
                        val index = intArrayOf(PCGame.componentsClasses.indexOf(it.obj.kotlin))
                        if (combo("", index, PCGame.componentsClasses.map { it.simpleName ?: "Nom inconnu" })) {
                            keepComponents.set(keepComponents.indexOf(it.obj), PCGame.componentsClasses[index[0]].java)
                            return@addImguiWidgetsArray true
                        }
                        false
                    })

                    treePop()
                }
            }
        }
    }

    fun startTween(gameObject: GameObject, tweenIndex: Int) {
        if (tweenIndex in tweens.indices)
            tweens[tweenIndex].start(gameObject)
    }

    private val addTweenTitle = "Ajouter un tween"
    private var currentTweenIndex = 0
    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            ImguiHelper.addImguiWidgetsArray("tweens", tweens, { TweenFactory.EmptyTween() }, {
                it.obj.insertImgui(it.obj.name, gameObject, level)
                false
            }) {
                if (button("Ajouter depuis..", Vec2(-1, 20f)))
                    openPopup(addTweenTitle)

                if (beginPopup(addTweenTitle)) {
                    functionalProgramming.withItemWidth(100f) {
                        combo("tweens", ::currentTweenIndex, TweenFactory.values().map { it.name })
                    }
                    if (button("Ajouter", Vec2(-1, 20f))) {
                        tweens.add(TweenFactory.values()[currentTweenIndex]())
                        closeCurrentPopup()
                    }

                    endPopup()
                }
            }
        }
    }
}