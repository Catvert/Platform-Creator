package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.GameObjectTag
import be.catvert.pc.Tags
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.PhysicsAction.PhysicsActions
import be.catvert.pc.components.Component
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import glm_.min
import imgui.ImGui
import imgui.ItemFlags
import imgui.functionalProgramming
import java.awt.SystemColor.text
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
data class CollisionListener(val gameObject: GameObject, val collideGameObject: GameObject, val side: BoxSide, val triggerCallCount: Int)


data class CollisionAction(@ExposeEditor var side: BoxSide = BoxSide.Left, @ExposeEditor(customType = CustomType.TAG_STRING) var target: GameObjectTag = Tags.Player.tag, @ExposeEditor var action: Action = EmptyAction())

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

        if (gravity && level.applyGravity)
            moveSpeedY -= level.gravitySpeed

        physicsActions.forEach actions@ {
            when (it) {
                PhysicsActions.GO_LEFT -> moveSpeedX -= moveSpeed
                PhysicsActions.GO_RIGHT -> moveSpeedX += moveSpeed
                PhysicsActions.GO_UP -> moveSpeedY += moveSpeed
                PhysicsActions.GO_DOWN -> moveSpeedY -= moveSpeed
                PhysicsActions.JUMP, PhysicsActions.FORCE_JUMP -> {
                    if (level.applyGravity) {
                        if (!jumpData.isJumping) {
                            if(gravity) {
                                if (it != PhysicsActions.FORCE_JUMP) {
                                    //  On vérifie si le gameObject est sur le sol
                                    if (!move(false, 0f, -1f, gameObject)) {
                                        return@actions
                                    }
                                }
                                jumpData.isJumping = true
                                jumpData.targetHeight = gameObject.box.y.roundToInt() + jumpHeight

                                gravity = false

                                moveSpeedY = level.gravitySpeed.toFloat()
                                addJumpAfterClear = true

                                onJumpAction(gameObject)
                            }
                        } else {
                            // Vérifie si le go est arrivé à la bonne hauteur de saut ou s'il rencontre un obstacle au dessus de lui
                            if (gameObject.box.y >= jumpData.targetHeight || move(false, 0f, 1f, gameObject) || gameObject.box.top() == level.matrixRect.top()) {
                                gravity = true
                                jumpData.isJumping = false
                            } else {
                                moveSpeedY = level.gravitySpeed.toFloat()
                                addJumpAfterClear = true
                            }
                        }
                    }
                }
            }
        }

        physicsActions.clear()

        moveSpeedX *= Utility.getDeltaTime() * Constants.physicsDeltaSpeed
        moveSpeedY *= Utility.getDeltaTime() * Constants.physicsDeltaSpeed

        // Si le déplacement est de type "fluide", on applique une interpolation linéaire à la vitesse à appliquer au gameObject
        if (movementType == MovementType.SMOOTH) {
            moveSpeedX = MathUtils.lerp(Math.round(actualMoveSpeedX / Constants.physicsEpsilon) * Constants.physicsEpsilon, moveSpeedX, 0.2f)
            moveSpeedY = MathUtils.lerp(Math.round(actualMoveSpeedY / Constants.physicsEpsilon) * Constants.physicsEpsilon, moveSpeedY, 0.2f)
        }

        actualMoveSpeedX = moveSpeedX
        actualMoveSpeedY = moveSpeedY

        val lastPos = Point(gameObject.position())

        move(true, moveSpeedX, moveSpeedY, gameObject)

        if (gameObject.position().x.equalsEpsilon(lastPos.x, Constants.physicsEpsilon) && gameObject.position().y.equalsEpsilon(lastPos.y, Constants.physicsEpsilon))
            onNothingAction(gameObject)

        if (addJumpAfterClear)
            physicsActions += PhysicsActions.JUMP
    }

    private fun triggerCollisionEvent(gameObject: GameObject, collideGameObject: GameObject, side: BoxSide, collideIndex: Int) {
        gameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == side || collisionAction.side == BoxSide.All) && collisionAction.target == collideGameObject.tag }?.apply {
                action(gameObject)
            }
            onCollisionWith.invoke(CollisionListener(gameObject, collideGameObject, side, collideIndex))
        }

        collideGameObject.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == -side || collisionAction.side == BoxSide.All) && collisionAction.target == gameObject.tag }?.apply {
                action(collideGameObject)
            }
            onCollisionWith.invoke(CollisionListener(collideGameObject, gameObject, -side, collideIndex))
        }
    }

    fun move(move: Boolean, targetMoveX: Float, targetMoveY: Float, gameObject: GameObject): Boolean {
        var moveX = 0f
        var moveY = 0f

        var returnCollide = false

        val potentialCollideGameObjects = level.getAllGameObjectsInCells(gameObject.box.merge(Rect(gameObject.box).apply { move(targetMoveX, targetMoveY) })).filter { filter ->
            filter !== gameObject && filter.getCurrentState().hasComponent<PhysicsComponent>() && let {
                filter.getCurrentState().getComponent<PhysicsComponent>()?.apply {
                    return@let ignoreTags.contains(gameObject.tag) == false
                            && if (isPlatform) {
                        gameObject.position().y >= filter.position().y + filter.size().height
                    } else true
                }
                true
            }
        }

        val checkRect = Rect(gameObject.box)
        var triggerCallCounter = 0
        // Do-while à la place de while pour pouvoir vérifier si le game object n'est pas déjà en overlaps avec un autre (mal placé dans l'éditeur)
        do {
            checkRect.y = gameObject.box.y + moveY + Math.signum(targetMoveY) * Constants.physicsEpsilon

            var collide = false
            potentialCollideGameObjects.forEach {
                if (checkRect.overlaps(it.box)) {
                    val side = when {
                        moveY > 0 -> BoxSide.Up
                        moveY < 0 -> BoxSide.Down
                        else -> {
                            gameObject.box.y = (it.box.top() + Constants.physicsEpsilon).min(level.matrixRect.top() - gameObject.box.height)
                            return@forEach
                        }
                    }

                    collide = true

                    triggerCollisionEvent(gameObject, it, side, triggerCallCounter++)
                }
            }

            if (!collide && Math.abs(moveY - targetMoveY) > Constants.physicsEpsilon)
                moveY += Math.signum(targetMoveY) * Constants.physicsEpsilon
            else {
                if (!collide) {
                    if (moveY > 0 && gameObject.box.top() != level.matrixRect.top())
                        onUpAction(gameObject)
                    else if (moveY < 0)
                        onDownAction(gameObject)
                } else
                    returnCollide = true

                if (move) {
                    gameObject.box.y = (gameObject.box.y + moveY - Math.signum(targetMoveY) * Constants.physicsEpsilon).min(level.matrixRect.top() - gameObject.box.height)
                }
                checkRect.y = gameObject.box.y
                break
            }
        } while (Math.abs(moveY) < Math.abs(targetMoveY))

        while (Math.abs(moveX) < Math.abs(targetMoveX)) {
            checkRect.x = gameObject.box.x + moveX + Math.signum(targetMoveX) * Constants.physicsEpsilon

            var collide = false
            potentialCollideGameObjects.forEach {
                if (checkRect.overlaps(it.box)) {
                    val side = when {
                        moveX > 0 -> BoxSide.Right
                        moveX < 0 -> BoxSide.Left
                        else -> {
                            return@forEach
                        }
                    }

                    collide = true

                    triggerCollisionEvent(gameObject, it, side, triggerCallCounter++)
                }
            }

            if (!collide && Math.abs(moveX - targetMoveX) > Constants.physicsEpsilon)
                moveX += Math.signum(targetMoveX) * Constants.physicsEpsilon
            else {
                if (!collide) {
                    if (moveX > 0 && gameObject.box.right() != level.matrixRect.right())
                        onRightAction(gameObject)
                    else if (moveX < 0 && gameObject.box.left() != level.matrixRect.left())
                        onLeftAction(gameObject)
                } else
                    returnCollide = true

                if (move)
                    gameObject.box.x = (gameObject.box.x + moveX - Math.signum(targetMoveX) * Constants.physicsEpsilon).clamp(level.matrixRect.left(), level.matrixRect.right() - gameObject.box.width)
                checkRect.x = gameObject.box.x
                break
            }
        }

        return returnCollide
    }

    fun getCollisionsGameObjectOnSide(gameObject: GameObject, boxSide: BoxSide, epsilon: Float = Constants.physicsEpsilon): Set<GameObject> {
        val collideGameObjects = mutableSetOf<GameObject>()

        level.getAllGameObjectsInCells(gameObject.box).filter { it !== gameObject }.forEach {
            when (boxSide) {
                BoxSide.Left -> {
                    if (it.box.right().equalsEpsilon(gameObject.box.left(), epsilon)) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Right -> {
                    if (it.box.x.equalsEpsilon(gameObject.box.right(), epsilon)) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Up -> {
                    if (it.box.y.equalsEpsilon(gameObject.box.top(), epsilon)) {
                        collideGameObjects += it
                    }
                }
                BoxSide.Down -> {
                    if (it.box.top().equalsEpsilon(gameObject.box.bottom(), epsilon)) {
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
        ImGui.pushItemFlag(ItemFlags.Disabled.i, isStatic)
        functionalProgramming.collapsingHeader("move actions") {
            functionalProgramming.withIndent {
                ImGuiHelper.action("on left", ::onLeftAction, gameObject, level, editorSceneUI)
                ImGuiHelper.action("on right", ::onRightAction, gameObject, level, editorSceneUI)
                ImGuiHelper.action("on up", ::onUpAction, gameObject, level, editorSceneUI)
                ImGuiHelper.action("on down", ::onDownAction, gameObject, level, editorSceneUI)
                ImGuiHelper.action("on jump", ::onJumpAction, gameObject, level, editorSceneUI)
                ImGuiHelper.action("on nothing", ::onNothingAction, gameObject, level, editorSceneUI)
            }
        }
        ImGui.popItemFlag()

        if(ImGui.isItemHovered() && isStatic) {
            functionalProgramming.withTooltip {
                ImGui.text("Impossible d'utiliser des actions liés aux mouvements si le game object est statique.")
            }
        }

        functionalProgramming.collapsingHeader("ignore tags") {
            functionalProgramming.withIndent {
                ImGuiHelper.addImguiWidgetsArray("ignore tags", ignoreTags, { it }, { Tags.Player.tag }, gameObject, level, editorSceneUI, ExposeEditorFactory.createExposeEditor(customType = CustomType.TAG_STRING))
            }
        }

        functionalProgramming.collapsingHeader("collisions actions") {
            functionalProgramming.withIndent {
                ImGuiHelper.addImguiWidgetsArray("collisions actions", collisionsActions, { it.side.name }, { CollisionAction() }, gameObject, level, editorSceneUI)
            }
        }
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.ORANGE, "collisions actions")
        collisionsActions.forEach {
            functionalProgramming.withIndent {
                ImGuiHelper.textColored(Color.RED, "<-->")
                ImGuiHelper.textPropertyColored(Color.ORANGE, "target :", it.target)
                ImGuiHelper.textPropertyColored(Color.ORANGE, "side :", it.side)
                ImGuiHelper.textPropertyColored(Color.ORANGE, "action :", it.action)
                ImGuiHelper.textColored(Color.RED, "<-->")
            }
        }
    }
}