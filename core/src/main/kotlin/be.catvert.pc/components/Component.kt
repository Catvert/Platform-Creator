package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.containers.GameObjectContainer
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Représente un élément consituant un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Component {
    open fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        this.gameObject = gameObject
    }

    @JsonIgnore
    protected lateinit var gameObject: GameObject

    override fun toString() = ""
}