package be.catvert.pc.containers

import be.catvert.pc.GameKeys
import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.fasterxml.jackson.annotation.JsonIgnore

abstract class GameObjectMatrixContainer : GameObjectContainer() {
    private val shapeRenderer = ShapeRenderer()

    @JsonIgnore
    val minMatrixSize = 10

    var matrixWidth = minMatrixSize
        set(value) {
            if (value >= minMatrixSize) {
                if (value > field) {
                    for (i in 0 until value - field) {
                        addLineWidth()
                        ++field
                    }
                } else {
                    for (i in 0 until Math.abs(field - value)) {
                        removeLineWidth()
                        --field
                    }
                }
            }
        }
    var matrixHeight = minMatrixSize
        set(value) {
            if (value >= minMatrixSize) {
                if (value > field) {
                    for (i in 0 until value - field) {
                        addLineHeight()
                        ++field
                    }
                } else {
                    for (i in 0 until Math.abs(field - value)) {
                        removeLineHeight()
                        --field
                    }
                }
            }
        }

    private val matrixSizeCell = 200
    /**
     * La matrix permettant de stoquer les différentes entités selon leur position dans l'espace
     */
    private val matrixGrid = matrix2d(matrixWidth, matrixHeight, { row: Int, width: Int -> MutableList(width) { col -> mutableListOf<GameObject>() to Rect(row * matrixSizeCell, col * matrixSizeCell, matrixSizeCell, matrixSizeCell) } })
    /**
     * Le box illustrant la matrix
     */

    @JsonIgnore
    var matrixRect = Rect(0, 0, matrixSizeCell * matrixWidth, matrixSizeCell * matrixHeight)
        private set

    /**
     * Le box illustrant la zone où les cellules sont actives
     */
    @JsonIgnore
    val activeRect = Rect()

    /**
     * Les cellules actives
     */
    private val activeGridCells = mutableListOf<GridCell>()

    @JsonIgnore
    fun getActiveGridCells() = activeGridCells.toList()

    @JsonIgnore
    var drawDebugCells = false

    @JsonIgnore
    var followGameObject: GameObject? = null

    init {
        activeRect.size = Size(Gdx.graphics.width * 2, Gdx.graphics.height * 2)
    }

    override fun update() {
        activeGridCells.clear()
        activeGridCells.addAll(getRectCells(activeRect))

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key))
            drawDebugCells = !drawDebugCells

        if (followGameObject != null && allowUpdatingGO) {
            activeRect.position = Point(Math.max(0, followGameObject!!.position().x - activeRect.width / 2 + followGameObject!!.size().width / 2), Math.max(0, followGameObject!!.position().y - activeRect.height / 2 + followGameObject!!.size().height / 2))
        }

        getAllGameObjectsInCells(activeGridCells).forEach {
            if (allowUpdatingGO)
                it.update()

            if (it.position().y < 0) {
                it.onOutOfMapAction(it)
            }
        }

        removeGameObjects()
    }

    fun drawDebug() {
        if (drawDebugCells) {
            shapeRenderer.projectionMatrix = PCGame.mainBatch.projectionMatrix
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            matrixGrid.forEach {
                it.forEach {
                    shapeRenderer.rect(it.second)
                }
            }
            shapeRenderer.rect(activeRect)
            shapeRenderer.end()
        }
    }

    private fun addLineWidth() {
        val width = mutableListOf<Pair<MutableList<GameObject>, Rect>>()
        matrixGrid.last().map { it.second }.forEach {
            width.add(mutableListOf<GameObject>() to Rect(it.x + matrixSizeCell, it.y, matrixSizeCell, matrixSizeCell))
        }

        matrixGrid.add(width)

        matrixRect.width += matrixSizeCell
    }

    private fun removeLineWidth() {
        val last = matrixGrid.last().apply {
            forEach {
                it.first.forEach {
                    removeGameObject(it)
                }
            }
        }
        matrixGrid.remove(last)

        matrixRect.width -= matrixSizeCell
    }

    private fun addLineHeight() {
        matrixGrid.forEach {
            val lastRect = it.last().second
            it.add(mutableListOf<GameObject>() to Rect(lastRect.x, lastRect.y + matrixSizeCell, matrixSizeCell, matrixSizeCell))
        }

        matrixRect.height += matrixSizeCell
    }

    private fun removeLineHeight() {
        matrixGrid.forEach {
            val last = it.last().apply {
                it.last().first.forEach {
                    removeGameObject(it)
                }
            }
            it.remove(last)
        }

        matrixRect.height -= matrixSizeCell
    }

    override fun addGameObject(gameObject: GameObject): GameObject {
        setGameObjectToGrid(gameObject)

        addRectListener(gameObject)

        return super.addGameObject(gameObject)
    }

    override fun onRemoveGameObject(gameObject: GameObject) {
        super.onRemoveGameObject(gameObject)
        gameObject.gridCells.forEach {
            matrixGrid.elementAtOrNull(it.x)?.elementAtOrNull(it.y)?.first?.remove(gameObject)
        }
        gameObject.gridCells.clear()
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        setGameObjectsToMatrix(gameObjects)
    }

    private fun addRectListener(gameObject: GameObject) {
        gameObject.box.onPositionChange.register { setGameObjectToGrid(gameObject) }
        gameObject.box.onSizeChange.register { setGameObjectToGrid(gameObject) }
    }

    /**
     * Permet de retourner les cellules présentes dans le box spécifié
     * @param rect Le box
     */
    fun getRectCells(rect: Rect): List<GridCell> {
        val cells = mutableListOf<GridCell>()

        fun rectContains(x: Int, y: Int): Boolean {
            if ((x < 0 || y < 0) || (x >= matrixWidth || y >= matrixHeight)) {
                return false
            }

            return rect.overlaps(matrixGrid[x][y].second)
        }

        if (matrixRect.overlaps(rect)) {
            var x = Math.max(0, rect.x / matrixSizeCell)
            var y = Math.max(0, rect.y / matrixSizeCell)
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
        // if (gameObject.gridCells.isEmpty())
        //   gameObject.active = false

        gameObject.gridCells.forEach {
            matrixGrid[it.x][it.y].first.remove(gameObject)
        }

        gameObject.gridCells.clear()

        getRectCells(gameObject.box).forEach {
            matrixGrid[it.x][it.y].first.add(gameObject)
            gameObject.gridCells.add(it)
        }
    }

    private fun clearMatrix() {
        matrixGrid.forEach {
            it.forEach {
                it.first.clear()
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
            list += matrixGrid[it.x][it.y].first
        }
        return list
    }

    /**
     * Permet de retourné les entités présentent dans le box spécifiés
     * @param rect le box dans lequel les entités seront retournées
     * @param overlaps permet de spécifier si le mode de détection est en overlaps ou contains
     */
    fun getAllGameObjectsInRect(rect: Rect, overlaps: Boolean = true): Set<GameObject> {
        val list = mutableSetOf<GameObject>()
        val gridCells = getRectCells(rect)
        gridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach {
                if (overlaps) {
                    if (rect.overlaps(it.box))
                        list += it
                } else {
                    if (rect.contains(it.box))
                        list += it
                }

            }
        }
        return list
    }
}