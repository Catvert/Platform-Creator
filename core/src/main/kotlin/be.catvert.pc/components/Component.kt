package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ReflectionUtility
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Classe de base d'un component
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Component {
    @ExposeEditor
    var name = ReflectionUtility.simpleNameOf(this).removeSuffix("Component")

    open fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) = Unit

    open fun onStateActive(gameObject: GameObject) = Unit
    open fun onStateInactive(gameObject: GameObject) = Unit
}

abstract class BasicComponent : Component()

/**
 * Classe abstraite permettant à un component d'être mis à jour
 */
abstract class LogicsComponent : Component() {
    abstract fun update(gameObject: GameObject)
}


/**
 * Classe abstraite permettant à un component d'être rendu à l'écran
 */
abstract class RenderableComponent(var flipX: Boolean = false, var flipY: Boolean = false, @JsonIgnore var alpha: Float = 1f) : Component() {
    abstract fun render(gameObject: GameObject, batch: Batch)
}