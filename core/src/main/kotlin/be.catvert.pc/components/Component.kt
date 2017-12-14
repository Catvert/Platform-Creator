package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.utility.ReflectionUtility
import be.catvert.pc.utility.ResourceLoader
import be.catvert.pc.utility.ResourceManager
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente un élément consituant un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
sealed class Component : ResourceLoader {
    open fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {}

    override fun loadResources() {}
}

/**
 * Component n'ayant pas besoin d'être mis à jour
 */
abstract class BasicComponent : Component()

/**
 * Component ayant le besoin d'être mis à jour
 */
abstract class LogicsComponent : Component() {
    abstract fun update(gameObject: GameObject)
}

/**
 * Component ayant le besoin de dessiner
 */
abstract class RenderableComponent(var flipX: Boolean = false, var flipY: Boolean = false, @JsonIgnore var alpha: Float = 1f) : Component() {
    abstract fun render(gameObject: GameObject, batch: Batch)
}