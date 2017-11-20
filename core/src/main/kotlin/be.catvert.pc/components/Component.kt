package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ReflectionUtility
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Classe de base d'un component
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Component {
    @ExposeEditor var name = ReflectionUtility.simpleNameOf(this).removeSuffix("Component")

    open fun onGOAddToContainer(state: GameObjectState, gameObject: GameObject) = Unit
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