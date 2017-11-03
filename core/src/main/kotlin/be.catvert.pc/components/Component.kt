package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Classe de base d'un component
 * @param active Permet de spécifier si le component est actif ou non
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Component(@ExposeEditor var active: Boolean = true) {
    @ExposeEditor var name = ReflectionUtility.simpleNameOf(this).removeSuffix("Component")

    @JsonIgnore
    lateinit var gameObject: GameObject

    fun linkGameObject(gameObject: GameObject) {
        this.gameObject = gameObject
    }

    open fun onGOAddToContainer(gameObject: GameObject) {}
}

abstract class BasicComponent : Component()

/**
 * Classe abstraite permettant à un component d'être mis à jour
 */
abstract class UpdeatableComponent : Updeatable, Component()


/**
 * Classe abstraite permettant à un component d'être rendu à l'écran
 */
abstract class RenderableComponent(var flipX: Boolean = false, var flipY: Boolean = false) : Renderable, Component()