package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.utility.InheritanceAdapter
import com.google.gson.annotations.JsonAdapter

@JsonAdapter(ComponentResolverAdapter::class)
abstract class Component {
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

private class ComponentResolverAdapter : InheritanceAdapter<Component>()