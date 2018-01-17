package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente une "action" a appliquer sur un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Action {

    /**
     * Permet d'invoquer l'action sur un gameObject quelconque
     */
    abstract operator fun invoke(gameObject: GameObject)

    override fun toString() = ReflectionUtility.simpleNameOf(this)
}