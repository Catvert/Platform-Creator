package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditor
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 * @param key La touche déclencheur
 * @param justPressed Est-ce que la touche doit-être pressé qu'une seule fois ?
 * @param action L'action à utiliser
 */
class InputComponent(@ExposeEditor(customType = CustomType.KEY_INT) var key: Int, @ExposeEditor var justPressed: Boolean, @ExposeEditor var action: Action) : UpdeatableComponent() {
    @JsonCreator private constructor(): this(Input.Keys.UNKNOWN, false, EmptyAction())

    override fun update(gameObject: GameObject) {
        if (justPressed) {
            if (Gdx.input.isKeyJustPressed(key))
                action(gameObject)
        } else {
            if (Gdx.input.isKeyPressed(key))
                action(gameObject)
        }
    }
}