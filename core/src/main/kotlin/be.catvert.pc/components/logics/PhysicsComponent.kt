package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectMatrixContainer
import be.catvert.pc.utility.Signal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

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
     * Permet à l'entité de savoir si elle est sur le sol ou non
     */
    var isOnGround = false

    /**
     * Permet à l'entité de passer les collisions, ne reçoit plus que les callbacks
     */
    var isSensor = false

    override fun update() {

    }

    /*override fun update() {


            if (physicsComp.isStatic || !physicsComp.active)
                return@forEach

            if (physicsComp.gravity && level.applyGravity)
                physicsComp.nextActions += NextActions.GRAVITY

            if (physicsComp.jumpData?.forceJumping ?: false) {
                physicsComp.nextActions += NextActions.JUMP
            }

            var moveSpeedX = 0f
            var moveSpeedY = 0f
            var addJumpAfterClear = false

            physicsComp.nextActions.forEach action@ {
                when (it) {
                    NextActions.GO_LEFT -> moveSpeedX -= physicsComp.moveSpeed
                    NextActions.GO_RIGHT -> moveSpeedX += physicsComp.moveSpeed
                    NextActions.GO_UP -> moveSpeedY += physicsComp.moveSpeed
                    NextActions.GO_DOWN -> moveSpeedY -= physicsComp.moveSpeed
                    NextActions.GRAVITY -> if (physicsComp.gravity) moveSpeedY -= gravity
                    NextActions.JUMP -> {
                        if (physicsComp.jumpData == null) {
                            ktx.log.error { "L'entité essaye de sauter mais ne contient pas de jumpData !" }
                            return@action
                        }

                        val jumpData = physicsComp.jumpData!!

                        if (!jumpData.isJumping) {
                            if (!jumpData.forceJumping) {
                                if (!checkIsOnGround(entity)) {
                                    return@action
                                }
                            } else {
                                jumpData.forceJumping = false
                            }
                            jumpData.isJumping = true
                            jumpData.targetHeight = transformComp.rectangle.y.toInt() + jumpData.jumpHeight
                            jumpData.startJumping = true

                            physicsComp.gravity = false

                            moveSpeedY = gravity.toFloat()
                            addJumpAfterClear = true
                        } else {
                            if (transformComp.rectangle.y >= jumpData.targetHeight || collideOnMove(0, gravity, entity)) {
                                physicsComp.gravity = true
                                jumpData.isJumping = false
                            } else {

                                moveSpeedY = gravity.toFloat()
                                addJumpAfterClear = true
                            }
                            jumpData.startJumping = false
                        }
                    }
                }
            }

            if (physicsComp.movementType == MovementType.SMOOTH) {
                moveSpeedX = MathUtils.lerp(physicsComp.actualMoveSpeedX, moveSpeedX, 0.2f)
                moveSpeedY = MathUtils.lerp(physicsComp.actualMoveSpeedY, moveSpeedY, 0.2f)
            }

            physicsComp.actualMoveSpeedX = moveSpeedX
            physicsComp.actualMoveSpeedY = moveSpeedY

            tryMove(moveSpeedX.toInt(), moveSpeedY.toInt(), entity) // move l'entité

            physicsComp.nextActions.clear()

            if (addJumpAfterClear)
                physicsComp.nextActions += NextActions.JUMP

            physicsComp.isOnGround = checkIsOnGround(entity)
        }
    }

    /**
     * Permet d'essayer de déplacer une entité ayant un physicsComponent
     * @param moveX : Le déplacement x
     * @param moveY : Le déplacement y
     */
    private fun tryMove(moveX: Int, moveY: Int, entity: Entity) {
        val transformTarget = transformMapper[entity]
        val physicsTarget = physicsMapper[entity]

        if (moveX != 0 || moveY != 0) {
            var newMoveX = moveX
            var newMoveY = moveY

            if (!collideOnMove(moveX, 0, entity)) {
                transformTarget.rectangle.x = Math.max(0f, transformTarget.rectangle.x + moveX)
                level.setEntityGrid(entity)
                physicsTarget.onMove.dispatch(MoveListener(entity, if (transformTarget.rectangle.x == 0f) 0 else moveX, moveY)) // TODO voir ici pour amélioration
                newMoveX = 0
            }
            if (!collideOnMove(0, moveY, entity)) {
                transformTarget.rectangle.y += moveY
                level.setEntityGrid(entity)
                physicsTarget.onMove.dispatch(MoveListener(entity, moveX, moveY))
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
            tryMove(newMoveX, newMoveY, entity)
        }
    }

    /**
     * Permet de vérifier si l'entité est sur le sol ou pas
     */
    fun checkIsOnGround(entity: Entity) = collideOnMove(0, -1, entity)

    /**
     * Permet de tester si un déplacement est possible ou non
     * @param moveX : Le déplacement x
     * @param moveY : Le déplacement y
     * @param entity : L'entité à tester
     */
    private fun collideOnMove(moveX: Int, moveY: Int, gameObject: GameObject): Boolean {
        val physicsTarget = physicsMapper[entity]
        val transformTarget = transformMapper[entity]

        val newRect = Rectangle(transformTarget.rectangle)
        newRect.setPosition(newRect.x + moveX, newRect.y + moveY)

        level.getRectCells(newRect).forEach {
            level.matrixGrid[it.x][it.y].first.filter {
                transformMapper[it] != transformTarget  // On vérifie si la targetEntity n'est pas la même que celle dans l'itération
                        && physicsMapper.has(it) // On vérifie si l'entité qu'on parcourt a un physicsComponent
                        && when (physicsMapper[it].maskCollision) { // On vérifie si le mask est correcte
                    MaskCollision.ALL -> true
                    MaskCollision.ONLY_PLAYER -> entity isType EntityFactory.EntityType.Player
                    MaskCollision.ONLY_ENEMY -> entity isType EntityFactory.EntityType.Enemy
                }
            }.forEach {
                val transformComponent = transformMapper[it]

                if (newRect.overlaps(transformComponent.rectangle)) {
                    val side = if (moveX > 0) CollisionSide.OnRight else if (moveX < 0) CollisionSide.OnLeft else if (moveY > 0) CollisionSide.OnUp else if (moveY < 0) CollisionSide.OnDown else CollisionSide.Unknow

                    if (entity isType EntityFactory.EntityType.Enemy && it isType EntityFactory.EntityType.Player)
                        entity[EnemyComponent::class.java].onPlayerCollision.dispatch(CollisionListener(entity, it, side))
                    else if (entity isType EntityFactory.EntityType.Player && it isType EntityFactory.EntityType.Enemy)
                        it[EnemyComponent::class.java].onPlayerCollision.dispatch(CollisionListener(it, entity, -side)) // - side to inverse the side
                    else if (!physicsMapper[it].isSensor)
                        physicsTarget.onCollisionWith.dispatch(CollisionListener(entity, it, side))
                    else // is a sensor
                        physicsMapper[it].onCollisionWith.dispatch(CollisionListener(it, entity, side))

                    if (!physicsMapper[it].isSensor)
                        return true
                }
            }
        }
        return false
    }*/
}