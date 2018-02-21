package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.utility.ReflectionUtility
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Liste des différentes actions disponibles en jeu
 */
enum class Actions(val action: KClass<out Action>) {
    Atlas(AtlasAction::class),
    AtlasFlip(AtlasFlipAction::class),
    AtlasFlipSwitcher(AtlasFlipSwitcherAction::class),
    Empty(EmptyAction::class),
    GameObject(GameObjectAction::class),
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
    Tween(TweenAction::class),
    Zoom(ZoomAction::class)
}

/**
 * Représente une "action" à appliquer sur un gameObject
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
abstract class Action {

    /**
     * Permet d'invoquer l'action sur un gameObject quelconque
     */
    abstract operator fun invoke(gameObject: GameObject)

    override fun toString() = ReflectionUtility.simpleNameOf(this)
}