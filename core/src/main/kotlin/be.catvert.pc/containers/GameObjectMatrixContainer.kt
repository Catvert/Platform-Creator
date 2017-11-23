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

    private val matrixWidth = 300
    private val matrixHeight = 300
    private val matrixSizeCell = 200

    /**
     * La matrix permettant de stoquer les différentes entités selon leur position dans l'espace
     */
    @JsonIgnore val matrixGrid = matrix2d(matrixWidth, matrixHeight, { row: Int, width: Int -> Array(width) { col -> mutableListOf<GameObject>() to Rect(row.toFloat() * matrixSizeCell, col.toFloat() * matrixSizeCell, matrixSizeCell, matrixSizeCell) } })

    /**
     * Le rectangle illustrant la matrix
     */
    @JsonIgnore val matrixRect = Rect(0f, 0f, matrixSizeCell * matrixWidth, matrixSizeCell * matrixHeight)

    /**
    * Le rectangle illustrant la zone où les cellules sont actives
    */
    @JsonIgnore val activeRect = Rect()

    /**
     * Les cellules actives
     */
    private val activeGridCells = mutableListOf<GridCell>()

    @JsonIgnore var drawDebugCells = false

    @JsonIgnore var followGameObject: GameObject? = null

    init {
        activeRect.size = Size(Gdx.graphics.width * 2, Gdx.graphics.height * 2)
    }

    override fun update() {
        updateActiveCells()

        if(Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key))
            drawDebugCells = !drawDebugCells

        if(followGameObject != null && allowUpdatingGO) {
            activeRect.position =  Point(Math.max(0f, followGameObject!!.position().x - activeRect.width / 2 + followGameObject!!.size().width / 2), Math.max(0f, followGameObject!!.position().y - activeRect.height / 2 + followGameObject!!.size().height / 2))
        }

        super.update()
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
            shapeRenderer.rect(activeRect.x.toFloat(), activeRect.y.toFloat(), activeRect.width.toFloat(), activeRect.height.toFloat())
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
            matrixGrid[it.x][it.y].first.remove(gameObject)
        }
        gameObject.gridCells.clear()
    }

    override fun onPostDeserialization() {
        super.onPostDeserialization()
        setGameObjectsToMatrix(gameObjects)
    }

    private fun addRectListener(gameObject: GameObject) {
        gameObject.rectangle.onPositionChange.register{ setGameObjectToGrid(gameObject) }
        gameObject.rectangle.onSizeChange.register { setGameObjectToGrid(gameObject) }
    }

    /**
     * Permet de mettre à jour les cellules actives
     */
    private fun updateActiveCells() {
        activeGridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach matrix@ {
                it.active = false
            }
        }

        activeGridCells.clear()

        activeGridCells.addAll(getRectCells(activeRect))

        activeGridCells.forEach {
            for (i in 0 until matrixGrid[it.x][it.y].first.size) {
                val go = matrixGrid[it.x][it.y].first[i]
                go.active = true
            }
        }
    }

    /**
     * Permet de retourner les cellules présentes dans le rectangle spécifié
     * @param rect Le rectangle
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
            var x = Math.max(0f, rect.x / matrixSizeCell).toInt()
            var y = Math.max(0f, rect.y / matrixSizeCell).toInt()
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
        if(gameObject.gridCells.isEmpty())
            gameObject.active = false

        gameObject.gridCells.forEach {
            matrixGrid[it.x][it.y].first.remove(gameObject)
        }

        gameObject.gridCells.clear()

        getRectCells(gameObject.rectangle).forEach {
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
     * Permet de retourné les entités présentent dans le rectangle spécifiés
     * @param rect le rectangle dans lequel les entités seront retournées
     * @param overlaps permet de spécifier si le mode de détection est en overlaps ou contains
     */
    fun getAllGameObjectsInRect(rect: Rect, overlaps: Boolean = true): Set<GameObject> {
        val list = mutableSetOf<GameObject>()
        val gridCells = getRectCells(rect)
        gridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach {
                if (overlaps) {
                    if (rect.overlaps(it.rectangle))
                        list += it
                } else {
                    if (rect.contains(it.rectangle))
                        list += it
                }

            }
        }
        return list
    }


}