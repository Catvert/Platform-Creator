package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.actions.PhysicsAction.PhysicsActions
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Signal
import com.badlogic.gdx.math.MathUtils
import com.dongbat.jbump.CollisionFilter
import com.dongbat.jbump.Item
import com.dongbat.jbump.Response
import com.dongbat.jbump.World
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Enmu permettant de définir le type de mouvement de l'entité (fluide ou linéaire)
 */
enum class MovementType {
    SMOOTH, LINEAR
}

/**
 * Classe de données permettant de gérer les sauts notament en définissant la hauteur du saut
 * @property isJumping : Permet de savoir si l'entité est entrain de sauté
 * @property targetHeight : La hauteur en y à atteindre
 * @property startJumping : Débute le saut de l'entité
 * @property forceJumping : Permet de forcer le saut de l'entité
 */
data class JumpData(var isJumping: Boolean = false, var targetHeight: Int = 0, var startJumping: Boolean = false, var forceJumping: Boolean = false)

/**
 * Enum permettant de connaître le côté toucher lors d'une collision
 */
enum class CollisionSide {
    OnLeft, OnRight, OnUp, OnDown, Unknow;

    operator fun unaryMinus(): CollisionSide = when (this) {
        CollisionSide.OnLeft -> OnRight
        CollisionSide.OnRight -> OnLeft
        CollisionSide.OnUp -> OnDown
        CollisionSide.OnDown -> OnUp
        CollisionSide.Unknow -> Unknow
    }
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
data class CollisionListener(val gameObject: GameObject, val collideGameObject: GameObject, val side: CollisionSide)

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 */
class PhysicsComponent(@ExposeEditor var isStatic: Boolean,
                       @ExposeEditor(maxInt = 100) var moveSpeed: Int = 0,
                       @ExposeEditor var movementType: MovementType = MovementType.LINEAR,
                       @ExposeEditor var gravity: Boolean = !isStatic,
                       @ExposeEditor var maskCollision: MaskCollision = MaskCollision.ALL,
                       @ExposeEditor(maxInt = 1000) var jumpHeight: Int = 0,
                       @ExposeEditor var jumpAction: Action = EmptyAction()) : LogicsComponent() {
    @JsonCreator private constructor() : this(true)

    /**
     * Les nextActions représentes les actions que doit faire l'entité pendant le prochain frame
     */
    @JsonIgnore
    val nextActions = mutableSetOf<PhysicsActions>()

    @JsonIgnore private val jumpData: JumpData = JumpData()

    /**
     * La vitesse de déplacement x actuelle de l'entité (à titre d'information)
     */
    @JsonIgnore private var actualMoveSpeedX = 0f

    /**
     * La vitesse de déplacement y actuelle de l'entité (à titre d'information)
     */
    @JsonIgnore private var actualMoveSpeedY = 0f

    /**
     * Signal appelé lorsque une entité ayant ce component touche cette entité
     */
    @JsonIgnore
    val onCollisionWith = Signal<CollisionListener>()

    /**
     * Permet à l'entité de savoir si elle est sur le sol ou non
     */
    @JsonIgnore private var isOnGround = false

    @JsonIgnore private val gravitySpeed = 15f

    @JsonIgnore private lateinit var item: Item<GameObject>

    @JsonIgnore private lateinit var world: World<GameObject>

    override fun onAddToContainer(gameObject: GameObject, container: GameObjectContainer) {
        world = (container as Level).world

        super.onAddToContainer(gameObject, container)
    }

    override fun onStateActive(gameObject: GameObject) {
        super.onStateActive(gameObject)

        item = world.add(Item(gameObject), gameObject.position().x, gameObject.position().y, gameObject.size().width.toFloat(), gameObject.size().height.toFloat())
    }

    override fun onStateInactive(gameObject: GameObject) {
        super.onStateInactive(gameObject)

        world.remove(item)
    }

    override fun update(gameObject: GameObject) {
        if (isStatic || !world.hasItem(item)) return

        world.update(item, gameObject.position().x, gameObject.position().y)

        if (gravity && (gameObject.container as? Level)?.applyGravity == true)
            nextActions += PhysicsAction.PhysicsActions.GRAVITY

        if (jumpData.forceJumping) {
            nextActions += PhysicsAction.PhysicsActions.JUMP
        }

        var moveSpeedX = 0f
        var moveSpeedY = 0f
        var addJumpAfterClear = false

        nextActions.forEach action@ {
            when (it) {
                PhysicsActions.GO_LEFT -> moveSpeedX -= moveSpeed
                PhysicsActions.GO_RIGHT -> moveSpeedX += moveSpeed
                PhysicsActions.GO_UP -> moveSpeedY += moveSpeed
                PhysicsActions.GO_DOWN -> moveSpeedY -= moveSpeed
                PhysicsActions.GRAVITY -> if (gravity) moveSpeedY -= gravitySpeed
                PhysicsActions.JUMP -> {
                    if (!jumpData.isJumping) {
                        if (!jumpData.forceJumping) {
                            if (checkYMove(gameObject.position(), -1f)) {
                                return@action
                            }
                        } else {
                            jumpData.forceJumping = false
                        }
                        jumpData.isJumping = true
                        jumpData.targetHeight = gameObject.rectangle.y.toInt() + jumpHeight
                        jumpData.startJumping = true

                        gravity = false

                        moveSpeedY = gravitySpeed
                        addJumpAfterClear = true
                        jumpAction.invoke(gameObject)
                    } else {
                        // Vérifie si le go est arrivé à la bonne hauteur de saut ou s'il rencontre un obstacle au dessus de lui
                        if (gameObject.rectangle.y >= jumpData.targetHeight || !checkYMove(gameObject.position(), gravitySpeed)) {
                            gravity = true
                            jumpData.isJumping = false
                        } else {
                            moveSpeedY = gravitySpeed
                            addJumpAfterClear = true
                        }
                        jumpData.startJumping = false
                    }
                }
            }
        }

        if (movementType == MovementType.SMOOTH) {
            moveSpeedX = MathUtils.lerp(actualMoveSpeedX, moveSpeedX, 0.2f)
            moveSpeedY = MathUtils.lerp(actualMoveSpeedY, moveSpeedY, 0.2f)
        }

        actualMoveSpeedX = moveSpeedX
        actualMoveSpeedY = moveSpeedY

        val collisions = world.move(item, gameObject.position().x + moveSpeedX, gameObject.position().y + moveSpeedY, collisionFilter)

        for(i in 0 until collisions.projectedCollisions.size()) {
            val collision = collisions.projectedCollisions.get(i)
            val normal = collision.normal

            val collisionSide = if(normal.x != 0f) {
                when(normal.x) {
                    -1f -> CollisionSide.OnRight
                    1f -> CollisionSide.OnLeft
                    else -> CollisionSide.Unknow
                }
            }
            else {
                when(normal.y) {
                    -1f -> CollisionSide.OnDown
                    1f -> CollisionSide.OnUp
                    else -> CollisionSide.Unknow
                }
            }

            (collision.item.userData as GameObject).getCurrentState().getComponent<PhysicsComponent>()?.onCollisionWith?.invoke(CollisionListener(collision.item.userData as GameObject, collision.other.userData as GameObject, collisionSide))
            (collision.other.userData as GameObject).getCurrentState().getComponent<PhysicsComponent>()?.onCollisionWith?.invoke(CollisionListener(collision.other.userData as GameObject, collision.item.userData as GameObject, -collisionSide))
        }

        val newRect = world.getRect(item)
        gameObject.rectangle.x = newRect.x
        gameObject.rectangle.y = newRect.y

        nextActions.clear()

        if (addJumpAfterClear)
            nextActions += PhysicsActions.JUMP

        isOnGround = !checkYMove(gameObject.position(), -1f)
    }

    /**
     * Permet de vérifier si l'entité est sur le sol ou pas
     */
    private fun checkYMove(actualPos: Point, moveY: Float) = world.check(item, actualPos.x, actualPos.y + moveY, collisionFilter).goalY != actualPos.y

    companion object {
        private val collisionFilter = CollisionFilter { i1, i2 ->
            val i1 = (i1.userData as GameObject)
            val i2 = (i2.userData as GameObject)

            val response = when (i2.getCurrentState().getComponent<PhysicsComponent>()!!.maskCollision) {
                MaskCollision.ALL -> Response.slide
                MaskCollision.ONLY_PLAYER -> if (i1.tag == GameObject.Tag.Player) Response.slide else Response.cross
                MaskCollision.ONLY_ENEMY -> if (i1.tag == GameObject.Tag.Enemy) Response.slide else Response.cross
            }

            response
        }
    }
}