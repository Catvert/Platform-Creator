package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.tweens.EmptyTween
import be.catvert.pc.tweens.Tween
import be.catvert.pc.tweens.TweenSystem
import be.catvert.pc.tweens.Tweens
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import com.fasterxml.jackson.annotation.JsonCreator
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.functionalProgramming
import kotlin.reflect.full.createInstance


/**
 * Action permettant de lancer un Tween sur une entité
 * @see Tween
 * @see TweenSystem
 */
@Description("Permet d'appliquer un/des tween(s) précis sur une entité")
class TweenAction(var tween: Tween, var loop: Boolean = false) : Action(), UIImpl {
    @JsonCreator private constructor() : this(EmptyTween(), false)

    override fun invoke(entity: Entity, container: EntityContainer) {
        val tween = SerializationFactory.copy(tween)
        TweenSystem.startTween(tween, entity, if (loop) tween else null)
    }

    private var addTweenTitle = "Ajouter un tween"
    private var addTweenCurrentTween: Tween? = null
    private var addTweenComboIndex = 0
    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        fun addBtnAddTween(currentTween: Tween?, counter: Int) {
            var openPopup = false
            functionalProgramming.withId("tween add btn $counter") {
                if (ImGui.button("->", Vec2(20f, -1))) {
                    openPopup = true
                    addTweenCurrentTween = currentTween
                }
            }

            if (openPopup)
                ImGui.openPopup(addTweenTitle)
        }

        with(ImGui) {
            var currentTween: Tween? = tween

            var counter = 0
            while (currentTween != null) {
                if (counter == 0) {
                    addBtnAddTween(null, counter)
                    sameLine()
                }

                ++counter

                functionalProgramming.withGroup {
                    functionalProgramming.withId("tween $counter") {
                        currentTween!!.insertUI("tween", entity, level, editorUI)
                    }

                    if (currentTween!!.nextTween == null) {
                        checkbox("boucle", ::loop)
                    }
                }
                sameLine()

                addBtnAddTween(currentTween, counter)

                currentTween = currentTween.nextTween

                if (currentTween != null) {
                    sameLine()
                }
            }

            functionalProgramming.popup(addTweenTitle) {
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("tween", ::addTweenComboIndex, Tweens.values().map {
                        it.tween.simpleName?.removeSuffix("Tween") ?: "Nom introuvable"
                    })
                }

                if (button("Ajouter", Vec2(Constants.defaultWidgetsWidth, 0))) {
                    val tween = Tweens.values()[addTweenComboIndex].tween.createInstance()
                    addTweenCurrentTween?.apply {
                        this.nextTween = tween
                    } ?: run { this@TweenAction.tween = tween }
                    closeCurrentPopup()
                }
            }
        }
    }
}