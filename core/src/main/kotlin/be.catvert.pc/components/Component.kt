package be.catvert.pc.components

import be.catvert.pc.GameObject
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Classe de base d'un component
 * @param active Permet de spécifier si le component est actif ou non
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Component(@JsonIgnore var active: Boolean = true) {
    @JsonIgnore
    var gameObject: GameObject? = null
        private set

    fun linkGameObject(gameObject: GameObject) {
        this.gameObject = gameObject
        onGameObjectSet(gameObject)
    }

    fun unlinkGameObject() {
        gameObject = null
    }

    protected open fun onGameObjectSet(gameObject: GameObject) {}
}