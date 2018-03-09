package be.catvert.pc.eca.components

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.components.basics.SoundComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.components.logics.*
import be.catvert.pc.eca.containers.EntityContainer
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Représente les différents components disponibles
 */
enum class Components(val component: KClass<out Component>) {
    Texture(TextureComponent::class),
    Sound(SoundComponent::class),
    Input(InputComponent::class),
    Life(LifeComponent::class),
    Mover(MoverComponent::class),
    Physics(PhysicsComponent::class),
    Sensor(SensorComponent::class),
    Timer(TimerComponent::class),
    Script(ScriptComponent::class)
}

/**
 * Représente un élément consituant un entity
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Component {
    open fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        this.entity = entity
    }

    @JsonIgnore
    var active = true

    @JsonIgnore
    protected lateinit var entity: Entity

    override fun toString() = ""
}