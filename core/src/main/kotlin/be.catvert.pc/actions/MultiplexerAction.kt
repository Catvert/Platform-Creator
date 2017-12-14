package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditorFactory
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'utiliser d'appliquer plusieurs actions sur un gameObject à la place d'une seule.
 */
class MultiplexerAction(var actions: ArrayList<Action>) : Action, CustomEditorImpl {
    constructor(vararg actions: Action) : this(arrayListOf(*actions))
    @JsonCreator private constructor() : this(arrayListOf())

    override fun invoke(gameObject: GameObject) {
        actions.forEach {
            it(gameObject)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level) {
        ImguiHelper.addImguiWidgetsArray("actions", actions, { ReflectionUtility.simpleNameOf(it) }, { EmptyAction() }, gameObject, level)
    }
}