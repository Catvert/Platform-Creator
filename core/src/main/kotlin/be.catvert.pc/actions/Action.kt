package be.catvert.pc.actions

import be.catvert.pc.GameObject
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente une action qu'un gameObject peut subir selon le component utilisé
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
interface Action {
    fun perform(gameObject: GameObject)
}