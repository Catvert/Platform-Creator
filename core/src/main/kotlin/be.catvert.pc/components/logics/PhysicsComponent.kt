package be.catvert.pc.components.logics

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.PhysicsAction
import be.catvert.pc.actions.PhysicsAction.PhysicsActions
import be.catvert.pc.components.LogicsComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.*
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import imgui.ImGui
import imgui.functionalProgramming
import kotlin.math.roundToInt

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
 * Listener lors d'une collision avec une autre entité pour le mask correspondant
 */
data class CollisionListener(val gameObject: GameObject, val collideGameObject: GameObject, val side: BoxSide)

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 * @param maskCollision Masque à appliquer au gameObject
 * @param onLeftAction Action appelée quand le gameObject se déplace vers la gauche
 * @param onRightAction Action appelée quand le gameObject se déplace vers la droite
 * @param onFallAction Action appelée quand le gameObject est en chute libre
 * @param onNothingAction Action appelée quand le gameObject ne subit aucune action physique
 */
class PhysicsComponent(@ExposeEditor var isStatic: Boolean,
                       @ExposeEditor(max = 100) var moveSpeed: Int = 0,
                       var isSensor: Boolean = false,
                       @ExposeEditor var movementType: MovementType = MovementType.LINEAR,
                       @ExposeEditor var gravity: Boolean = !isStatic,
                       var ignoreTags: ArrayList<GameObjectTag> = arrayListOf(),
                       @ExposeEditor(max = 1000) var jumpHeight: Int = 0,
                       var onLeftAction: Action = EmptyAction(),
                       var onRightAction: Action = EmptyAction(),
                       var onJumpAction: Action = EmptyAction(),
                       var onFallAction: Action = EmptyAction(),
                       var onNothingAction: Action = EmptyAction(),
                       var onSensorIn: Action = EmptyAction(),
                       var onSensorOut: Action = EmptyAction()) : LogicsComponent(), CustomEditorImpl {
    @JsonCreator private constructor() : this(true)

    /**
     * Les physicsActions représentes les actions que doit faire l'entité pendant le prochain frame
     */
    @JsonIgnore
    val physicsActions = mutableSetOf<PhysicsActions>()

    /**
     * Donnée définissant les paramètres de saut du gameObject
     */
    private val jumpData: JumpData = JumpData()

    /**
     * La vitesse de déplacement x actuelle de l'entité, permettant d'appliquer un lerp sur le gameObject si son type de déplacement est SMOOTH
     */
    private var actualMoveSpeedX = 0f

    /**
     * La vitesse de déplacement y actuelle de l'entité, permettant d'appliquer un lerp sur le gameObject si son type de déplacement est SMOOTH
     */
    private var actualMoveSpeedY = 0f

    /**
     * Signal appelé lorsque un gameObject rentre en collision avec un autre gameObject ayant un PhysicsComponent
     */
    @JsonIgnore
    val onCollisionWith = Signal<CollisionListener>()

    private val sensorOverlaps = mutableSetOf<GameObject>()

    /**
     * Vitesse de la gravité, représente aussi la vitesse de saut (inversé)
     */
    private val gravitySpeed = 15

    private var level: Level? = null

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        level = container.cast()
    }

    override fun update(gameObject: GameObject) {
        if (isSensor)
            checkSensorOverlaps(gameObject)
        if (isStatic || isSensor) return

        if (physicsActions.isEmpty())
            onNothingAction(gameObject)

        if (jumpData.forceJumping) {
            physicsActions += PhysicsAction.PhysicsActions.JUMP
        }

        var moveSpeedX = 0f
        var moveSpeedY = 0f
        var addJumpAfterClear = false

        if (gravity && gameObject.container.cast<Level>()?.applyGravity == true)
            moveSpeedY -= gravitySpeed

        physicsActions.forEach action@ {
            when (it) {
                PhysicsActions.GO_LEFT -> moveSpeedX -= moveSpeed
                PhysicsActions.GO_RIGHT -> moveSpeedX += moveSpeed
                PhysicsActions.GO_UP -> moveSpeedY += moveSpeed
                PhysicsActions.GO_DOWN -> moveSpeedY -= moveSpeed
                PhysicsActions.JUMP -> {
                    if (!jumpData.isJumping) {
                        if (!jumpData.forceJumping) {
                            //  On vérifie si le gameObject est sur le sol
                            if (!collideOnMove(0, -1, gameObject)) {
                                return@action
                            }
                        } else {
                            jumpData.forceJumping = false
                        }
                        jumpData.isJumping = true
                        jumpData.targetHeight = gameObject.box.y + jumpHeight
                        jumpData.startJumping = true

                        gravity = false

                        moveSpeedY = gravitySpeed.toFloat()
                        addJumpAfterClear = true

                        onJumpAction(gameObject)
                    } else {
                        // Vérifie si le go est arrivé à la bonne hauteur de saut ou s'il rencontre un obstacle au dessus de lui
                        if (gameObject.box.y >= jumpData.targetHeight || collideOnMove(0, gravitySpeed, gameObject)) {
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

        // Si le déplacement est de type "fluide", on applique une interpolation linéaire à la vitesse à appliquer au gameObject
        if (movementType == MovementType.SMOOTH) {
            moveSpeedX = MathUtils.lerp(actualMoveSpeedX, moveSpeedX, 0.2f)
            moveSpeedY = MathUtils.lerp(actualMoveSpeedY, moveSpeedY, 0.2f)
        }

        actualMoveSpeedX = moveSpeedX
        actualMoveSpeedY = moveSpeedY

        tryMove(moveSpeedX.roundToInt(), moveSpeedY.roundToInt(), gameObject)

        physicsActions.clear()

        if (addJumpAfterClear)
            physicsActions += PhysicsActions.JUMP
    }

    private fun checkSensorOverlaps(gameObject: GameObject) {
        val checkedGameObject = mutableSetOf<GameObject>()

        level?.getAllGameObjectsInCells(gameObject.box)?.filter { it !== gameObject && gameObject.box.overlaps(it.box) }?.forEach {
            if (!sensorOverlaps.contains(it)) {
                onSensorIn(gameObject)
                sensorOverlaps += it
            }

            checkedGameObject += it
        }

        sensorOverlaps.filter { !checkedGameObject.contains(it) }.forEach {
            onSensorOut(gameObject)
            sensorOverlaps.remove(it)
        }
    }


    /**
     * Permet d'essayer le déplacement physique d'un gameObject si il ne rencontre aucun obstacle
     */
    fun tryMove(moveX: Int, moveY: Int, gameObject: GameObject) {
        if (moveX != 0 || moveY != 0) {
            var newMoveX = moveX
            var newMoveY = moveY

            if (!collideOnMove(moveX, 0, gameObject)) {
                gameObject.box.x += moveX

                if (gameObject.box.x != 0 && moveX < 0)
                    onLeftAction(gameObject)
                else if (gameObject.box.x != level?.matrixRect?.width?.minus(gameObject.box.width) && moveX > 0)
                    onRightAction(gameObject)

                newMoveX = 0
            }
            if (!collideOnMove(0, moveY, gameObject)) {
                gameObject.box.y += moveY

                if (moveY < 0)
                    onFallAction(gameObject)

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
     * Permet de tester si un déplacement physique est possible ou non
     */
    fun collideOnMove(moveX: Int, moveY: Int, gameObject: GameObject): Boolean {
        val newRect = Rect(gameObject.box)
        newRect.position = Point(newRect.x + moveX, newRect.y + moveY)

        if (level?.matrixRect?.contains(newRect) == false)
            return true

        level?.apply {
            this.getAllGameObjectsInCells(newRect).filter { filter ->
                filter !== gameObject && let {
                    filter.getCurrentState().getComponent<PhysicsComponent>()?.apply {
                        return@let !isSensor && ignoreTags.contains(gameObject.tag) == false
                    }
                    true
                }
            }.forEach {
                if (newRect.overlaps(it.box)) {
                    val side = when {
                        moveX > 0 -> BoxSide.Right
                        moveX < 0 -> BoxSide.Left
                        moveY > 0 -> BoxSide.Up
                        moveY < 0 -> BoxSide.Down
                        else -> {
                            Log.warn { "Collision invalide !" }
                            BoxSide.Left
                        }
                    }

                    gameObject.getCurrentState().getComponent<PhysicsComponent>()?.onCollisionWith?.invoke(CollisionListener(gameObject, it, side))
                    it.getCurrentState().getComponent<PhysicsComponent>()?.onCollisionWith?.invoke(CollisionListener(it, gameObject, -side))

                    return true
                }
            }
        }
        return false
    }

    fun getCollisionsGameObjectOnSide(gameObject: GameObject, boxSide: BoxSide): Set<GameObject> {
        val collideGameObjects = mutableSetOf<GameObject>()

        level?.getAllGameObjectsInCells(gameObject.box)?.filter { it !== gameObject }?.forEach {
            when (boxSide) {
                BoxSide.Left -> {
                    if (it.box.x + it.box.width == gameObject.box.x) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Right -> {
                    if (it.box.x == gameObject.box.x + gameObject.box.width) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Up -> {
                    if (it.box.y == gameObject.box.y + gameObject.box.height) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Down -> {
                    if (it.box.y + it.box.height == gameObject.box.y) {
                        collideGameObjects += it
                    }
                }
            }
        }

        return collideGameObjects
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        functionalProgramming.collapsingHeader("move actions") {
            ImguiHelper.action("left action", ::onLeftAction, gameObject, level)
            ImguiHelper.action("right action", ::onRightAction, gameObject, level)
            ImguiHelper.action("jump action", ::onLeftAction, gameObject, level)
            ImguiHelper.action("fall action", ::onLeftAction, gameObject, level)
           // ImguiHelper.action("up action", ::onUpAction, gameObject, level)
           // ImguiHelper.action("down action", ::onDownAction, gameObject, level)
            ImguiHelper.action("nothing action", ::onNothingAction, gameObject, level)
        }

        ImGui.checkbox("sensor", ::isSensor)
        if(isSensor) {
            functionalProgramming.collapsingHeader("sensor props") {
                ImguiHelper.action("in action", ::onSensorIn, gameObject, level)
                ImguiHelper.action("out action", ::onSensorOut, gameObject, level)
            }
        }

        ImguiHelper.addImguiWidgetsArray("ignore tags", ignoreTags, { Tags.Player.tag }, {
            ImguiHelper.gameObjectTag(it, level)
        })
    }
}