package be.catvert.pc.actions

import be.catvert.pc.GameObject
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente une "action" a appliquer sur un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
interface Action {
    /**
     * Permet d'invoquer l'action sur un gameObject quelconque
     */
    operator fun invoke(gameObject: GameObject)
}