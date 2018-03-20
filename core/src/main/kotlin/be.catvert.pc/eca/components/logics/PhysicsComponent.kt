package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityState
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.actions.PhysicsAction.PhysicsActions
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import glm_.min
import imgui.ImGui
import imgui.ItemFlags
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
data class CollisionListener(val entity: Entity, val collideEntity: Entity, val side: BoxSide, val triggerCallCount: Int)


data class CollisionAction(@UI(customName = "côté") var side: BoxSide = BoxSide.Left,
                           @UI(customName = "cible", customType = CustomType.TAG_STRING) var target: EntityTag = Tags.Player.tag,
                           @UI var action: Action = EmptyAction(),
                           @UI(customName = "appliquer l'action sur la cible") var applyActionOnCollider: Boolean = false)

/**
 * Ce component permet d'ajouter à l'entité des propriétés physique tel que la gravité, vitesse de déplacement ...
 * Une entité contenant ce component ne pourra pas être traversé par une autre entité ayant également ce component
 * @param isStatic : Permet de spécifier si l'entité est sensé bougé ou non
 * @param moveSpeed : La vitesse de déplacement de l'entité qui sera utilisée avec les NextActions
 * @param movementType : Permet de définir le type de déplacement de l'entité
 * @param gravity : Permet de spécifier si la gravité est appliquée à l'entité
 * @param onLeftAction Action appelée quand le entity se déplace vers la gauche
 * @param onRightAction Action appelée quand le entity se déplace vers la droite
 * @param onDownAction Action appelée quand le entity est en chute libre
 * @param onNothingAction Action appelée quand le entity ne subit aucune action physique
 */
@Description("Ajoute des propriétés physique à une entité")
class PhysicsComponent(@UI(customName = "figée") var isStatic: Boolean,
                       @UI(customName = "vitesse", max = 100f) var moveSpeed: Int = 0,
                       @UI(customName = "déplacement", description = "Défini si le déplacement doit être \"fluide\" ou non.") var movementType: MovementType = MovementType.SMOOTH,
                       @UI(customName = "gravité") var gravity: Boolean = !isStatic,
                       @UI(customName = "est une plateforme") var isPlatform: Boolean = false,
                       val ignoreTags: ArrayList<EntityTag> = arrayListOf(),
                       val collisionsActions: ArrayList<CollisionAction> = arrayListOf(),
                       @UI(customName = "hauteur du saut", max = 1000f) var jumpHeight: Int = 0,
                       var onLeftAction: Action = EmptyAction(),
                       var onRightAction: Action = EmptyAction(),
                       var onUpAction: Action = EmptyAction(),
                       var onDownAction: Action = EmptyAction(),
                       var onJumpAction: Action = EmptyAction(),
                       var onNothingAction: Action = EmptyAction()) : Component(), Updeatable, UIImpl, UITextImpl {
    @JsonCreator private constructor() : this(true)

    /**
     * Les physicsActions représentes les actions que doit faire l'entité pendant le prochain frame
     */
    @JsonIgnore
    val physicsActions = mutableSetOf<PhysicsActions>()

    /**
     * Donnée définissant les paramètres de saut du entity
     */
    private val jumpData: JumpData = JumpData()

    /**
     * La vitesse de déplacement x actuelle de l'entité, permettant d'appliquer un lerp sur le entity si son type de déplacement est SMOOTH
     */
    private var actualMoveSpeedX = 0f

    /**
     * La vitesse de déplacement y actuelle de l'entité, permettant d'appliquer un lerp sur le entity si son type de déplacement est SMOOTH
     */
    private var actualMoveSpeedY = 0f

    /**
     * Signal appelé lorsque un entity rentre en collision avec un autre entity ayant un PhysicsComponent
     */
    @JsonIgnore
    val onCollisionWith = Signal<CollisionListener>()

    private lateinit var level: Level

    override fun onStateActive(entity: Entity, state: EntityState, container: EntityContainer) {
        super.onStateActive(entity, state, container)

        level = container.cast()!!
    }

    override fun update() {
        if (isStatic) return

        var moveSpeedX = 0f
        var moveSpeedY = 0f
        var addJumpAfterClear = false

        if (gravity && level.applyGravity)
            moveSpeedY -= level.gravitySpeed

        physicsActions.forEach actions@{
            when (it) {
                PhysicsActions.MOVE_LEFT -> moveSpeedX -= moveSpeed
                PhysicsActions.MOVE_RIGHT -> moveSpeedX += moveSpeed
                PhysicsActions.MOVE_UP -> moveSpeedY += moveSpeed
                PhysicsActions.MOVE_DOWN -> moveSpeedY -= moveSpeed
                PhysicsActions.JUMP, PhysicsActions.FORCE_JUMP -> {
                    if (level.applyGravity) {
                        if (!jumpData.isJumping) {
                            if (gravity) {
                                if (it != PhysicsActions.FORCE_JUMP) {
                                    //  On vérifie si le entity est sur le sol
                                    if (!move(false, 0f, -1f)) {
                                        return@actions
                                    }
                                }
                                jumpData.isJumping = true
                                jumpData.targetHeight = entity.box.y.roundToInt() + jumpHeight

                                gravity = false

                                moveSpeedY = level.gravitySpeed.toFloat()
                                addJumpAfterClear = true

                                if (entity.container != null)
                                    onJumpAction(entity, entity.container!!)
                            }
                        } else {
                            // Vérifie si l'entité est arrivé à la bonne hauteur de saut ou si elle rencontre un obstacle au dessus d'elle
                            if (entity.box.y >= jumpData.targetHeight || move(false, 0f, 1f) || entity.box.top() == level.matrixRect.top()) {
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

        // Si le déplacement est de type "fluide", on applique une interpolation linéaire à la vitesse à appliquer au entity
        if (movementType == MovementType.SMOOTH) {
            moveSpeedX = MathUtils.lerp(Math.round(actualMoveSpeedX / Constants.physicsEpsilon) * Constants.physicsEpsilon, moveSpeedX, 0.2f)
            moveSpeedY = MathUtils.lerp(Math.round(actualMoveSpeedY / Constants.physicsEpsilon) * Constants.physicsEpsilon, moveSpeedY, 0.2f)
        }

        actualMoveSpeedX = moveSpeedX
        actualMoveSpeedY = moveSpeedY

        val lastPos = Point(entity.position())

        move(true, moveSpeedX, moveSpeedY)

        if (entity.position().x.equalsEpsilon(lastPos.x, Constants.physicsEpsilon) && entity.position().y.equalsEpsilon(lastPos.y, Constants.physicsEpsilon) && entity.container != null)
            onNothingAction(entity, entity.container!!)

        if (addJumpAfterClear)
            physicsActions += PhysicsActions.JUMP
    }

    private fun triggerCollisionEvent(entity: Entity, collideEntity: Entity, side: BoxSide, collideIndex: Int) {
        entity.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == side || collisionAction.side == BoxSide.All) && collisionAction.target == collideEntity.tag }?.apply {
                val entity = if (applyActionOnCollider) collideEntity else entity
                if (entity.container != null)
                    action(entity, entity.container!!)
            }
            onCollisionWith.invoke(CollisionListener(entity, collideEntity, side, collideIndex))
        }

        collideEntity.getCurrentState().getComponent<PhysicsComponent>()?.apply {
            this.collisionsActions.firstOrNull { collisionAction -> (collisionAction.side == -side || collisionAction.side == BoxSide.All) && collisionAction.target == entity.tag }?.apply {
                val entity = if (applyActionOnCollider) entity else collideEntity
                if (entity.container != null)
                    action(entity, entity.container!!)
            }
            onCollisionWith.invoke(CollisionListener(collideEntity, entity, -side, collideIndex))
        }
    }

    fun move(move: Boolean, targetMoveX: Float, targetMoveY: Float): Boolean {
        var moveX = 0f
        var moveY = 0f

        var returnCollide = false

        val potentialCollideEntities = level.getAllEntitiesInCells(entity.box.merge(Rect(entity.box).apply { move(targetMoveX, targetMoveY) })).filter { filter ->
            filter !== entity && filter.getCurrentState().hasComponent<PhysicsComponent>() && let {
                filter.getCurrentState().getComponent<PhysicsComponent>()?.also { filterComp ->
                    return@let !filterComp.ignoreTags.contains(entity.tag) && !this.ignoreTags.contains(filter.tag)
                            && if (filterComp.isPlatform) {
                        entity.position().y >= filter.position().y + filter.size().height
                    } else true
                }
                true
            }
        }

        val checkRect = Rect(entity.box)
        var triggerCallCounter = 0
        // Do-while à la place de while pour pouvoir vérifier si l'entité n'est pas déjà en overlaps avec une autre (mal placé dans l'éditeur)
        do {
            checkRect.y = entity.box.y + moveY + Math.signum(targetMoveY) * Constants.physicsEpsilon

            var collide = false
            potentialCollideEntities.forEach {
                if (checkRect.overlaps(it.box)) {
                    val side = when {
                        moveY > 0 -> BoxSide.Up
                        moveY < 0 -> BoxSide.Down
                        else -> {

                            entity.box.y = (it.box.top() + Constants.physicsEpsilon).min(level.matrixRect.top() - entity.box.height)
                            return@forEach
                        }
                    }

                    collide = true

                    triggerCollisionEvent(entity, it, side, triggerCallCounter++)
                }
            }

            if (!collide && Math.abs(moveY - targetMoveY) > Constants.physicsEpsilon)
                moveY += Math.signum(targetMoveY) * Constants.physicsEpsilon
            else {
                if (!collide && entity.container != null) {
                    if (moveY > 0 && entity.box.top() != level.matrixRect.top())
                        onUpAction(entity, entity.container!!)
                    else if (moveY < 0)
                        onDownAction(entity, entity.container!!)
                } else
                    returnCollide = true

                if (move) {
                    entity.box.y = (entity.box.y + moveY - Math.signum(targetMoveY) * Constants.physicsEpsilon).min(level.matrixRect.top() - entity.box.height)
                }
                checkRect.y = entity.box.y
                break
            }
        } while (Math.abs(moveY) < Math.abs(targetMoveY))

        while (Math.abs(moveX) < Math.abs(targetMoveX)) {
            checkRect.x = entity.box.x + moveX + Math.signum(targetMoveX) * Constants.physicsEpsilon

            var collide = false
            potentialCollideEntities.forEach {
                if (checkRect.overlaps(it.box)) {
                    val side = when {
                        moveX > 0 -> BoxSide.Right
                        moveX < 0 -> BoxSide.Left
                        else -> {
                            return@forEach
                        }
                    }

                    collide = true

                    triggerCollisionEvent(entity, it, side, triggerCallCounter++)
                }
            }

            if (!collide && Math.abs(moveX - targetMoveX) > Constants.physicsEpsilon)
                moveX += Math.signum(targetMoveX) * Constants.physicsEpsilon
            else {
                if (!collide && entity.container != null) {
                    if (moveX > 0 && entity.box.right() != level.matrixRect.right())
                        onRightAction(entity, entity.container!!)
                    else if (moveX < 0 && entity.box.left() != level.matrixRect.left())
                        onLeftAction(entity, entity.container!!)
                } else
                    returnCollide = true

                if (move)
                    entity.box.x = (entity.box.x + moveX - Math.signum(targetMoveX) * Constants.physicsEpsilon).clamp(level.matrixRect.left(), level.matrixRect.right() - entity.box.width)
                checkRect.x = entity.box.x
                break
            }
        }

        return returnCollide
    }

    // TODO refactor + ignore tags
    fun getCollideEntitiesOnSide(entity: Entity, boxSide: BoxSide, epsilon: Float = Constants.physicsEpsilon): Set<Entity> {
        val collideEntities = mutableSetOf<Entity>()
        level.getAllEntitiesInCells(entity.box).filter { it !== entity }.forEach {
            when (boxSide) {
                BoxSide.Left -> {
                    if ((entity.box.bottom() in it.box.bottom()..it.box.top()
                                    || entity.box.top() in it.box.bottom()..it.box.top()
                                    || it.box.bottom() in entity.box.bottom()..entity.box.top()
                                    || it.box.top() in entity.box.bottom()..entity.box.top())
                            && it.box.right().equalsEpsilon(entity.box.left(), epsilon)) {
                        collideEntities += it
                    }
                }
                BoxSide.Right -> {
                    if ((entity.box.bottom() in it.box.bottom()..it.box.top()
                                    || entity.box.top() in it.box.bottom()..it.box.top()
                                    || it.box.bottom() in entity.box.bottom()..entity.box.top()
                                    || it.box.top() in entity.box.bottom()..entity.box.top())
                            && it.box.x.equalsEpsilon(entity.box.right(), epsilon)) {
                        collideEntities += it
                    }
                }
                BoxSide.Up -> {
                    if ((entity.box.left() in it.box.left()..it.box.right()
                                    || entity.box.right() in it.box.left()..it.box.right()
                                    || it.box.left() in entity.box.left()..entity.box.right()
                                    || it.box.right() in entity.box.left()..entity.box.right())
                            && it.box.y.equalsEpsilon(entity.box.top(), epsilon))
                        collideEntities += it
                }
                BoxSide.Down -> {
                    if ((entity.box.left() in it.box.left()..it.box.right()
                                    || entity.box.right() in it.box.left()..it.box.right()
                                    || it.box.left() in entity.box.left()..entity.box.right()
                                    || it.box.right() in entity.box.left()..entity.box.right())
                            && it.box.top().equalsEpsilon(entity.box.bottom(), epsilon))
                        collideEntities += it
                }
                BoxSide.All -> {
                    collideEntities += getCollideEntitiesOnSide(entity, BoxSide.Left, epsilon)
                    collideEntities += getCollideEntitiesOnSide(entity, BoxSide.Right, epsilon)
                    collideEntities += getCollideEntitiesOnSide(entity, BoxSide.Up, epsilon)
                    collideEntities += getCollideEntitiesOnSide(entity, BoxSide.Down, epsilon)
                }
            }
        }

        return collideEntities
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGui.pushItemFlag(ItemFlags.Disabled.i, isStatic)
        functionalProgramming.collapsingHeader("actions de déplacement") {
            functionalProgramming.withIndent {
                ImGuiHelper.action("à gauche", ::onLeftAction, entity, level, editorUI)
                ImGuiHelper.action("à droite", ::onRightAction, entity, level, editorUI)
                ImGuiHelper.action("au-dessus", ::onUpAction, entity, level, editorUI)
                ImGuiHelper.action("en-dessous", ::onDownAction, entity, level, editorUI)
                ImGuiHelper.action("au saut", ::onJumpAction, entity, level, editorUI)
                ImGuiHelper.action("rien", ::onNothingAction, entity, level, editorUI)
            }
        }
        ImGui.popItemFlag()

        if (ImGui.isItemHovered() && isStatic) {
            functionalProgramming.withTooltip {
                ImGui.text("Impossible d'utiliser des actions liés aux mouvements si l'entité est figée.")
            }
        }

        functionalProgramming.collapsingHeader("tags ignorés") {
            functionalProgramming.withIndent {
                ImGuiHelper.addImguiWidgetsArray("tags ignorés", ignoreTags, { it }, { Tags.Player.tag }, entity, level, editorUI, UIFactory.createUI(customType = CustomType.TAG_STRING))
            }
        }

        functionalProgramming.collapsingHeader("actions de collision") {
            functionalProgramming.withIndent {
                ImGuiHelper.addImguiWidgetsArray("collide actions", collisionsActions, { it.side.name }, { CollisionAction() }, entity, level, editorUI)
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
