package be.catvert.pc.eca.containers

import be.catvert.pc.GameKeys
import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityChecker
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.fasterxml.jackson.annotation.JsonIgnore
import glm_.func.common.clamp
import glm_.max
import kotlin.math.roundToInt

abstract class EntityMatrixContainer : EntityContainer() {
    private val shapeRenderer = ShapeRenderer()

    var matrixWidth = Constants.minMatrixSize
        set(value) {
            if (value >= Constants.minMatrixSize) {
                if (value > field) {
                    for (i in 0 until value - field) {
                        ++field

                        val width = mutableListOf<MutableList<Entity>>()
                        matrixGrid.last().forEach {
                            width.add(mutableListOf())
                        }
                        matrixGrid.add(width)

                        matrixRect.width += Constants.matrixCellSize
                    }
                } else {
                    for (i in 0 until Math.abs(field - value)) {
                        --field

                        val last = matrixGrid.last().apply {
                            forEach {
                                it.forEach {
                                    removeEntity(it)
                                }
                            }
                        }
                        matrixGrid.remove(last)

                        matrixRect.width -= Constants.matrixCellSize
                    }
                }

                updateActiveCells()
            }
        }
    var matrixHeight = Constants.minMatrixSize
        set(value) {
            if (value >= Constants.minMatrixSize) {
                if (value > field) {
                    for (i in 0 until value - field) {
                        ++field
                        matrixGrid.forEach {
                            it.add(mutableListOf())
                        }

                        matrixRect.height += Constants.matrixCellSize
                    }
                } else {
                    for (i in 0 until Math.abs(field - value)) {
                        --field
                        matrixGrid.forEach {
                            val last = it.last().apply {
                                it.last().forEach {
                                    removeEntity(it)
                                }
                            }
                            it.remove(last)
                        }

                        matrixRect.height -= Constants.matrixCellSize
                    }
                }

                updateActiveCells()
            }
        }
    /**
     * La matrix permettant de stoquer les différentes entités selon leur position dans l'espace
     */
    private val matrixGrid = matrix2d(matrixWidth, matrixHeight, { row: Int, width: Int -> MutableList(width) { col -> mutableListOf<Entity>() } })
    /**
     * Le box illustrant la matrix
     */

    @JsonIgnore
    var matrixRect = Rect(0f, 0f, Constants.matrixCellSize * matrixWidth, Constants.matrixCellSize * matrixHeight)
        private set

    /**
     * Le box illustrant la zone où les cellules sont actives
     */
    @JsonIgnore
    val activeRect = Rect(size = Size(Constants.viewportRatioWidth.roundToInt(), Constants.viewportRatioHeight.roundToInt()))

    /**
     * Les cellules actives
     */
    private val activeGridCells = getCellsInRect(activeRect).toMutableList()

    @JsonIgnore
    fun getActiveGridCells() = activeGridCells.toList()

    @JsonIgnore
    var drawDebugCells = false

    var followEntity = EntityChecker()

    override val processEntities: Set<Entity>
        get() = getAllEntitiesInCells(activeGridCells)

    override fun findEntitiesByTag(tag: EntityTag) = getAllEntitiesInCells(activeGridCells).filter { it.tag == tag }.toList()

    override fun update() {
        updateActiveCells()

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key))
            drawDebugCells = !drawDebugCells

        if (followEntity.entity != null && allowUpdating) {
            activeRect.position = Point(
                    (followEntity.entity!!.box.center().x.roundToInt().toFloat() - activeRect.width / 2).clamp(matrixRect.left(), (matrixRect.right() - activeRect.width).max(0f)),
                    ((followEntity.entity!!.box.center().y.roundToInt().toFloat() - activeRect.height / 2)).clamp(matrixRect.bottom(), (matrixRect.top() - activeRect.height)).max(0f))
        }

        super.update()
    }

    private fun updateActiveCells() {
        activeGridCells.clear()
        activeGridCells.addAll(getCellsInRect(activeRect))
    }

    fun drawDebug() {
        if (drawDebugCells) {
            shapeRenderer.projectionMatrix = PCGame.mainBatch.projectionMatrix
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            matrixGrid.forEachIndexed { x, it ->
                it.forEachIndexed { y, it ->
                    shapeRenderer.rect(getRectangleCell(x, y))
                }
            }
            shapeRenderer.rect(activeRect)
            shapeRenderer.end()
        }
    }

    override fun addEntity(entity: Entity): Entity {
        setEntityToGrid(entity)

        addRectListener(entity)

        return super.addEntity(entity)
    }

    override fun onRemoveEntity(entity: Entity) {
        super.onRemoveEntity(entity)
        entity.gridCells.forEach {
            matrixGrid.elementAtOrNull(it.x)?.elementAtOrNull(it.y)?.remove(entity)
        }
        entity.gridCells.clear()
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        setEntitiesToMatrix(entities)
    }

    private fun getRectangleCell(x: Int, y: Int) = Rect(x.toFloat() * Constants.matrixCellSize, y.toFloat() * Constants.matrixCellSize, Constants.matrixCellSize, Constants.matrixCellSize)

    private fun addRectListener(entity: Entity) {
        entity.box.onPositionChange.register { setEntityToGrid(entity) }
        entity.box.onSizeChange.register { setEntityToGrid(entity) }
    }

    /**
     * Permet de retourner les cellules présentes dans le box spécifié
     * @param rect Le box
     */
    private fun getCellsInRect(rect: Rect): List<GridCell> {
        val cells = mutableListOf<GridCell>()

        fun rectContains(x: Int, y: Int): Boolean {
            if ((x < 0 || y < 0) || (x >= matrixWidth || y >= matrixHeight)) {
                return false
            }

            return rect.overlaps(getRectangleCell(x, y))
        }

        if (matrixRect.overlaps(rect)) {
            var x = Math.max(0, rect.x.roundToInt() / Constants.matrixCellSize)
            var y = Math.max(0, rect.y.roundToInt() / Constants.matrixCellSize)
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

                    cells += if (rectContains(x, y + 1)) {
                        ++y
                        GridCell(x, y)
                    } else
                        break
                } while (true)
            }
        }

        return cells
    }

    /**
     * Permet de mettre à jour la liste des cellules où l'entité se trouve
     * @param entity L'entité à mettre à jour
     */
    private fun setEntityToGrid(entity: Entity) {
        entity.gridCells.forEach {
            matrixGrid[it.x][it.y].remove(entity)
        }

        entity.gridCells.clear()

        getCellsInRect(entity.box).forEach {
            matrixGrid[it.x][it.y].add(entity)
            entity.gridCells.add(it)
        }
    }

    private fun clearMatrix() {
        matrixGrid.forEach {
            it.forEach {
                it.clear()
            }
        }
    }

    private fun setEntitiesToMatrix(newEntities: Set<Entity>) {
        clearMatrix()

        newEntities.forEach {
            addRectListener(it)

            setEntityToGrid(it) // Besoin de setGrid car les entités n'ont pas encore été ajoutée à la matrix
        }
    }

    /**
     * Permet de retourner les entities présents dans les cellules spécifiées
     * @param cells Les cellules où les entités sont présentes
     */
    fun getAllEntitiesInCells(cells: List<GridCell>): Set<Entity> {
        val list = mutableSetOf<Entity>()
        cells.forEach {
            list += matrixGrid[it.x][it.y]
        }
        return list
    }

    fun getAllEntitiesInCells(inRect: Rect): Set<Entity> = getAllEntitiesInCells(getCellsInRect(inRect))
}