package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectMatrixContainer
import be.catvert.pc.Log
import be.catvert.pc.actions.NextPhysicsActions
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.Rect
import be.catvert.pc.utility.Signal
import com.badlogic.gdx.math.MathUtils

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
 * Listener lorsque l'entité bouge
 */
data class MoveListener(val gameObject: GameObject, val moveX: Int, val moveY: Int)

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 */
class PhysicsComponent(var isStatic: Boolean, var moveSpeed: Int = 0, var movementType: MovementType = MovementType.LINEAR, var gravity: Boolean = !isStatic, var maskCollision: MaskCollision = MaskCollision.ALL) : UpdeatableComponent() {
    /**
     * Les nextActions représentes les actions que doit faire l'entité pendant le prochain frame
     */
    val nextActions = mutableSetOf<NextPhysicsActions>()

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
     * Permet à l'entité de savoir si elle est sur le sol ou non
     */
    var isOnGround = false

    /**
     * Permet à l'entité de passer les collisions, ne reçoit plus que les callbacks
     */
    var isSensor = false

    val gravitySpeed = 15

    override fun update() {
        if (isStatic || gameObject == null) return

        val gameObject = gameObject!!

        if (gravity /*&& level.applyGravity*/) // TODO
            nextActions += NextPhysicsActions.GRAVITY

        if (jumpData?.forceJumping == true) {
            nextActions += NextPhysicsActions.JUMP
        }

        var moveSpeedX = 0f
        var moveSpeedY = 0f
        var addJumpAfterClear = false

        nextActions.forEach action@ {
            when (it) {
                NextPhysicsActions.GO_LEFT -> moveSpeedX -= moveSpeed
                NextPhysicsActions.GO_RIGHT -> moveSpeedX += moveSpeed
                NextPhysicsActions.GO_UP -> moveSpeedY += moveSpeed
                NextPhysicsActions.GO_DOWN -> moveSpeedY -= moveSpeed
                NextPhysicsActions.GRAVITY -> if (gravity) moveSpeedY -= gravitySpeed
                NextPhysicsActions.JUMP -> {
                    if (jumpData == null) {
                        Log.error { "L'entité essaye de sauter mais ne contient pas de jumpData !" }
                        return@action
                    }

                    val jumpData = jumpData!!

                    if (!jumpData.isJumping) {
                        if (!jumpData.forceJumping) {
                            if (!checkIsOnGround(gameObject)) {
                                return@action
                            }
                        } else {
                            jumpData.forceJumping = false
                        }
                        jumpData.isJumping = true
                        jumpData.targetHeight = gameObject.rectangle.y + jumpData.jumpHeight
                        jumpData.startJumping = true

                        gravity = false

                        moveSpeedY = gravitySpeed.toFloat()
                        addJumpAfterClear = true
                    } else {
                        if (gameObject.rectangle.y >= jumpData.targetHeight || collideOnMove(0, gravitySpeed, gameObject)) {
                            gravity = true
                            jumpData.isJumping = false
                        } else {

                            moveSpeedY = gravitySpeed.toFloat()
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

        tryMove(moveSpeedX.toInt(), moveSpeedY.toInt(), gameObject) // move l'entité

        nextActions.clear()

        if (addJumpAfterClear)
            nextActions += NextPhysicsActions.JUMP

        isOnGround = checkIsOnGround(gameObject)
    }
}

/**
 * Permet d'essayer de déplacer une entité ayant un physicsComponent
 * @param moveX : Le déplacement x
 * @param moveY : Le déplacement y
 */
private fun tryMove(moveX: Int, moveY: Int, gameObject: GameObject) {
    if (moveX != 0 || moveY != 0) {
        var newMoveX = moveX
        var newMoveY = moveY

        if (!collideOnMove(moveX, 0, gameObject)) {
            gameObject.rectangle.x = Math.max(0, gameObject.rectangle.x + moveX)
            //gameObject.getComponent<PhysicsComponent>()?.onMove.dispatch(MoveListener(entity, if (transformTarget.rectangle.x == 0f) 0 else moveX, moveY)) // TODO voir ici pour amélioration
            newMoveX = 0
        }
        if (!collideOnMove(0, moveY, gameObject)) {
            gameObject.rectangle.y += moveY
            // physicsTarget.onMove.dispatch(MoveListener(entity, moveX, moveY))
            newMoveY = 0
        }

        if (newMoveX > 0)
            newMoveX -= 1
        else if (newMoveX < 0)
            newMoveX += 1

        if (newMoveY > 0)
            newMoveY -= 1
        else if (newMoveY < 0)
            newMoveY += 1
        tryMove(newMoveX, newMoveY, gameObject)
    }
}

/**
 * Permet de vérifier si l'entité est sur le sol ou pas
 */
fun checkIsOnGround(gameObject: GameObject) = collideOnMove(0, -1, gameObject)

/**
 * Permet de tester si un déplacement est possible ou non
 * @param moveX : Le déplacement x
 * @param moveY : Le déplacement y
 * @param entity : L'entité à tester
 */
private fun collideOnMove(moveX: Int, moveY: Int, gameObject: GameObject): Boolean {
    val newRect = Rect(gameObject.rectangle)
    newRect.position = Point(newRect.x + moveX, newRect.y + moveY)

    if (gameObject.container != null && gameObject.container is GameObjectMatrixContainer) {
        val container = gameObject.container!! as GameObjectMatrixContainer
        container.getRectCells(newRect).forEach {
            container.matrixGrid[it.x][it.y].first.filter {
                it.id != gameObject.id
                        && it.hasComponent<PhysicsComponent>()
                        && when (it.getComponent<PhysicsComponent>()?.maskCollision) {
                    MaskCollision.ALL -> true
                    MaskCollision.ONLY_PLAYER -> gameObject.tag == GameObject.Tag.Player
                    MaskCollision.ONLY_ENEMY -> gameObject.tag == GameObject.Tag.Enemy
                    null -> false
                }
            }.forEach {
                if (newRect.overlaps(it.rectangle)) {
                    val side = if (moveX > 0) CollisionSide.OnRight else if (moveX < 0) CollisionSide.OnLeft else if (moveY > 0) CollisionSide.OnUp else if (moveY < 0) CollisionSide.OnDown else CollisionSide.Unknow

                    if (gameObject.tag == GameObject.Tag.Enemy && it.tag == GameObject.Tag.Player) {
                    }
                    //entity[EnemyComponent::class.java].onPlayerCollision.dispatch(CollisionListener(entity, it, side))
                    else if (gameObject.tag == GameObject.Tag.Player && it.tag == GameObject.Tag.Enemy) {
                    }
                    // it[EnemyComponent::class.java].onPlayerCollision.dispatch(CollisionListener(it, entity, -side)) // - side to inverse the side
                    else if (it.getComponent<PhysicsComponent>()?.isSensor == false)
                        gameObject.getComponent<PhysicsComponent>()?.onCollisionWith?.invokeSignal(CollisionListener(gameObject, it, side))
                    else // is a sensor
                        it.getComponent<PhysicsComponent>()?.onCollisionWith?.invokeSignal(CollisionListener(it, gameObject, side))

                    if (it.getComponent<PhysicsComponent>()?.isSensor == false)
                        return true
                }
            }
        }
    }
    return false
}