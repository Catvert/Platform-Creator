package be.catvert.pc.components.logics

import be.catvert.pc.actions.Action
import be.catvert.pc.components.UpdeatableComponent
import be.catvert.pc.utility.CustomType
import be.catvert.pc.utility.ExposeEditor
import com.badlogic.gdx.Gdx

/**
 * Component permettant d'effectuer une action quand l'utilisateur appuie sur une touche
 * @param key La touche déclencheur
 * @param justPressed Est-ce que la touche doit-être pressé qu'une seule fois ?
 * @param action L'action à utiliser
 */
class InputComponent(@ExposeEditor(customType = CustomType.KEY_INT) val key: Int, @ExposeEditor val justPressed: Boolean, @ExposeEditor val action: Action) : UpdeatableComponent() {
    override fun update() {
        if (justPressed) {
            if (Gdx.input.isKeyJustPressed(key))
                performAction()
        } else {
            if (Gdx.input.isKeyPressed(key))
                performAction()
        }
    }

    private fun performAction() {
        action.perform(gameObject)
    }
}