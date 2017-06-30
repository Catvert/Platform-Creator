package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Listener
import com.badlogic.ashley.signals.Signal

/**
 * Created by Catvert on 04/06/17.
 */


/**
 * Enmu permettant de définir le type de mouvement de l'entité (fluide ou linéaire)
 */
enum class MovementType {
    SMOOTH, LINEAR
}

/**
 * Classe de données permettant de gérer les sauts notament en définissant la hauteur du saut
 * @property jumpHeight : La hauteur du saut
 * @property isJumping : Permet de savoir si l'entité est entrain de sauté
 * @property targetHeight : La hauteur en y à atteindre
 * @property startJumping : Débute le saut de l'entité
 * @property forceJumping : Permet de forcer le saut de l'entité
 */
data class JumpData(var jumpHeight: Int, var isJumping: Boolean = false, var targetHeight: Int = 0, var startJumping: Boolean = false, var forceJumping: Boolean = false)

/**
 * Enum permettant de connaître le côté toucher lors d'une collision
 */
enum class CollisionSide {
    OnLeft, OnRight, OnUp, OnDown, Unknow;

    operator fun unaryMinus(): CollisionSide {
        if (this == OnLeft)
            return OnRight
        else if (this == OnRight)
            return OnLeft
        else if (this == OnUp)
            return OnDown
        else if (this == OnDown)
            return OnUp
        return Unknow
    }
}

/**
 * Enum permettant de choisir la prochaine "action" physique à appliquer sur l'entité
 */
enum class NextActions {
    GO_LEFT, GO_RIGHT, GO_UP, GO_DOWN, GRAVITY, JUMP
}

/**
 * Permet de déterminer quel mask l'entité doit utiliser pour les collisions
 */
enum class MaskCollision {
    ALL, ONLY_PLAYER, ONLY_ENEMY
}

/**
 * Listener lors d'une collision avec une autre entité pour le mask correspondant
 */
data class CollisionListener(val entity: Entity, val collideEntity: Entity, val side: CollisionSide)

/**
 * Listener lorsque l'entité bouge
 */
data class MoveListener(val entity: Entity, val moveX: Int, val moveY: Int)

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 */
class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, var movementType: MovementType = MovementType.LINEAR, var gravity: Boolean = !isStatic, var maskCollision: MaskCollision = MaskCollision.ALL) : BaseComponent<PhysicsComponent>() {
    override fun copy(): PhysicsComponent {
        val physicsComp = PhysicsComponent(isStatic, moveSpeed, movementType, maskCollision = maskCollision)

        physicsComp.jumpData = if (jumpData != null) JumpData(jumpData!!.jumpHeight) else null
        physicsComp.onCollisionWith
        physicsComp.onMove = onMove

        return physicsComp
    }

    /**
     * Les nextActions représentes les actions que doit faire l'entité pendant le prochain frame
     */
    val nextActions = mutableSetOf<NextActions>()

    /**
     * Donnée de jump, si null, aucun jump sera disponible pour l'entité
     */
    var jumpData: JumpData? = null

    /**
     * La vitesse de déplacement x actuelle de l'entité (à titre d'information)
     */
    var actualMoveSpeedX = 0f

    /**
     * La vitesse de déplacement y actuelle de l'entité (à titre d'information)
     */
    var actualMoveSpeedY = 0f

    /**
     * Signal appelé lorsque une entité ayant ce component touche cette entité
     */
    var onCollisionWith = Signal<CollisionListener>()

    /**
     * Signal appelé lorsque l'entité bouge à cause des NextActions
     */
    var onMove = Signal<MoveListener>()

    /**
     * Permet à l'entité de savoir si elle est sur le sol ou non
     */
    var isOnGround = false

    /**
     * Permet à l'entité de passer les collisions, ne reçoit plus que les callbacks
     */
    var isSensor = false
}

fun physicsComponent(isStatic: Boolean, init: PhysicsComponent.() -> Unit, onCollisionWith: (PhysicsComponent.() -> Listener<CollisionListener>)? = null, onMove: (PhysicsComponent.() -> Listener<MoveListener>)? = null): PhysicsComponent {
    val physicsComponent = PhysicsComponent(isStatic)

    physicsComponent.init()

    if (onCollisionWith != null)
        physicsComponent.onCollisionWith.add(physicsComponent.onCollisionWith())
    if (onMove != null)
        physicsComponent.onMove.add(physicsComponent.onMove())

    return physicsComponent
}