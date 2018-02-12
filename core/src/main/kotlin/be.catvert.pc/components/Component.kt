package be.catvert.pc.components

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.components.basics.SoundComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.components.logics.*
import be.catvert.pc.containers.GameObjectContainer
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

enum class Components(val component: KClass<out Component>) {
    Atlas(AtlasComponent::class),
    Sound(SoundComponent::class),
    Input(InputComponent::class),
    Life(LifeComponent::class),
    Mover(MoverComponent::class),
    Physics(PhysicsComponent::class),
    Sensor(SensorComponent::class),
    Timer(TimerComponent::class);
}

/**
 * Représente un élément consituant un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Component {
    open fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        this.gameObject = gameObject
    }

    @JsonIgnore
    var active = true

    @JsonIgnore
    protected lateinit var gameObject: GameObject

    override fun toString() = ""
}