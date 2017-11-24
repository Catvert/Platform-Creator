package be.catvert.pc.components

import aurelienribon.tweenengine.Tween
import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.PCGame
import be.catvert.pc.actions.Action
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.factories.TweenFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ExposeEditorFactory
import com.badlogic.gdx.Gdx
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming

class TweenComponent(var tweenData: Array<TweenData>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor(): this(arrayOf())

    data class TweenData(var name: String, var type: GameObjectTweenAccessor.GameObjectTween, var target: FloatArray, var duration: Float, var newComponents: Array<Component>, var keepComponents: Array<Class<out Component>>, var endAction: Action): CustomEditorImpl {
        @JsonIgnore private var started = false
        fun start(gameObject: GameObject) {
            if(started)
                return

            val tweenState = gameObject.addState("tween-state") {
                gameObject.getCurrentState().getComponents().forEach {
                    if(keepComponents.contains(it.javaClass))
                        addComponent(it)
                    newComponents.forEach {
                        addComponent(it)
                    }
                }
            }

            val lastState = gameObject.getCurrentStateIndex()
            gameObject.setState(tweenState, false)

            Tween.to(gameObject, type.tweenType, duration)
                    .target(*target)
                    .setCallback { _, _->
                        started = false
                        gameObject.setState(lastState, false)
                        endAction(gameObject)
                    }.start(PCGame.tweenManager)
            started = true
        }

        @JsonIgnore private val editTitle = "Éditer le tween"
        override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
            with(ImGui) {
                if(button("Éditer $labelName"))
                    openPopup(editTitle)
            }
        }

        override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
            super.insertImguiPopup(gameObject, editorScene)

            with(ImGui) {
                val popupWidth = Gdx.graphics.width / 3 * 2
                val popupHeight = Gdx.graphics.height / 3 * 2
                setNextWindowSize(Vec2(popupWidth, popupHeight))
                setNextWindowPos(Vec2(Gdx.graphics.width / 2f - popupWidth / 2f, Gdx.graphics.height / 2f - popupHeight / 2f))
                if(beginPopup(editTitle)) {
                    val index = intArrayOf(GameObjectTweenAccessor.GameObjectTween.values().indexOf(type))
                    if(combo("type", index, GameObjectTweenAccessor.GameObjectTween.values().map { it.name }))
                        type = GameObjectTweenAccessor.GameObjectTween.values()[index[0]]
                    inputFloatN("target", target, target.size, 1, 0)
                    sliderFloat("duration", this@TweenData::duration, 0f, 10f, "%.1f")

                    endPopup()
                }
            }
        }
    }

    fun startTween(gameObject: GameObject, tweenIndex: Int) {
        if(tweenIndex in tweenData.indices)
            tweenData[tweenIndex].start(gameObject)
    }

    @JsonIgnore private val addTweenTitle = "Ajouter un tween"
    @JsonIgnore private var currentTweenIndex = 0
    override fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene) {
        with(ImGui) {
            editorScene.addImguiWidgetsArray(gameObject, "tweens", { tweenData }, { tweenData = it }, { TweenFactory.EmptyTween() }, { tweenData[it].name }, { ExposeEditorFactory.empty })

            if(button("Ajouter depuis..", Vec2(-1, 20f)))
                openPopup(addTweenTitle)
        }
    }

    override fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) {
        super.insertImguiPopup(gameObject, editorScene)

        with(ImGui) {
            if(beginPopup(addTweenTitle)) {
                combo("tweens", this@TweenComponent::currentTweenIndex, TweenFactory.values().map { it.name })
                if(button("Ajouter", Vec2(-1, 20f))) {
                    tweenData += TweenFactory.values()[currentTweenIndex]()
                    closeCurrentPopup()
                }

                endPopup()
            }
        }
    }
}