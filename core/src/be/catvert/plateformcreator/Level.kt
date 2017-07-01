package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.IUpdateable
import be.catvert.plateformcreator.ecs.components.BaseComponent
import be.catvert.plateformcreator.ecs.components.LifeComponent
import be.catvert.plateformcreator.ecs.components.TransformComponent
import be.catvert.plateformcreator.ecs.systems.physics.GridCell
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

/**
 * Created by Catvert on 07/06/17.
 */

/**
 * Classe représantant le niveau en cour
 * @property game : l'objet du jeu
 * @property levelName : Le nom du niveau
 * @property levelFile : Le fichier utilisé pour charger le niveau
 * @property player : L'entité du joueur
 * @property backgroundPath : Le fond d'écran du niveau
 * @property loadedEntities : Les entités chargés et à sauvegarder dans le fichier du niveau
 */
class Level(private val game: MtrGame, var levelName: String, val levelFile: FileHandle, var player: Entity, var backgroundPath: FileHandle, val loadedEntities: MutableList<Entity>) : IUpdateable {
    /**
     * ShapeRenderer utilisé par exemple pour dessiner des rectangles autour des entités
     */
    private val shapeRenderer = ShapeRenderer()

    /**
     * Taille de la matrix en largeur
     */
    private val matrixWidth = 300

    /**
     * Taille de la matrix en hauteur
     */
    private val matrixHeight = 100

    /**
     * Taille d'une cellule en largeur x hauteur
     */
    private val sizeGridCell = 200f

    /**
     * La matrix permettant de stoquer les différentes entités selon leur position dans l'espace
     */
    val matrixGrid = matrix2d(matrixWidth, matrixHeight, { row: Int, width: Int -> Array(width) { col -> mutableListOf<Entity>() to Rectangle(row.toFloat() * sizeGridCell, col.toFloat() * sizeGridCell, sizeGridCell, sizeGridCell) } })

    /**
     * Le rectangle illustrant la matrix
     */
    val matrixRect = Rectangle(0f, 0f, sizeGridCell * matrixWidth, sizeGridCell * matrixHeight)

    /**
     * Les cellules actives
     */
    private val activeGridCells = mutableListOf<GridCell>()

    /**
     * Permet de retourner les cellules actives
     */
    fun getActiveGridCells(): List<GridCell> = activeGridCells

    /**
     * Le rectangle illustrant la zone où les cellules sont actives
     */
    val activeRect = Rectangle(player.getComponent(TransformComponent::class.java).rectangle)

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val lifeMapper = ComponentMapper.getFor(LifeComponent::class.java)

    /**
     * Permet de dessiner ou non les cellules (debug)
     */
    var drawDebugCells = false

    /**
     * Permet de spécifier si la caméra doit suivre le joueur
     */
    var followPlayerCamera = true

    /**
     * Permet de spécifier si le niveau doit tuer les entités qui ont un y négatif
     */
    var killEntityNegativeY = true

    /**
     * Appliquer ou non la gravité
     */
    var applyGravity = true

    init {
        activeRect.setSize(Gdx.graphics.width.toFloat() * 1.5f, Gdx.graphics.height.toFloat() * 1.5f)

        setActualEntitiesList(loadedEntities)
    }

    override fun update(deltaTime: Float) {
        updateActiveCells()

        if (drawDebugCells) {
            shapeRenderer.projectionMatrix = game.batch.projectionMatrix
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            matrixGrid.forEach {
                it.forEach {
                    shapeRenderer.rect(it.second.x, it.second.y, it.second.width, it.second.height)
                }
            }
            shapeRenderer.rect(activeRect.x, activeRect.y, activeRect.width, activeRect.height)
            shapeRenderer.end()
        }
    }

    /**
     * Permet de mettre à jour les cellules actives
     */
    private fun updateActiveCells() {
        activeGridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach matrix@ {
                if (it === player)
                    return@matrix
                it.components.filter { c -> c is BaseComponent<*> }.forEach {
                    (it as BaseComponent<*>).active = false
                }
            }
        }

        activeGridCells.clear()

        activeGridCells.addAll(getRectCells(activeRect))

        activeGridCells.forEach {
            for (i in 0..matrixGrid[it.x][it.y].first.size - 1) {
                val entity = matrixGrid[it.x][it.y].first[i]
                entity.components.filter { c -> c is BaseComponent<*> }.forEach {
                    (it as BaseComponent<*>).active = true
                    if (killEntityNegativeY && it is TransformComponent && it.rectangle.y < 0) {
                        if (lifeMapper.has(entity))
                            lifeMapper[entity].killInstant()
                        removeEntity(entity)
                    }
                }
            }
        }
    }

    /**
     * Permet de retourner les cellules présentes dans le rectangle spécifié
     * @param rect Le rectangle
     */
    fun getRectCells(rect: Rectangle): List<GridCell> {
        val cells = mutableListOf<GridCell>()

        fun rectContains(x: Int, y: Int): Boolean {
            if ((x < 0 || y < 0) || (x >= matrixWidth || y >= matrixHeight)) {
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

    /**
     * Permet d'ajouter une entité au niveau (ne l'ajoute pas dans loadedEntities)
     * @param entity L'entité à ajouter
     */
    fun addEntity(entity: Entity) {
        setEntityGrid(entity)

        entity.components.filter { c -> c is BaseComponent<*> }.forEach {
            (it as BaseComponent<*>).active = false
        }
    }

    /**
     * Permet de supprimer une entité du niveau (ne la supprime pas de loadedEntities)
     * @param entity L'entité à supprimer
     */
    fun removeEntity(entity: Entity) {
        transformMapper[entity].gridCell.forEach {
            matrixGrid[it.x][it.y].first.remove(entity)
        }
    }

    /**
     * Permet de mettre à jour la liste des cellules où l'entité se trouve
     * @param entity L'entité à mettre à jour
     */
    fun setEntityGrid(entity: Entity) {
        val transformComp = transformMapper[entity]

        transformComp.gridCell.forEach {
            matrixGrid[it.x][it.y].first.remove(entity)
        }

        transformComp.gridCell.clear()

        getRectCells(transformComp.rectangle).forEach {
            matrixGrid[it.x][it.y].first.add(entity)
            transformComp.gridCell.add(it)
        }
    }

    fun clearMatrix() {
        matrixGrid.forEach {
            it.forEach {
                it.first.clear()
            }
        }
    }

    fun setActualEntitiesList(entities: List<Entity>) {
        clearMatrix()



        entities.forEach {
            setEntityGrid(it) // Besoin de setGrid car les entités n'ont pas encore été ajoutée à la matrix
            if (it === player)
                return@forEach
            it.components.filter { c -> c is BaseComponent<*> }.forEach {
                (it as BaseComponent<*>).active = false
            }
        }
    }

    /**
     * Permet de retourner les entités présentent dans les cellules spécifiées
     * @param cells Les cellules où les entités sont présentes
     */
    fun getAllEntitiesInCells(cells: List<GridCell>): List<Entity> {
        val list = mutableListOf<Entity>()
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
    fun getAllEntitiesInRect(rect: Rectangle, overlaps: Boolean = true): List<Entity> {
        val list = mutableSetOf<Entity>()
        val gridCells = getRectCells(rect)
        gridCells.forEach {
            matrixGrid[it.x][it.y].first.forEach {
                if (overlaps) {
                    if (rect.overlaps(transformMapper[it].rectangle))
                        list += it
                } else {
                    if (transformMapper[it].rectangle in rect)
                        list += it
                }

            }
        }
        return list.toList()
    }

}