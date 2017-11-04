package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Classe de base d'un component
 * @param active Permet de spécifier si le component est actif ou non
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Component(@ExposeEditor var active: Boolean = true) {
    @ExposeEditor var name = ReflectionUtility.simpleNameOf(this).removeSuffix("Component")

    open fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) {}
}

abstract class BasicComponent : Component()

/**
 * Classe abstraite permettant à un component d'être mis à jour
 */
abstract class UpdeatableComponent : Component() {
    abstract fun update(gameObject: GameObject)
}


/**
 * Classe abstraite permettant à un component d'être rendu à l'écran
 */
abstract class RenderableComponent(var flipX: Boolean = false, var flipY: Boolean = false) : Component() {
    abstract fun render(gameObject: GameObject, batch: Batch)
}