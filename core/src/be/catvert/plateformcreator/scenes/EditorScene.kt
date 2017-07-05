package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.*
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.ecs.components.ParameterType
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Signal
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.*
import ktx.actors.contains
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.ashley.has
import ktx.ashley.mapperFor
import ktx.collections.toGdxArray
import ktx.vis.window

/**
 * Created by Catvert on 10/06/17.
 */

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(game: MtrGame, private val level: Level) : BaseScene(game, systems = RenderingSystem(game)) {
    /**
     * Enum représentant les différents modes de l'éditeur(sélection d'entités, copie ..)
     */
    private enum class EditorMode {
        NoMode, SelectEntity, CopyEntity, SelectPoint
    }

    /**
     * Enum représentant les différents modes de redimensionnement
     */
    private enum class EditorResizeMode {
        Free, Lock
    }

    /**
     * Enum représentant les différents mode disponible pour les entités sélectionnées
     */
    private enum class SelectEntityMode {
        NOTHING, MOVE, RESIZE
    }

    /**
     * Classe de donnée permettant de créer le rectangle de sélection
     */
    private data class RectangleMode(var startPosition: Vector2, var endPosition: Vector2, var rectangleStarted: Boolean = false) {
        fun getRectangle(): Rectangle {
            val minX = Math.min(startPosition.x, endPosition.x)
            val minY = Math.min(startPosition.y, endPosition.y)
            val maxX = Math.max(startPosition.x, endPosition.x)
            val maxY = Math.max(startPosition.y, endPosition.y)

            return Rectangle(minX, minY, maxX - minX, maxY - minY)
        }
    }

    /**
     * Le shapeRenderer utilisé par exemple pour afficher un rectangle autour d'une entité
     */
    private val shapeRenderer = ShapeRenderer()

    private val transformMapper = mapperFor<TransformComponent>()
    private val renderMapper = mapperFor<RenderComponent>()

    /**
     * Vitesse de la caméra
     */
    private val cameraMoveSpeed = 10f

    /**
     * Taille maximum qu'une entité peut avoir (en largeur et hauteur)
     */
    private val maxEntitySize = 500f

    /**
     * Les entités sélectionnées par le joueur
     */
    private val selectEntities = mutableSetOf<Entity>()

    /**
     * L'entité sélectionné pour le déplacement de celle-ci ou du groupe
     */
    private var selectMoveEntity: Entity? = null

    /**
     * Le mode d'action sur les entités sélectionnées
     */
    private var selectEntityMode = SelectEntityMode.NOTHING

    /**
     * Le mode de redimensionnement sur les entités sélectionnées
     */
    private var resizeMode = EditorResizeMode.Lock

    /**
     * Signal appelé quand l'entité sélectionnée subit des modifications
     */
    private val onSelectEntityUpdate: Signal<Entity?> = Signal()

    private val onSelectPoint: Signal<Point> = Signal()

    /**
     * L'entité à copier
     */
    private var copyEntity: Entity? = null
        set(value) {
            field = value

            if (value == null || value isType EntityFactory.EntityType.Player)
                editorMode = EditorMode.NoMode
            else
                editorMode = EditorMode.CopyEntity
        }

    /**
     * La position du début et de la fin du rectangle
     */
    private val rectangleMode = RectangleMode(Vector2(), Vector2())

    /**
     * Le mode de l'éditeur, plusieurs modes sont possibles, tel que le mode de sélection, de copie..
     */
    private var editorMode: EditorMode = EditorMode.NoMode

    /**
     * Est-ce que à la dernière frame, la bouton gauche était pressé
     */
    private var leftButtonPressedLastFrame = false

    /**
     * Est-ce que à la dernière frame, la bouton droit était pressé
     */
    private var latestRightButtonClick = false

    /**
     * Position de la souris au dernier frame
     */
    private var latestMousePos = Vector2()

    /**
     * Le font utilisé par l'éditeur
     */
    private val editorFont = BitmapFont(Gdx.files.internal("fonts/editorFont.fnt"))

    /**
     * La layer sélectionnée utilisé pour la détection d'une entité sous la souris selon son layer
     */
    private var selectedLayer = Layer.LAYER_0

    /**
     * L'entityFactory utilisé pour créer de nouvelle entités
     */
    private val entityFactory = EntityFactory(game, level.levelFile)

    /**
     * Permet d'ajouter une nouvelle entité sélectionnée
     */
    private fun addSelectEntity(entity: Entity) {
        selectEntities += entity
        if (selectEntities.size == 1) {
            selectMoveEntity = entity
            onSelectEntityUpdate.dispatch(entity)
        } else
            onSelectEntityUpdate.dispatch(null)

        editorMode = EditorMode.SelectEntity
    }

    /**
     * Permet de supprimer les entités sélectionnées de la liste -> ils ne sont pas supprimés du niveau, juste déséléctionnés
     */
    private fun clearSelectEntities() {
        selectEntities.clear()
        selectMoveEntity = null
        onSelectEntityUpdate.dispatch(null)

        editorMode = EditorMode.NoMode
    }

    /**
     * Permet de vérifier si l'utilisateur est sur l'UI ou non
     */
    fun isUIHover(): Boolean {
        stage.actors.filter { it.isVisible }.forEach {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()

            if ((mouseX >= it.x && mouseX <= it.x + it.width) && (Gdx.graphics.height - mouseY <= it.y + it.height && Gdx.graphics.height - mouseY >= it.y)) {
                return true
            }
        }
        return false
    }

    init {
        background = game.getBackground(level.backgroundPath).second

        showInfoEntityWindow()

        level.activeRect.setSize(Gdx.graphics.width.toFloat() * 3, Gdx.graphics.height.toFloat() * 3)
        level.followPlayerCamera = false
        level.drawDebugCells = true
        level.killEntityNegativeY = false
    }

    override fun dispose() {
        super.dispose()

        editorFont.dispose()
    }

    override fun render(delta: Float) {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        /**
         * Dessine les entités qui n'ont pas de renderComponent avec un rectangle noir et les paramètres des entités
         */
        entities.forEach { entity ->
            if (!renderMapper.has(entity)) {
                shapeRenderer.color = Color.BLACK
                val rect = transformMapper[entity].rectangle
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            }

            if (entity.has(mapperFor<ParametersComponent>())) {
                val params = mapperFor<ParametersComponent>()[entity].getParameters()

                params.forEach {
                    if (it.drawInEditor) {
                        it.param.onDrawInEditor(entity, shapeRenderer)
                    }
                }
            }
        }

        when (editorMode) {
            EditorScene.EditorMode.NoMode -> {
                /**
                 * Dessine le rectangle en cour de création
                 */
                if (rectangleMode.rectangleStarted) {
                    shapeRenderer.color = Color.BLUE
                    val rect = rectangleMode.getRectangle()
                    shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
                }
            }
            EditorScene.EditorMode.SelectEntity -> {
                /**
                 * Dessine les entités sélectionnées
                 */
                selectEntities.forEach {
                    shapeRenderer.color = Color.RED
                    if (it === selectMoveEntity) {
                        shapeRenderer.color = Color.CORAL
                    }
                    val transform = transformMapper[it]
                    shapeRenderer.rect(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width, transform.rectangle.height)
                }

                if (selectMoveEntity != null && !transformMapper[selectMoveEntity].fixedSizeEditor) {
                    val transform = transformMapper[selectMoveEntity]

                    when (selectEntityMode) {
                        EditorScene.SelectEntityMode.NOTHING -> {
                            shapeRenderer.color = Color.RED; shapeRenderer.circle(transform.rectangle.x + transform.rectangle.width, transform.rectangle.y + transform.rectangle.height, 10f)
                        }
                        EditorScene.SelectEntityMode.RESIZE -> {
                            shapeRenderer.color = Color.BLUE; shapeRenderer.circle(transform.rectangle.x + transform.rectangle.width, transform.rectangle.y + transform.rectangle.height, 10f)
                        }
                        EditorScene.SelectEntityMode.MOVE -> {
                        }
                    }
                }

                /**
                 * Dessine la position de la première entité sélectionnée
                 */
                game.batch.projectionMatrix = camera.combined
                game.batch.use { gameBatch ->
                    val transform = transformMapper[selectEntities.elementAt(0)]
                    editorFont.draw(gameBatch, "(${transform.rectangle.x.toInt()}, ${transform.rectangle.y.toInt()})", transform.rectangle.x, transform.rectangle.y + transform.rectangle.height + 20)
                }
            }
            EditorScene.EditorMode.CopyEntity -> {
                /**
                 * Dessine l'entité à copier
                 * Vérifie si l'entité se trouve bien dans le niveau, dans le cas contraire, ça veut dire que l'entité vient d'être créer et qu'il ne faut donc pas afficher son cadre car elle n'est pas encore dans le niveau
                 */
                if (copyEntity != null && level.loadedEntities.contains(copyEntity!!)) {
                    val transform = transformMapper[copyEntity]
                    shapeRenderer.color = Color.GREEN
                    shapeRenderer.rect(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width, transform.rectangle.height)
                }
            }
            EditorScene.EditorMode.SelectPoint -> {
                game.batch.projectionMatrix = game.defaultProjection
                game.batch.use { gameBatch ->
                    val layout = GlyphLayout(game.mainFont, "Sélectionner un point")
                    game.mainFont.draw(gameBatch, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - game.mainFont.lineHeight)
                }
            }
        }
        shapeRenderer.end()

        game.batch.projectionMatrix = game.defaultProjection
        game.batch.use { gameBatch ->
            with(editorFont) {
                draw(gameBatch, "Layer sélectionné : $selectedLayer", 10f, Gdx.graphics.height - editorFont.lineHeight)
                draw(gameBatch, "Nombre d'entités : ${level.loadedEntities.size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
                draw(gameBatch, "Resize mode : ${resizeMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 3)
            }
        }

        super.render(delta)
    }

    override fun update(deltaTime: Float) {
        level.drawDebug()

        if (!isUIHover()) {
            level.activeRect.setPosition(Math.max(0f, camera.position.x - level.activeRect.width / 2), Math.max(0f, camera.position.y - level.activeRect.height / 2))

            level.update(deltaTime)

            entities.clear()
            entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

            game.refreshEntitiesInEngine()
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                showExitWindow()
            }

            var moveCameraX = 0f
            var moveCameraY = 0f
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key)) {
                camera.zoom -= 0.02f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
                camera.zoom += 0.02f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key)) {
                camera.zoom = 1f
            }
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_LEFT.key)) {
                moveCameraX -= cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_RIGHT.key)) {
                moveCameraX += cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_DOWN.key)) {
                moveCameraY -= cameraMoveSpeed
            }
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_UP.key)) {
                moveCameraY += cameraMoveSpeed
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_UP_LAYER.key)) {
                val currentIndex = Layer.values().indexOf(selectedLayer)
                if (currentIndex + 1 < Layer.values().filterNot { it == Layer.LAYER_HUD }.size)
                    selectedLayer = Layer.values()[currentIndex + 1]
            }
            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_DOWN_LAYER.key)) {
                val currentIndex = Layer.values().indexOf(selectedLayer)
                if (currentIndex - 1 >= 0)
                    selectedLayer = Layer.values()[currentIndex - 1]
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_SWITCH_RESIZE_MODE.key)) {
                if (resizeMode == EditorResizeMode.Lock) resizeMode = EditorResizeMode.Free else resizeMode = EditorResizeMode.Lock
            }

            val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
            val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)
            camera.position.set(x, y, 0f)

            if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key)) {
                level.drawDebugCells = !level.drawDebugCells
            }

            camera.update()

            val mousePos = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            val mousePosInWorld = camera.unproject(Vector3(mousePos, 0f))

            when (editorMode) {
                EditorScene.EditorMode.NoMode -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (leftButtonPressedLastFrame) { // Rectangle
                            rectangleMode.endPosition = Vector2(mousePosInWorld.x, mousePosInWorld.y)
                        } else { // Select
                            val entity = findEntityUnderMouse()
                            if (entity != null)
                                addSelectEntity(entity)
                            else { // Maybe rectangle
                                rectangleMode.rectangleStarted = true
                                rectangleMode.startPosition = Vector2(mousePosInWorld.x.toInt().toFloat(), mousePosInWorld.y.toInt().toFloat())
                                rectangleMode.endPosition = rectangleMode.startPosition
                            }
                        }
                    } else if (leftButtonPressedLastFrame && rectangleMode.rectangleStarted) { // left button released on this frame
                        rectangleMode.rectangleStarted = false

                        level.getAllEntitiesInRect(rectangleMode.getRectangle(), false).forEach {
                            addSelectEntity(it)
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && !(entity isType EntityFactory.EntityType.Player)) {
                            copyEntity = entity
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null) {
                            removeEntityFromLevel(entity)
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && renderMapper.has(entity)) {
                            renderMapper[entity].flipX = !renderMapper[entity].flipX
                        }
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && renderMapper.has(entity)) {
                            renderMapper[entity].flipY = !renderMapper[entity].flipY
                        }
                    }
                }
                EditorScene.EditorMode.SelectEntity -> {

                    /**
                     * Permet de déplacer les entités sélectionnées
                     */
                    fun moveEntities(moveX: Int, moveY: Int) {
                        if (selectEntities.let {
                            var result = true
                            it.forEach {
                                val transform = transformMapper[it]
                                if (Rectangle(transform.rectangle.x + moveX, transform.rectangle.y + moveY, transform.rectangle.width, transform.rectangle.height) !in level.matrixRect) {
                                    result = false
                                }
                            }
                            result
                        }) {
                            selectEntities.forEach {
                                val transform = transformMapper[it]
                                moveEntityTo(it, transform.rectangle.x.toInt() + moveX, transform.rectangle.y.toInt() + moveY)
                            }
                            if (selectEntities.size == 1 && selectMoveEntity != null)
                                onSelectEntityUpdate.dispatch(selectMoveEntity)
                        }
                    }

                    /**
                     * On vérifie si le pointeur est dans le cercle de redimensionnement
                     */
                    fun checkCircleResize(): Boolean {
                        return selectMoveEntity != null && Circle(transformMapper[selectMoveEntity].rectangle.x + transformMapper[selectMoveEntity].rectangle.width, transformMapper[selectMoveEntity].rectangle.y + transformMapper[selectMoveEntity].rectangle.height, 10f).contains(mousePosInWorld.x, mousePosInWorld.y)
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (!leftButtonPressedLastFrame) { // Le clique gauche de la précendante scène est pressé ou pas
                            selectEntityMode = SelectEntityMode.NOTHING

                            if (checkCircleResize()) {
                                selectEntityMode = SelectEntityMode.RESIZE
                            } else {
                                val entity = findEntityUnderMouse()
                                if (entity == null) {
                                    clearSelectEntities()
                                } else if (selectEntities.isEmpty() || Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                                    addSelectEntity(entity)
                                } else if (entity in selectEntities) {
                                    selectMoveEntity = entity
                                } else {
                                    clearSelectEntities()
                                    addSelectEntity(entity)
                                }
                            }
                        } else if (selectMoveEntity != null && latestMousePos != mousePos) {
                            if (selectEntityMode == SelectEntityMode.NOTHING)
                                selectEntityMode = SelectEntityMode.MOVE

                            val transformMoveEntity = transformMapper[selectMoveEntity]

                            when (selectEntityMode) {
                                SelectEntityMode.NOTHING -> {
                                }
                                SelectEntityMode.RESIZE -> {
                                    var resizeX = transformMoveEntity.rectangle.x + transformMoveEntity.rectangle.width - mousePosInWorld.x
                                    var resizeY = transformMoveEntity.rectangle.y + transformMoveEntity.rectangle.height - mousePosInWorld.y

                                    if (resizeMode == EditorResizeMode.Lock) {
                                        if (Math.abs(resizeX) > Math.abs(resizeY))
                                            resizeX = resizeY
                                        else
                                            resizeY = resizeX
                                    }

                                    if (selectEntities.let {
                                        var result = true
                                        it.forEach {
                                            val transform = transformMapper[it]
                                            if (Rectangle(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width - resizeX, transform.rectangle.height - resizeY) !in level.matrixRect) {
                                                result = false
                                            }
                                        }
                                        result
                                    }) {
                                        selectEntities.forEach {
                                            if (it isType selectMoveEntity!!.getType() && !transformMapper[it].fixedSizeEditor) {
                                                val transform = transformMapper[it]

                                                val newSizeX = transform.rectangle.width - resizeX
                                                val newSizeY = transform.rectangle.height - resizeY

                                                if (newSizeX > 0 && newSizeX <= maxEntitySize)
                                                    transform.rectangle.width = newSizeX
                                                if (newSizeY > 0 && newSizeY <= maxEntitySize)
                                                    transform.rectangle.height = newSizeY
                                            }
                                        }

                                        if (selectEntities.size == 1 && selectMoveEntity != null)
                                            onSelectEntityUpdate.dispatch(selectMoveEntity)
                                    }
                                }
                                SelectEntityMode.MOVE -> {
                                    // move entities
                                    val moveX = transformMoveEntity.rectangle.x + transformMoveEntity.rectangle.width / 2 - mousePosInWorld.x
                                    val moveY = transformMoveEntity.rectangle.y + transformMoveEntity.rectangle.height / 2 - mousePosInWorld.y

                                    moveEntities(-moveX.toInt(), -moveY.toInt())
                                }
                            }
                        }
                    } else {
                        selectEntityMode = SelectEntityMode.NOTHING

                        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                            if (findEntityUnderMouse() in selectEntities)
                                showContextMenuSelectEntities()
                            else
                                clearSelectEntities()
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_LEFT.key)) {
                            moveEntities(-1, 0)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_RIGHT.key)) {
                            moveEntities(1, 0)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_UP.key)) {
                            moveEntities(0, 1)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_DOWN.key)) {
                            moveEntities(0, -1)
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)
                            && (selectEntities.size > 1 || findEntityUnderMouse() === selectEntities.elementAt(0))) {
                        selectEntities.forEach {
                            removeEntityFromLevel(it)
                        }
                        clearSelectEntities()
                    }
                }
                EditorScene.EditorMode.CopyEntity -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        copyEntity = findEntityUnderMouse()
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        val newEntity = entityFactory.copyEntity(copyEntity!!)
                        val transform = transformMapper[newEntity]

                        var posX = transform.rectangle.x
                        var posY = transform.rectangle.y

                        var sizeX = transform.rectangle.width.toInt()
                        var sizeY = transform.rectangle.height.toInt()

                        if (transform.rectangle.width == 0f || transform.rectangle.height == 0f) {
                            if (renderMapper.has(newEntity)) {
                                val region = renderMapper[newEntity].getActualAtlasRegion()

                                sizeX = region.regionWidth
                                sizeY = region.regionHeight
                            }
                        }

                        var moveToNextEntity = true

                        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_LEFT.key)) {
                            posX -= sizeX
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_RIGHT.key)) {
                            posX += sizeX
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_DOWN.key)) {
                            posY -= sizeY
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_UP.key)) {
                            posY += sizeY
                        } else {
                            posX = Math.min(level.matrixRect.width - transform.rectangle.width, // Les min et max permettent de rester dans le cadre du matrix
                                    Math.max(0f, mousePosInWorld.x - transform.rectangle.width / 2))
                            posY = Math.min(level.matrixRect.height - transform.rectangle.height,
                                    Math.max(0f, mousePosInWorld.y - transform.rectangle.height / 2))

                            moveToNextEntity = false
                        }

                        transform.rectangle.setPosition(posX, posY)
                        addEntityToLevel(newEntity)

                        if (moveToNextEntity)
                            copyEntity = newEntity
                    }
                }
                EditorScene.EditorMode.SelectPoint -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !leftButtonPressedLastFrame) {
                        onSelectPoint.dispatch(Point(mousePosInWorld.x.toInt(), mousePosInWorld.y.toInt()))
                        editorMode = EditorMode.NoMode
                    }
                }
            }

            leftButtonPressedLastFrame = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
            latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
            latestMousePos = mousePos
        }

    }

    /**
     * Permet d'ajouter une entité au niveau
     * @param entity L'entité à ajouter au niveau
     */
    private fun addEntityToLevel(entity: Entity) {
        level.loadedEntities += entity
        level.addEntity(entity)
    }

    /**
     * Permet de supprimer une entité du niveau
     * @param entity L'entité à supprimer du niveau
     */
    private fun removeEntityFromLevel(entity: Entity) {
        if (entity isNotType EntityFactory.EntityType.Player) {
            level.loadedEntities -= entity
            level.removeEntity(entity)
        }
    }

    /**
     * Permet de déplacer une entité du niveau
     * @param entity L'entité à déplacer
     * @param x La nouvelle position x de l'entité
     * @param y La nouvelle position y de l'entité
     */
    private fun moveEntityTo(entity: Entity, x: Int, y: Int) {
        if (transformMapper.has(entity) && level.matrixRect.contains(x.toFloat(), y.toFloat()) && level.matrixRect.contains(x.toFloat() + transformMapper[entity].rectangle.width, y.toFloat() + transformMapper[entity].rectangle.height)) {
            transformMapper[entity].rectangle.setPosition(x.toFloat(), y.toFloat())
            level.setEntityGrid(entity)
        }
    }

    /**
     * Permet de redimensionner une entité du niveau
     * @param entity L'entité à redimensionner
     * @param width La nouvelle largeur de l'entité
     * @param height La nouvelle hauteur de l'entité
     */
    private fun resizeEntityTo(entity: Entity, width: Int, height: Int) {
        if (transformMapper.has(entity) && !transformMapper[entity].fixedSizeEditor && width > 0 && height > 0 && level.matrixRect.contains(transformMapper[entity].rectangle.x + width, transformMapper[entity].rectangle.y + height))
            transformMapper[entity].rectangle.setSize(width.toFloat(), height.toFloat())
    }

    /**
     * Permet de retourner l'entité sous le pointeur par rapport à son layer
     */
    private fun findEntityUnderMouse(): Entity? {
        stage.keyboardFocus = null // Enlève le focus sur la fenêtre active permettant d'utiliser par exemple les touches de déplacement même si le joueur était dans un textField l'étape avant

        val mousePosInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

        val entities = entities.filter {
            transformMapper[it].rectangle.contains(mousePosInWorld.x, mousePosInWorld.y)
        }

        if (!entities.isEmpty()) {
            val goodLayerEntity = entities.find { renderMapper.has(it) && renderMapper[it].renderLayer == selectedLayer }
            if (goodLayerEntity != null)
                return goodLayerEntity
            else {
                val entity = entities.first()
                if (renderMapper.has(entity))
                    selectedLayer = renderMapper[entity].renderLayer
                return entity
            }
        }

        return null
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     * @param onTextureSelected Méthode appelée quand la texture a été correctement sélectionnée par le joueur
     */
    private fun showSelectTextureWindow(onTextureSelected: (TextureInfo) -> Unit) {
        /**
         * Classe de donnée représentant la sélection d'une texture atlas
         */
        data class TextureAtlasSelect(val textureAtlas: TextureAtlas, val atlasName: String) {
            override fun toString(): String {
                return atlasName
            }
        }

        stage + window("Sélectionner une texture") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                selectBox<TextureAtlasSelect> {
                    items = game.getTextureAtlasList().let {
                        val list = mutableListOf<TextureAtlasSelect>()

                        it.forEach {
                            list += TextureAtlasSelect(it.first, it.second)
                        }

                        list.toGdxArray()
                    }

                    addListener(onChange {
                        table.clearChildren()

                        var count = 0
                        selected.textureAtlas.regions.forEach {
                            val textureInfo = it
                            val image = VisImage(textureInfo)

                            image.userObject = TextureInfo(it, selected.atlasName, it.name)

                            image.addListener(image.onClick {
                                selectedImage.drawable = image.drawable
                                selectedImage.userObject = image.userObject
                            })

                            table.add(image).size(50f, 50f).space(10f)

                            ++count
                            if (count >= 5) {
                                table.row()
                                count = 0
                            }
                        }
                    })

                    if (items.size > 1) { // Permet de mettre a jour les acteurs pour créer l'entités
                        selectedIndex = 1
                        selectedIndex = 0
                    }
                }
                row()

                val scroll = ScrollPane(table)
                add(scroll).size(300f, 200f).space(10f)

                row()

                add(selectedImage).size(50f, 50f).space(10f)

                row()

                textButton("Sélectionner") {
                    addListener(onClick {
                        if (selectedImage.userObject != null && selectedImage.userObject is TextureInfo) {
                            onTextureSelected(selectedImage.userObject as TextureInfo)
                            this@window.remove()
                        }
                    })
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour créer une entité
     */
    private fun showAddEntityWindow() {
        stage + window("Ajouter une entité") {
            setSize(250f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            verticalGroup {
                space(10f)
                verticalGroup verticalSettingsGroup@ {
                    space(10f)

                    selectBox<EntityFactory.EntityType> {
                        items = EntityFactory.EntityType.values().toGdxArray().let {
                            it.removeValue(EntityFactory.EntityType.Player, false)
                            it
                        }

                        fun addSize(): Pair<VisTextField, VisTextField> {
                            val widthField: VisTextField = VisTextField()
                            val heightField: VisTextField = VisTextField()

                            this@verticalSettingsGroup.verticalGroup {
                                space(10f)

                                horizontalGroup {
                                    label("Largeur : ")
                                    addActor(widthField)
                                }
                                horizontalGroup {
                                    label("Hauteur : ")
                                    addActor(heightField)
                                }
                            }

                            return widthField to heightField
                        }

                        fun addSelectTexture(onTextureSelected: (TextureInfo) -> Unit, widthField: VisTextField, heightField: VisTextField) {
                            this@verticalSettingsGroup.table {
                                val image = com.kotcrab.vis.ui.widget.VisImage()
                                textButton("Sélectionner texture") {
                                    addListener(onClick {
                                        showSelectTextureWindow({ texture ->
                                            widthField.text = texture.texture.regionWidth.toString()
                                            heightField.text = texture.texture.regionHeight.toString()

                                            onTextureSelected(texture)
                                            image.drawable = TextureRegionDrawable(texture.texture)
                                            if (image !in this@table) {
                                                this@table.add(image).size(this.height, this.height).spaceLeft(10f)
                                            }
                                        })
                                    })
                                }
                            }
                        }

                        fun checkValidSize(widthField: VisTextField, heightField: VisTextField): Boolean {
                            if (widthField.text.toIntOrNull() == null || heightField.text.toIntOrNull() == null)
                                return false
                            val width = widthField.text.toInt()
                            val height = heightField.text.toInt()

                            return (width in 1f..maxEntitySize) && (height in 1f..maxEntitySize)
                        }

                        fun finishEntityBuild(entity: Entity) {
                            copyEntity = entity

                            this@window.remove()
                        }

                        addListener(onChange {
                            this@verticalSettingsGroup.clearChildren()
                            this@verticalSettingsGroup.addActor(this)

                            val addButton = VisTextButton("Ajouter !")

                            if (selected != null) {
                                when (selected!!) {
                                    EntityFactory.EntityType.Sprite -> {
                                        val (width, height) = addSize()

                                        var selectedTexture: TextureInfo? = null
                                        addSelectTexture({ texture ->
                                            selectedTexture = texture
                                        }, width, height)

                                        addButton.addListener(addButton.onClick {
                                            if (checkValidSize(width, height) && selectedTexture != null) {
                                                finishEntityBuild(EntityFactory.createSprite(TransformComponent(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat())), renderComponent { textures, _ -> textures += selectedTexture!! }))
                                            }
                                        })
                                    }
                                    EntityFactory.EntityType.PhysicsSprite -> {
                                        val (width, height) = addSize()

                                        var selectedTexture: TextureInfo? = null
                                        addSelectTexture({ texture ->
                                            selectedTexture = texture
                                        }, width, height)

                                        addButton.addListener(addButton.onClick {
                                            if (checkValidSize(width, height) && selectedTexture != null) {
                                                finishEntityBuild(EntityFactory.createPhysicsSprite(TransformComponent(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat())), renderComponent { textures, _ -> textures += selectedTexture!! }, PhysicsComponent(true)))
                                            }
                                        })

                                    }
                                    EntityFactory.EntityType.Enemy -> {
                                        val enemyType = this@verticalSettingsGroup.selectBox<EnemyType> {
                                            items = EnemyType.values().toGdxArray()
                                        }

                                        addButton.addListener(addButton.onClick {
                                            finishEntityBuild(entityFactory.createEnemyWithType(enemyType.selected, Point(0, 0)))
                                        })
                                    }
                                    EntityFactory.EntityType.Player -> {
                                    }
                                    EntityFactory.EntityType.Special -> {
                                        val specialType = this@verticalSettingsGroup.selectBox<SpecialType> {
                                            items = SpecialType.values().toGdxArray()
                                        }

                                        addButton.addListener(addButton.onClick {
                                            finishEntityBuild(entityFactory.createSpecialWithType(specialType.selected, Point(0, 0)))
                                        })
                                    }
                                }
                            }

                            this@verticalSettingsGroup.addActor(addButton)
                        })

                        if (this.items.size > 1) { // Permet de mettre a jour les acteurs pour créer l'entités
                            this.selectedIndex = 1
                            this.selectedIndex = 0
                        }

                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre comportant les informations de l'entité sélectionnée
     */
    private fun showInfoEntityWindow() {
        stage + window("Réglages des entités") {
            setSize(250f, 275f)
            setPosition(Gdx.graphics.width - width, Gdx.graphics.height - height)
            verticalGroup {
                space(10f)

                textButton("Ajouter une entité") {
                    addListener(onClick { showAddEntityWindow() })
                }
                textButton("Supprimer l'entité sélectionnée") {
                    touchable = Touchable.disabled

                    onSelectEntityUpdate.add { _, selectEntity ->
                        this.touchable =
                                if (selectEntity == null || selectEntity isType EntityFactory.EntityType.Player)
                                    Touchable.disabled
                                else
                                    Touchable.enabled
                    }

                    addListener(onClick {
                        removeEntityFromLevel(selectEntities.elementAt(0))
                        clearSelectEntities()
                    })
                }

                label("Aucune entité sélectionnée") {
                    onSelectEntityUpdate.add { _, selectEntity ->
                        if (selectEntity == null)
                            setText("Aucune entité sélectionnée")
                        else
                            setText(EntityFactory.EntityType.values().first { entityType -> selectEntity isType entityType }.name)
                    }
                }
                horizontalGroup {
                    label("Layer : ")
                    selectBox<Layer> {
                        items = Layer.values().filterNot { it == Layer.LAYER_HUD }.toGdxArray()

                        touchable = Touchable.disabled

                        onSelectEntityUpdate.add { _, selectEntity ->
                            if (selectEntity == null || !renderMapper.has(selectEntity)) {
                                touchable = Touchable.disabled
                                selectedIndex = 0
                            } else {
                                touchable = Touchable.enabled
                                selected = renderMapper[selectEntity].renderLayer
                            }
                        }

                        addListener(onChange {
                            if (this.isTouchable) {
                                val selectEntity = selectEntities.elementAt(0)
                                renderMapper[selectEntity].renderLayer = selected
                            }
                        })
                    }
                }

                table {
                    onSelectEntityUpdate.add { _, _ ->
                        clearChildren()
                        if (selectEntities.isNotEmpty() && renderMapper.has(selectEntities.first())) {
                            val firstRender = renderMapper[selectEntities.first()]

                            val firstSpriteSheet = firstRender.textureInfoList[0].spriteSheet
                            val firstRegionName = firstRender.textureInfoList[0].texture.name

                            var canChangeManuallyOnly = false

                            selectEntities.forEach {
                                if (!renderMapper.has(it)
                                        || it isNotType selectEntities.first().getType()
                                        || renderMapper[it].useAnimation
                                        || renderMapper[it].textureInfoList.size > 1
                                        || renderMapper[it].textureInfoList[0].spriteSheet != firstSpriteSheet
                                        || renderMapper[it].fixedTextureEditor)
                                    return@add

                                if (renderMapper[it].textureInfoList[0].texture.name != firstRegionName)
                                    canChangeManuallyOnly = true
                            }

                            val image = VisImageButton(TextureRegionDrawable(firstRender.getActualAtlasRegion()), "Changer la texture")
                            image.setSize(50f, 50f)

                            fun updateRegion(textureInfo: TextureInfo) {
                                image.style.imageUp = TextureRegionDrawable(textureInfo.texture)
                                image.style.imageDown = TextureRegionDrawable(textureInfo.texture)

                                selectEntities.forEach {
                                    renderMapper[it].textureInfoList[0] = textureInfo
                                }
                            }

                            image.addListener(image.onClick {
                                showSelectTextureWindow {
                                    updateRegion(it)
                                }
                            })

                            fun changeRegion(atlas: TextureAtlas, moveIndex: Int) {
                                val posActualRegion = atlas.regions.indexOfFirst { it.name == firstRender.textureInfoList[0].texturePath }
                                if (posActualRegion != -1) {
                                    val newIndex = posActualRegion + moveIndex

                                    val region =
                                            if (newIndex < 0) atlas.regions[atlas.regions.size - 1]
                                            else if (newIndex >= atlas.regions.size) atlas.regions[0]
                                            else atlas.regions[posActualRegion + moveIndex]

                                    updateRegion(TextureInfo(region, firstSpriteSheet, region.name))
                                }
                            }

                            if (!canChangeManuallyOnly) {
                                textButton("<-") {
                                    addListener(onClick {
                                        val atlas = game.getTextureAtlasList().firstOrNull { it.second == firstSpriteSheet }?.first

                                        if (atlas != null)
                                            changeRegion(atlas, -1)
                                    })
                                }
                            }

                            add(image).size(75f, 75f).space(10f)

                            if (!canChangeManuallyOnly) {
                                textButton("->") {
                                    addListener(onClick {
                                        val atlas = game.getTextureAtlasList().firstOrNull { it.second == firstSpriteSheet }?.first

                                        if (atlas != null)
                                            changeRegion(atlas, 1)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre permettant de sauvegarder et quitter l'éditeur
     */
    private fun showExitWindow() {
        stage + window("Quitter") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    addListener(onClick {
                        try {
                            LevelFactory.saveLevel(level)
                            this@window.remove()
                        } catch(e: Exception) {
                            ktx.log.error(e, message = { "Erreur lors de l'enregistrement du niveau !" })
                        }
                    })
                }
                textButton("Options du niveau") {
                    addListener(onClick {
                        showSettingsLevelWindow()
                    })
                }
                textButton("Quitter") {
                    addListener(onClick { game.setScene(MainMenuScene(game)) })
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre permettant de modifier les paramètres du niveau
     */
    private fun showSettingsLevelWindow() {
        fun switchBackground(i: Int) {
            val index = game.getBackgroundsList().indexOfFirst { it.first == level.backgroundPath }
            if (index != -1) {
                val newIndex = index + i
                if (newIndex >= 0 && newIndex < game.getBackgroundsList().size) {
                    val newBackground = game.getBackgroundsList()[newIndex]
                    level.backgroundPath = newBackground.first
                    background = newBackground.second
                }
            } else {
                error { "Selected background no found !" }
            }
        }

        stage + window("Paramètres du niveau") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                horizontalGroup {
                    space(10f)

                    textButton("<-") {
                        addListener(onClick {
                            switchBackground(-1)
                        })
                    }

                    label("Fond d'écran")

                    textButton("->") {
                        addListener(onClick {
                            switchBackground(1)
                        })
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre de configuration des entités
     */
    private fun showContextMenuSelectEntities() {
        stage + window("Paramètre de l'entité") {
            isModal = true
            isResizable = false
            setSize(250f, 350f)
            setPosition(Gdx.graphics.width / 2 - width / 2, Gdx.graphics.height / 2 - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                val firstEntity = selectEntities.elementAt(0)
                val firstTransform = transformMapper[firstEntity]

                textButton("Supprimer") {
                    addListener(onClick {
                        selectEntities.forEach {
                            removeEntityFromLevel(it)
                        }

                        clearSelectEntities()

                        this@window.remove()
                    })
                }

                textButton("Réinitialiser la taille par défaut") {
                    addListener(onClick {
                        selectEntities.forEach {
                            if (renderMapper.has(it))
                                resizeEntityTo(it, renderMapper[it].getActualAtlasRegion().regionWidth, renderMapper[it].getActualAtlasRegion().regionHeight)
                        }
                    })
                }

                if (selectEntities.size == 1) {
                    if (firstEntity.has(mapperFor<ParametersComponent>())) {
                        val parameters = mapperFor<ParametersComponent>()[firstEntity].getParameters()
                        if (parameters.isNotEmpty()) {
                            textButton("Paramètres avancées") {
                                addListener(onClick {
                                    showParametersEntityWindow(parameters)
                                    this@window.remove()
                                })
                            }
                        }
                    }
                }

                horizontalGroup {
                    space(10f)

                    label("Position X : ")

                    if (selectEntities.size == 1) {
                        textField(firstTransform.rectangle.x.toInt().toString()) {
                            addListener(onChange {
                                if (text.toIntOrNull() != null) {
                                    moveEntityTo(firstEntity, text.toInt(), firstTransform.rectangle.y.toInt())
                                }
                            })
                        }
                    } else {
                        textButton("-") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    moveEntityTo(it, transformMapper[it].rectangle.x.toInt() - 1, transformMapper[it].rectangle.y.toInt())
                                }
                            })
                        }
                        textButton("+") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    moveEntityTo(it, transformMapper[it].rectangle.x.toInt() + 1, transformMapper[it].rectangle.y.toInt())
                                }
                            })
                        }
                    }
                }
                horizontalGroup {
                    space(10f)

                    label("Position Y : ")
                    if (selectEntities.size == 1) {
                        textField(firstTransform.rectangle.y.toInt().toString()) {
                            addListener(onChange {
                                if (text.toIntOrNull() != null) {
                                    moveEntityTo(firstEntity, firstTransform.rectangle.x.toInt(), text.toInt())
                                }
                            })
                        }
                    } else {
                        textButton("-") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    moveEntityTo(it, transformMapper[it].rectangle.x.toInt(), transformMapper[it].rectangle.y.toInt() - 1)
                                }
                            })
                        }
                        textButton("+") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    moveEntityTo(it, transformMapper[it].rectangle.x.toInt(), transformMapper[it].rectangle.y.toInt() + 1)
                                }
                            })
                        }
                    }
                }
                horizontalGroup {
                    space(10f)

                    label("Largeur : ")
                    if (selectEntities.size == 1) {
                        textField(firstTransform.rectangle.width.toInt().toString()) {
                            isReadOnly = firstTransform.fixedSizeEditor

                            addListener(onChange {
                                if (text.toIntOrNull() != null) {
                                    resizeEntityTo(firstEntity, text.toInt(), firstTransform.rectangle.height.toInt())
                                }
                            })
                        }
                    } else {
                        textButton("-") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    resizeEntityTo(it, transformMapper[it].rectangle.width.toInt() - 1, transformMapper[it].rectangle.height.toInt())
                                }
                            })
                        }
                        textButton("+") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    resizeEntityTo(it, transformMapper[it].rectangle.width.toInt() + 1, transformMapper[it].rectangle.height.toInt())
                                }
                            })
                        }
                    }
                }

                horizontalGroup {
                    space(10f)

                    label("Hauteur : ")
                    if (selectEntities.size == 1) {
                        textField(firstTransform.rectangle.height.toInt().toString()) {
                            isReadOnly = firstTransform.fixedSizeEditor
                            addListener(onChange {
                                if (text.toIntOrNull() != null) {
                                    resizeEntityTo(firstEntity, firstTransform.rectangle.width.toInt(), text.toInt())
                                }
                            })
                        }
                    } else {
                        textButton("-") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    resizeEntityTo(it, transformMapper[it].rectangle.width.toInt(), transformMapper[it].rectangle.height.toInt() - 1)
                                }
                            })
                        }
                        textButton("+") {
                            addListener(onClick {
                                selectEntities.forEach {
                                    resizeEntityTo(it, transformMapper[it].rectangle.width.toInt(), transformMapper[it].rectangle.height.toInt() + 1)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre permettant de modifier les paramètres de l'entités ayant un parametersComponent
     * @param parameters Les paramètres à afficher et à modifier
     */
    private fun showParametersEntityWindow(parameters: List<EntityParameter<*>>) {
        stage + window("Les paramètres avancées") {
            isModal = true
            setSize(600f, 200f)
            setPosition(Gdx.graphics.width / 2 - width / 2, Gdx.graphics.height / 2 - height / 2)
            addCloseButton()

            fun addSaveButton(onClick: () -> Unit): VisTextButton {
                val saveButton = VisTextButton("Sauvegarder")
                saveButton.addListener(saveButton.onClick {
                    onClick()
                })
                return saveButton
            }

            verticalGroup {
                space(10f)

                parameters.forEach { parameter ->
                    when (parameter.param.type) {
                        ParameterType.Point -> {
                            val point = parameter.cast<Point>().getValue()

                            var x = point.x
                            var y = point.y

                            horizontalGroup {
                                space(10f)

                                label(parameter.description)

                                textArea(point.x.toString()) {
                                    addListener(onChange {
                                        if (text.toIntOrNull() != null)
                                            x = text.toInt()
                                    })

                                    onSelectPoint.add { _, (posX, _) ->
                                        text = posX.toString()
                                    }
                                }

                                textArea(point.y.toString()) {
                                    addListener(onChange {
                                        if (text.toIntOrNull() != null)
                                            y = text.toInt()
                                    })

                                    onSelectPoint.add { _, (_, posY) ->
                                        text = posY.toString()
                                    }
                                }

                                textButton("Sélect.") {
                                    addListener(onClick {
                                        this@window.isVisible = false

                                        editorMode = EditorMode.SelectPoint
                                    })

                                    onSelectPoint.add { _, (posX, posY) ->
                                        x = posX
                                        y = posY

                                        this@window.isVisible = true
                                    }
                                }

                                addActor(addSaveButton({
                                    parameter.cast<Point>().setValue(Point(Math.max(0, Math.min(level.matrixRect.width.toInt(), x)), Math.max(0, Math.min(level.matrixRect.height.toInt(), y))))
                                }))
                            }
                        }
                        ParameterType.Boolean -> {
                            val bool = parameter.cast<Boolean>().getValue()

                            var value = bool

                            horizontalGroup {
                                space(10f)

                                checkBox(parameter.description) {
                                    isChecked = value

                                    addListener(onChange {
                                        value = isChecked
                                    })
                                }

                                addActor(addSaveButton({
                                    parameter.cast<Boolean>().setValue(value)
                                }))
                            }
                        }
                        ParameterType.Integer -> {
                            val int = parameter.cast<Int>().getValue()

                            var value = int

                            horizontalGroup {
                                space(10f)

                                label(parameter.description)

                                textArea(value.toString()) {
                                    addListener(onChange {
                                        if (text.toIntOrNull() != null)
                                            value = text.toInt()
                                    })
                                }

                                addActor(addSaveButton {
                                    parameter.cast<Int>().setValue(value)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}