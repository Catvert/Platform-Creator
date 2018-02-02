package be.catvert.pc.components.logics

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.PhysicsAction.PhysicsActions
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import glm_.min
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
 */
private data class JumpData(var isJumping: Boolean = false, var targetHeight: Int = 0)

/**
 * Listener lors d'une collision avec une autre entité pour le mask correspondant
 */
data class CollisionListener(val gameObject: GameObject, val collideGameObject: GameObject, val side: BoxSide)


data class CollisionAction(@ExposeEditor var side: BoxSide = BoxSide.Left, @ExposeEditor(customType = CustomType.TAG_STRING) var target: GameObjectTag = Tags.Player.tag, @ExposeEditor var action: Action = EmptyAction(), @ExposeEditor(customType = CustomType.NO_CHECK_COMPS_GO) var collideAction: Action = EmptyAction())

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 * @param onLeftAction Action appelée quand le gameObject se déplace vers la gauche
 * @param onRightAction Action appelée quand le gameObject se déplace vers la droite
 * @param onDownAction Action appelée quand le gameObject est en chute libre
 * @param onNothingAction Action appelée quand le gameObject ne subit aucune action physique
 */
@Description("Ajoute des propriétés physique à un game object")
class PhysicsComponent(@ExposeEditor var isStatic: Boolean,
                       @ExposeEditor(max = 100f) var moveSpeed: Int = 0,
                       @ExposeEditor(description = "Défini si le mouvement doit être \"fluide\" ou non.") var movementType: MovementType = MovementType.SMOOTH,
                       @ExposeEditor var gravity: Boolean = !isStatic,
                       @ExposeEditor var isPlatform: Boolean = false,
                       val ignoreTags: ArrayList<GameObjectTag> = arrayListOf(),
                       val collisionsActions: ArrayList<CollisionAction> = arrayListOf(),
                       @ExposeEditor(max = 1000f) var jumpHeight: Int = 0,
                       var onLeftAction: Action = EmptyAction(),
                       var onRightAction: Action = EmptyAction(),
                       var onUpAction: Action = EmptyAction(),
                       var onDownAction: Action = EmptyAction(),
                       var onJumpAction: Action = EmptyAction(),
                       var onNothingAction: Action = EmptyAction()) : Component(), Updeatable, CustomEditorImpl, CustomEditorTextImpl {
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

    /**
     * Vitesse de la gravité, représente aussi la vitesse de saut (inversé)
     */
    private val gravitySpeed = 15f

    private lateinit var level: Level

    override fun onStateActive(gameObject: GameObject, state: GameObjectState, container: GameObjectContainer) {
        super.onStateActive(gameObject, state, container)

        level = container.cast()!!
    }

    override fun update() {
        if (isStatic) return

        var moveSpeedX = 0f
        var moveSpeedY = 0f
        var addJumpAfterClear = false

        if (gravity && gameObject.container.cast<Level>()?.applyGravity == true)
            moveSpeedY -= gravitySpeed

        physicsActions.forEach actions@ {
            when (it) {
                PhysicsActions.GO_LEFT -> moveSpeedX -= moveSpeed
                PhysicsActions.GO_RIGHT -> moveSpeedX += moveSpeed
                PhysicsActions.GO_UP -> moveSpeedY += moveSpeed
                PhysicsActions.GO_DOWN -> moveSpeedY -= moveSpeed
                PhysicsActions.JUMP, PhysicsActions.FORCE_JUMP -> {
                    if (!jumpData.isJumping) {
                        if (it != PhysicsActions.FORCE_JUMP) {
                            //  On vérifie si le gameObject est sur le sol
                            if (checkMove(0f, -1f, gameObject).none { it.second == BoxSide.Down }) {
                                return@actions
                            }
                        }
                        jumpData.isJumping = true
                        jumpData.targetHeight = gameObject.box.y.roundToInt() + jumpHeight

                        gravity = false

                        moveSpeedY = gravitySpeed
                        addJumpAfterClear = true

                        onJumpAction(gameObject)
                    } else {
                        // Vérifie si le go est arrivé à la bonne hauteur de saut ou s'il rencontre un obstacle au dessus de lui
                        if (gameObject.box.y >= jumpData.targetHeight || checkMove(0f, gravitySpeed, gameObject).any { it.second == BoxSide.Up } || gameObject.box.top() == level.matrixRect.top()) {
                            gravity = true
                            jumpData.isJumping = false
                        } else {
                            moveSpeedY = gravitySpeed
                            addJumpAfterClear = true
                        }
                    }
                }
            }
        }

        physicsActions.clear()

        // Si le déplacement est de type "fluide", on applique une interpolation linéaire à la vitesse à appliquer au gameObject
        if (movementType != MovementType.SMOOTH) {
            moveSpeedX = MathUtils.lerp(actualMoveSpeedX, moveSpeedX, 0.2f)
            moveSpeedY = MathUtils.lerp(actualMoveSpeedY, moveSpeedY, 0.2f)
        }

        actualMoveSpeedX = moveSpeedX
        actualMoveSpeedY = moveSpeedY

        val lastPos = Point(gameObject.position())

        move(moveSpeedX * Gdx.graphics.deltaTime * 60f, moveSpeedY * Gdx.graphics.deltaTime * 60f, gameObject)

        if (gameObject.position().x.equalsEpsilon(lastPos.x, Constants.physicsEpsilon) && gameObject.position().y.equalsEpsilon(lastPos.y, Constants.physicsEpsilon))
            onNothingAction(gameObject)

        if (addJumpAfterClear)
            physicsActions += PhysicsActions.JUMP
    }

    fun move(moveX: Float, moveY: Float, gameObject: GameObject) {
        val moveXCollides = checkMove(moveX, 0f, gameObject)
        val moveYCollides = checkMove(0f, moveY, gameObject)

        val targetPos = Point((gameObject.position().x + moveX).clamp(level.matrixRect.left(), level.matrixRect.right() - gameObject.box.width), (gameObject.position().y + moveY).min(level.matrixRect.top() - gameObject.box.height))

        if(moveXCollides.isEmpty()) {
            gameObject.box.x = targetPos.x

            if(gameObject.box.left() != level.matrixRect.left() && moveX < Constants.physicsEpsilon)
                onLeftAction(gameObject)
            else if(gameObject.box.right() != level.matrixRect.right() && moveX > Constants.physicsEpsilon)
                onRightAction(gameObject)
        }

        if(moveYCollides.isEmpty()) {
            gameObject.box.y = targetPos.y

            if(moveY < Constants.physicsEpsilon)
                onDownAction(gameObject)
            else if(moveY > Constants.physicsEpsilon)
                onUpAction(gameObject)
        }

        moveXCollides.forEach {
            when (it.second) {
                BoxSide.Left -> gameObject.box.x = it.first.box.right()
                BoxSide.Right -> gameObject.box.x = it.first.box.left() - gameObject.box.width
                else -> {}
            }
            triggerCollisionEvent(gameObject, it.first, it.second)
        }

        moveYCollides.forEach {
            when (it.second) {
                BoxSide.Up -> gameObject.box.y = it.first.box.bottom() - gameObject.box.height
                BoxSide.Down -> gameObject.box.y = it.first.box.top()
                else -> {}
            }
            triggerCollisionEvent(gameObject, it.first, it.second)
        }
    }

    private fun triggerCollisionEvent(gameObject: GameObject, collideGameObject: GameObject, side: BoxSide) {
        gameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == side || collisionAction.side == BoxSide.All) && collisionAction.target == collideGameObject.tag }?.apply {
                action(gameObject)
                collideAction(collideGameObject)
            }
            onCollisionWith.invoke(CollisionListener(gameObject, collideGameObject, side))
        }

        collideGameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == -side || collisionAction.side == BoxSide.All) && collisionAction.target == gameObject.tag }?.apply {
                action(collideGameObject)
                collideAction(gameObject)
            }
            onCollisionWith.invoke(CollisionListener(collideGameObject, gameObject, -side))
        }
    }

    private fun checkMove(moveX: Float, moveY: Float, gameObject: GameObject): Set<Pair<GameObject, BoxSide>> {
        val collideGameObjects = mutableSetOf<Pair<GameObject, BoxSide>>()

        val newRect = Rect(gameObject.box).apply { move(moveX, moveY) }

        level.getAllGameObjectsInCells(newRect).filter { filter ->
            filter !== gameObject && filter.getCurrentState().hasComponent<PhysicsComponent>() && let {
                filter.getCurrentState().getComponent<PhysicsComponent>()?.apply {
                    return@let ignoreTags.contains(gameObject.tag) == false
                            && if (isPlatform) {
                        gameObject.position().y >= filter.position().y + filter.size().height
                    } else true
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
                                return@forEach
                            }
                        }

                        collideGameObjects += it to side
                    }
                }

        println(collideGameObjects.size)
        return collideGameObjects
    }

    fun getCollisionsGameObjectOnSide(gameObject: GameObject, boxSide: BoxSide): Set<GameObject> {
        val collideGameObjects = mutableSetOf<GameObject>()

        level.getAllGameObjectsInCells(gameObject.box).filter { it !== gameObject }.forEach {
            when (boxSide) { // TODO epsilon?
                BoxSide.Left -> {
                    if (it.box.right() == gameObject.box.x) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Right -> {
                    if (it.box.x == gameObject.box.right()) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Up -> {
                    if (it.box.y == gameObject.box.top()) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Down -> {
                    if (it.box.top() == gameObject.box.y) {
                        collideGameObjects += it
                    }
                }
                BoxSide.All -> {
                    collideGameObjects += it
                }
            }
        }

        return collideGameObjects
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.collapsingHeader("move actions") {
            functionalProgramming.withIndent {
                ImguiHelper.action("on left", ::onLeftAction, gameObject, level, editorSceneUI)
                ImguiHelper.action("on right", ::onRightAction, gameObject, level, editorSceneUI)
                ImguiHelper.action("on up", ::onUpAction, gameObject, level, editorSceneUI)
                ImguiHelper.action("on down", ::onDownAction, gameObject, level, editorSceneUI)
                ImguiHelper.action("on nothing", ::onNothingAction, gameObject, level, editorSceneUI)
            }
        }

        functionalProgramming.collapsingHeader("ignore tags") {
            functionalProgramming.withIndent {
                ImguiHelper.addImguiWidgetsArray("ignore tags", ignoreTags, { it }, { Tags.Player.tag }, gameObject, level, editorSceneUI, ExposeEditorFactory.createExposeEditor(customType = CustomType.TAG_STRING))
            }
        }

        functionalProgramming.collapsingHeader("collisions actions") {
            functionalProgramming.withIndent {
                ImguiHelper.addImguiWidgetsArray("collisions actions", collisionsActions, { it.side.name }, { CollisionAction() }, gameObject, level, editorSceneUI)
            }
        }
    }

    override fun insertText() {
        ImguiHelper.textColored(Color.ORANGE, "collisions actions")
        collisionsActions.forEach {
            functionalProgramming.withIndent {
                ImguiHelper.textColored(Color.RED, "<-->")
                ImguiHelper.textPropertyColored(Color.ORANGE, "target :", it.target)
                ImguiHelper.textPropertyColored(Color.ORANGE, "side :", it.side)
                ImguiHelper.textPropertyColored(Color.ORANGE, "action :", it.action)
                ImguiHelper.textPropertyColored(Color.ORANGE, "collide action :", it.collideAction)
                ImguiHelper.textColored(Color.RED, "<-->")
            }
        }
    }
}