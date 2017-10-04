package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.github.salomonbrys.kotson.value
import com.google.gson.GsonBuilder

class InputComponent(val action: Action) : UpdeatableComponent() {
    override fun update() {
        if(Gdx.input.isKeyJustPressed(Input.Keys.A) && gameObject != null)
            action.perform(gameObject!!)
    }
}