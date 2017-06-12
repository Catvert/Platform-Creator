package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.IUpdateable
import be.catvert.mtrktx.ecs.components.*
import be.catvert.mtrktx.ecs.systems.physics.GridCell
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle

/**
 * Created by arno on 07/06/17.
 */

class Level(var levelName: String, val player: Entity, val background: Pair<FileHandle, RenderComponent>, val levelFile: FileHandle, val loadedEntities: MutableList<Entity>): IUpdateable {
    val camera: OrthographicCamera = OrthographicCamera()

    private val shapeRenderer = ShapeRenderer()

    private val matrixSizeX = 300
    private val matrixSizeY = 100
    private val sizeGridCell = 200f

    val matrixGrid = matrix2d(matrixSizeX, matrixSizeY, { row: Int, width: Int -> Array(width) { col -> Pair(mutableListOf<Entity>(), Rectangle(row.toFloat() * sizeGridCell, col.toFloat() * sizeGridCell, sizeGridCell, sizeGridCell)) } })
    val matrixRect = Rectangle(0f, 0f, sizeGridCell * matrixSizeX, sizeGridCell * matrixSizeY)

    private val activeGridCells = mutableListOf<GridCell>()
    fun getActiveGridCells(): List<GridCell> = activeGridCells

    val activeRect = Rectangle(player.getComponent(TransformComponent::class.java).rectangle)

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)
    private val lifeMapper = ComponentMapper.getFor(LifeComponent::class.java)

    var drawDebugCells = false
    var followPlayerCamera = true
    var killEntityUnderY = true
    var applyGravity = true

    private val transformPlayer: TransformComponent = player.getComponent(TransformComponent::class.java)

    init {
        camera.setToOrtho(false, 1280f, 720f)
        activeRect.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        loadedEntities.forEach {
            setEntityGrid(it) // Besoin de setGrid car les entités n'ont pas encore été ajoutée à la matrix
            if (it == player)
                return@forEach
            it.components.filter { c -> c is BaseComponent }.forEach {
                (it as BaseComponent).active = false
            }
        }
    }

    override fun update(deltaTime: Float) {
        updateActiveCells()

        if(followPlayerCamera)
            camera.position.set(Math.max(0f + camera.viewportWidth / 2, transformPlayer.rectangle.x + transformPlayer.rectangle.width / 2), Math.max(0f + camera.viewportHeight / 2, transformPlayer.rectangle.y + transformPlayer.rectangle.height / 2), 0f)

        if (drawDebugCells) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            matrixGrid.forEach {
                it.forEach{
                    shapeRenderer.rect(it.second.x, it.second.y, it.second.width, it.second.height)
                }
            }
            shapeRenderer.rect(activeRect.x, activeRect.y, activeRect.width, activeRect.height)
            shapeRenderer.end()
        }
    }

    private fun updateActiveCells() {
        activeGridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach matrix@ {
                if (it == player)
                    return@matrix
                it.components.filter { c -> c is BaseComponent }.forEach {
                    (it as BaseComponent).active = false
                }
            }
        }

        activeGridCells.clear()

        activeGridCells.addAll(getRectCells(activeRect))

        activeGridCells.forEach {
            for(i in 0 until matrixGrid[it.x][it.y].first.size) {
                val entity = matrixGrid[it.x][it.y].first[i]
                entity.components.filter { c -> c is BaseComponent }.forEach {
                    (it as BaseComponent).active = true
                    if(killEntityUnderY && it is TransformComponent && it.rectangle.y < 0) {
                        if(lifeMapper.has(entity))
                            lifeMapper[entity].killInstant()
                        removeEntity(entity)
                    }
                }
            }
        }
    }

    fun getRectCells(rect: Rectangle): List<GridCell> {
        val cells = mutableListOf<GridCell>()

        fun rectContains(x: Int, y: Int): Boolean {
            if ((x < 0 || y < 0) || (x >= matrixSizeX || y >= matrixSizeY)) {
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

    fun addEntity(entity: Entity) {
        setEntityGrid(entity)

        entity.components.filter { c -> c is BaseComponent }.forEach {
            (it as BaseComponent).active = false
        }
    }

    fun removeEntity(entity: Entity) {
        transformMapper[entity].gridCell.forEach {
            matrixGrid[it.x][it.y].first.remove(entity)
        }
    }

    fun setEntityGrid(entity: Entity) {
        val transformComp = transformMapper[entity]

        fun addEntityTo(cell: GridCell) {
            matrixGrid[cell.x][cell.y].first.add(entity)
            transformComp.gridCell.add(cell)
        }

        transformComp.gridCell.forEach {
            matrixGrid[it.x][it.y].first.remove(entity)
        }

        transformComp.gridCell.clear()

        getRectCells(transformComp.rectangle).forEach {
            addEntityTo(it)
        }

    }

    fun getAllEntitiesInCells(cells: List<GridCell>): List<Entity> {
        val list = mutableListOf<Entity>()
        cells.forEach {
            list += matrixGrid[it.x][it.y].first
        }
        return list
    }

    fun getAllEntitiesInRect(rect: Rectangle, overlaps: Boolean = true): List<Entity> {
        val list = mutableSetOf<Entity>()
        val gridCells = getRectCells(rect)
        gridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach {
                if(overlaps) {
                    if(rect.overlaps(transformMapper[it].rectangle))
                        list += it
                }
                else {
                    if(rect.contains(transformMapper[it].rectangle))
                        list += it
                }

            }
        }
        return list.toList()
    }

}