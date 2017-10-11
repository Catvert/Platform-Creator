package be.catvert.pc.components

import be.catvert.pc.GameObject

/**
 * Classe de base d'un component
 * @param active Permet de spécifier si le component est actif ou non
 */
abstract class Component(var active: Boolean = true) {
    @Transient
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