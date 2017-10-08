package be.catvert.pc.components.logics

import be.catvert.pc.actions.Action
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class InputComponent(val action: Action) : UpdeatableComponent() {
    override fun update() {
        if(Gdx.input.isKeyJustPressed(Input.Keys.A) && gameObject != null)
            action.perform(gameObject!!)
    }
}