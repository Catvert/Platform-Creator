package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Liste des différentes actions disponibles en jeu
 */
enum class Actions(val action: KClass<out Action>) {
    Empty(EmptyAction::class),
    Entity(EntityAction::class),
    Gravity(GravityAction::class),
    Input(InputAction::class),
    Level(LevelAction::class),
    Life(LifeAction::class),
    Move(MoveAction::class),
    Multiplexer(MultiplexerAction::class),
    Physics(PhysicsAction::class),
    RemoveGO(RemoveGOAction::class),
    Resize(ResizeAction::class),
    Score(ScoreAction::class),
    Sound(SoundAction::class),
    SpawnPosition(SpawnPositionAction::class),
    SpawnSide(SpawnSideAction::class),
    State(StateAction::class),
    StateSwitcher(StateSwitcherAction::class),
    Tag(TagAction::class),
    Teleport(TeleportAction::class),
    TeleportSide(TeleportSideAction::class),
    Texture(TextureAction::class),
    TextureFlip(TextureFlipAction::class),
    TextureFlipSwitcher(TextureFlipSwitcherAction::class),
    Tween(TweenAction::class),
    Zoom(ZoomAction::class)
}

/**
 * Représente une "action" à appliquer sur un entity
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Action {

    /**
     * Permet d'invoquer l'action sur un entity quelconque
     */
    abstract operator fun invoke(entity: Entity)

    override fun toString() = ReflectionUtility.simpleNameOf(this)
}