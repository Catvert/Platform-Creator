package be.catvert.pc.components.logics.ai

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.BasicComponent
import be.catvert.pc.components.logics.PhysicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.ImguiHelper.enum
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

/**
 * Component permettant d'ajouter de l'interaction avec un type de gameObject précis lors de la collision avec celui-ci.
 * @see PhysicsComponent
 * @param target Le type de gameObject ciblé
 * @param actionTarget L'action à appliquer sur le gameObject ciblé
 * @param actionCondTarget Condition pour appliquer l'action sur le gameObject ciblé selon le côté touché avec le gameObject
 * @param actionOnThis L'action à appliquer sur ce gameObject
 * @param actionCondThis Condition pour appliquer l'action sur ce gameObject selon le côté touché par le gameObject ciblé
 */
class AIComponent(var targets: ArrayList<GameObjectTag>, var actionTarget: Action, var actionCondTarget: ArrayList<BoxSide>, var actionOnThis: Action, var actionCondThis: ArrayList<BoxSide>) : BasicComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(arrayListOf(), EmptyAction(), arrayListOf(), EmptyAction(), arrayListOf())

    private var physicsComponent: PhysicsComponent? = null

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        physicsComponent = state.getComponent()
        if (physicsComponent != null) {
            physicsComponent!!.onCollisionWith.register {
                if (targets.contains(it.collideGameObject.tag)) {
                    actionCondTarget.forEach { side ->
                        if (side == it.side) {
                            actionTarget(it.collideGameObject)
                        }
                    }

                    actionCondThis.forEach { side ->
                        if (side == it.side) {
                            actionOnThis(gameObject)
                        }
                    }
                }
            }
        } else {
            Log.error { "Impossible de d'utiliser un AIComponent si l'objet n'a pas de PhysicsComponent" }
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            ImguiHelper.addImguiWidgetsArray("cibles", targets, { Tags.Player.tag }, {
                ImguiHelper.gameObjectTag(it, level)
            })
            ImguiHelper.addImguiWidgetsArray("conditions cible", actionCondTarget, { BoxSide.Left }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.action("action cible ", ::actionTarget, gameObject, level)
            }
            ImguiHelper.addImguiWidgetsArray("conditions gameObject", actionCondThis, { BoxSide.Left }, {
                enum("side", it.cast())
            }) {
                ImguiHelper.action("action go", ::actionOnThis, gameObject, level)
            }
        }
    }
}