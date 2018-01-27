package be.catvert.pc.containers

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.roundToInt

abstract class GameObjectMatrixContainer : GameObjectContainer() {
    private val shapeRenderer = ShapeRenderer()

    var matrixWidth = Constants.minMatrixSize
        set(value) {
            if (value >= Constants.minMatrixSize) {
                if (value > field) {
                    for (i in 0 until value - field) {
                        ++field

                        val width = mutableListOf<MutableList<GameObject>>()
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
                                    removeGameObject(it)
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
                                    removeGameObject(it)
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
    private val matrixGrid = matrix2d(matrixWidth, matrixHeight, { row: Int, width: Int -> MutableList(width) { col -> mutableListOf<GameObject>() } })
    /**
     * Le box illustrant la matrix
     */

    @JsonIgnore
    var matrixRect = Rect(0, 0, Constants.matrixCellSize * matrixWidth, Constants.matrixCellSize * matrixHeight)
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
    private var drawDebugCells = false

    var followGameObject: GameObject? = null

    override val processGameObjects: Set<GameObject>
        get() = getAllGameObjectsInCells(activeGridCells)

    override fun update() {
        updateActiveCells()

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key))
            drawDebugCells = !drawDebugCells

        if (followGameObject != null && allowUpdatingGO) {
            activeRect.position = Point(Math.max(0, followGameObject!!.position().x - activeRect.width / 2 + followGameObject!!.size().width / 2), Math.max(0, followGameObject!!.position().y - activeRect.height / 2 + followGameObject!!.size().height / 2))
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

    override fun addGameObject(gameObject: GameObject): GameObject {
        setGameObjectToGrid(gameObject)

        addRectListener(gameObject)

        return super.addGameObject(gameObject)
    }

    override fun onRemoveGameObject(gameObject: GameObject) {
        super.onRemoveGameObject(gameObject)
        gameObject.gridCells.forEach {
            matrixGrid.elementAtOrNull(it.x)?.elementAtOrNull(it.y)?.remove(gameObject)
        }
        gameObject.gridCells.clear()
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        setGameObjectsToMatrix(gameObjects)
    }

    private fun getRectangleCell(x: Int, y: Int) = Rect(x * Constants.matrixCellSize, y * Constants.matrixCellSize, Constants.matrixCellSize, Constants.matrixCellSize)

    private fun addRectListener(gameObject: GameObject) {
        gameObject.box.onPositionChange.register { setGameObjectToGrid(gameObject) }
        gameObject.box.onSizeChange.register { setGameObjectToGrid(gameObject) }
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
            var x = Math.max(0, rect.x / Constants.matrixCellSize)
            var y = Math.max(0, rect.y / Constants.matrixCellSize)
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
    private fun setGameObjectToGrid(gameObject: GameObject) {
        gameObject.gridCells.forEach {
            matrixGrid[it.x][it.y].remove(gameObject)
        }

        gameObject.gridCells.clear()

        getCellsInRect(gameObject.box).forEach {
            matrixGrid[it.x][it.y].add(gameObject)
            gameObject.gridCells.add(it)
        }
    }

    private fun clearMatrix() {
        matrixGrid.forEach {
            it.forEach {
                it.clear()
            }
        }
    }

    private fun setGameObjectsToMatrix(newGameObjects: Set<GameObject>) {
        clearMatrix()

        newGameObjects.forEach {
            addRectListener(it)

            setGameObjectToGrid(it) // Besoin de setGrid car les entités n'ont pas encore été ajoutée à la matrix
        }
    }

    /**
     * Permet de retourner les gameObjects présents dans les cellules spécifiées
     * @param cells Les cellules où les entités sont présentes
     */
    fun getAllGameObjectsInCells(cells: List<GridCell>): Set<GameObject> {
        val list = mutableSetOf<GameObject>()
        cells.forEach {
            list += matrixGrid[it.x][it.y]
        }
        return list
    }

    fun getAllGameObjectsInCells(inRect: Rect): Set<GameObject> = getAllGameObjectsInCells(getCellsInRect(inRect))
}