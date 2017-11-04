package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente une action qu'un gameObject peut subir selon le component utilisé
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
interface Action {
    fun perform(gameObject: GameObject)
    operator fun invoke(gameObject: GameObject) = perform(gameObject)
}

