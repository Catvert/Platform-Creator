package be.catvert.mtrktx.ecs.systems.physics

import be.catvert.mtrktx.Level
import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.BaseComponent
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.EntityEventSystem
import be.catvert.mtrktx.matrix2d
import com.badlogic.ashley.core.*
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import ktx.app.use

/**
 * Created by arno on 04/06/17.
 */

class PhysicsSystem(private val game: MtrGame, private val level: Level, private val camera: Camera, val gravity: Int = 5) : EntityEventSystem() {
    private val shapeRenderer = ShapeRenderer()

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)

    private lateinit var entities: ImmutableArray<Entity>

    private val sizeGridCell = 200f
    private val matrixSizeX = 300
    private val matrixSizeY = 100
    private val matrixRect = Rectangle(0f, 0f, sizeGridCell * matrixSizeX, sizeGridCell * matrixSizeY)
    private val matrixGrid = matrix2d(matrixSizeX, matrixSizeY, { row: Int, width: Int -> Array(width) { col -> Pair(mutableListOf<Entity>(), Rectangle(row.toFloat() * sizeGridCell, col.toFloat() * sizeGridCell, sizeGridCell, sizeGridCell)) } })

    private val activeGridCells = mutableListOf<GridCell>()

    private val playerRect = Rectangle(level.player.getComponent(TransformComponent::class.java).rectangle)

    var drawDebugCells = false

    init {
        playerRect.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        level.loadedEntities.forEach {
            setEntityGrid(it) // Besoin de setGrid car les entités n'ont pas encore été ajoutée à la matrix
            if (it == level.player)
                return@forEach
            it.components.filter { c -> c is BaseComponent }.forEach {
                (it as BaseComponent).active = false
            }
        }
    }

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
        entities = getEngine().getEntitiesFor(Family.all(PhysicsComponent::class.java, TransformComponent::class.java).get())
    }

    override fun onEntityAdded(entity: Entity) { // Appelé lors de l'ajout d'entité APRES le chargement du niveau
        if (entities.contains(entity)) {
            setEntityGrid(entity)
        }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        if (drawDebugCells) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            matrixGrid.forEachIndexed { x, it ->
                it.forEachIndexed { y, it ->
                    shapeRenderer.rect(it.second.x, it.second.y, it.second.width, it.second.height)
                }
            }
            shapeRenderer.rect(playerRect.x, playerRect.y, playerRect.width, playerRect.height)
            shapeRenderer.end()
        }

        for (i in 0..entities.size() - 1) {
            val entity = entities[i]
            val physicsComp = physicsMapper[entity]
            val transformComp = transformMapper[entity]

            if (physicsComp.isStatic || !physicsComp.active)
                continue

            if (physicsComp.gravity)
                physicsComp.nextActions += PhysicsComponent.NextActions.GRAVITY

            var moveSpeedX = 0
            var moveSpeedY = 0
            physicsComp.nextActions.forEach {
                when (it) {
                    PhysicsComponent.NextActions.GO_LEFT -> moveSpeedX -= physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_RIGHT -> moveSpeedX += physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_UP -> moveSpeedY += physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GO_DOWN -> moveSpeedY -= physicsComp.moveSpeed
                    PhysicsComponent.NextActions.GRAVITY -> moveSpeedY -= gravity
                }
            }
            if (physicsComp.smoothMove != null) { // Smooth mode
                physicsComp.smoothMove.targetMoveSpeedX = moveSpeedX
                physicsComp.smoothMove.targetMoveSpeedY = moveSpeedY

                physicsComp.smoothMove.actualMoveSpeedX = MathUtils.lerp(physicsComp.smoothMove.actualMoveSpeedX, physicsComp.smoothMove.targetMoveSpeedX.toFloat(), 0.5f)
                physicsComp.smoothMove.actualMoveSpeedY = MathUtils.lerp(physicsComp.smoothMove.actualMoveSpeedY, physicsComp.smoothMove.targetMoveSpeedY.toFloat(), 0.5f)

                tryMove(physicsComp.smoothMove.actualMoveSpeedX.toInt(), physicsComp.smoothMove.actualMoveSpeedY.toInt(), entity, transformComp)

            } else { // NO smooth mode
                tryMove(moveSpeedX, moveSpeedY, entity, transformComp)
            }

            if (entity == level.player) {
                playerRect.setPosition(Math.max(0f, transformComp.rectangle.x - playerRect.width / 2 + transformComp.rectangle.width / 2), Math.max(0f, transformComp.rectangle.y - playerRect.height / 2 + transformComp.rectangle.height / 2))
                updateActiveCells()
            }

            physicsComp.nextActions.clear()
        }
    }

    private fun tryMove(moveX: Int, moveY: Int, entity: Entity, transformTarget: TransformComponent) {
        if (moveX != 0 || moveY != 0) {
            var newMoveX = moveX
            var newMoveY = moveY

            if (!collideOnMove(moveX, 0, entity)) {
                transformTarget.rectangle.x = Math.max(0f, transformTarget.rectangle.x + moveX)
                setEntityGrid(entity)
                newMoveX = 0
            }
            if (!collideOnMove(0, moveY, entity)) {
                transformTarget.rectangle.y = Math.max(0f, transformTarget.rectangle.y + moveY)
                setEntityGrid(entity)
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

            tryMove(newMoveX, newMoveY, entity, transformTarget)
        }
    }

    private fun collideOnMove(moveX: Int, moveY: Int, entity: Entity): Boolean {
        val transformTarget = transformMapper[entity]

        val newRect = Rectangle(transformTarget.rectangle)
        newRect.setPosition(newRect.x + moveX, newRect.y + moveY)

        getRectCells(newRect).forEach {
            matrixGrid[it.x][it.y].first.forEach matrixLoop@ {
                val transformComponent = transformMapper[it]

                if (transformComponent == transformTarget)
                    return@matrixLoop

                if (newRect.overlaps(transformComponent.rectangle))
                    return true
            }
        }

        return false
    }

    private fun getRectCells(rect: Rectangle): List<GridCell> {
        val cells = mutableListOf<GridCell>()

        fun rectContains(x: Int, y: Int): Boolean {
            if ((x < 0 || y < 0) || (x >= matrixGrid.size || y >= matrixGrid.size)) {
                return false
            }

            return rect.overlaps(matrixGrid[x][y].second)
        }
        if (matrixRect.overlaps(rect)) {
            var x = Math.max(0f, rect.x / sizeGridCell).toInt()
            var y = Math.max(0f, rect.y / sizeGridCell).toInt()
            if (rectContains(x, y)) {
                cells += GridCell(x, y)
                val firstXCell = x
                do {
                    if (rectContains(x + 1, y)) {
                        ++x
                        cells += GridCell(x, y)
                        continue
                    } else {
                        x = firstXCell
                    }

                    if (rectContains(x, y + 1)) {
                        ++y
                        cells += GridCell(x, y)
                    } else
                        break
                } while (true)
            }
        }

        return cells
    }

    private fun setEntityGrid(entity: Entity) {
        val transformComp = transformMapper[entity]
        val physicsComp = physicsMapper[entity]

        fun addEntityTo(cell: GridCell) {
            matrixGrid[cell.x][cell.y].first.add(entity)
            physicsComp.gridCell.add(cell)
        }

        physicsComp.gridCell.forEach {
            matrixGrid[it.x][it.y].first.remove(entity)
        }

        physicsComp.gridCell.clear()

        getRectCells(transformComp.rectangle).forEach {
            addEntityTo(it)
        }

    }

    private fun updateActiveCells() {
        activeGridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach matrix@ {
                if (it == level.player)
                    return@matrix
                it.components.filter { c -> c is BaseComponent }.forEach {
                    (it as BaseComponent).active = false
                }
            }
        }

        activeGridCells.clear()

        activeGridCells.addAll(getRectCells(playerRect))

        activeGridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach {
                it.components.filter { c -> c is BaseComponent }.forEach {
                    (it as BaseComponent).active = true
                }
            }
        }
    }
}