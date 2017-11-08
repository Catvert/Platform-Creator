package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.graphics.AnimationComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyUp
import ktx.actors.plus
import ktx.app.use
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray
import ktx.vis.verticalGroup
import ktx.vis.window
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(val level: Level) : Scene() {
    private enum class EditorMode {
        NO_MODE, SELECT_GO, COPY_GO, SELECT_POINT, TRY_LEVEL
    }

    private enum class ResizeMode {
        FREE, PROPORTIONAL
    }

    private enum class SelectGOMode {
        NO_MODE, MOVE, RESIZE
    }

    /**
     * Classe de donnée permettant de créer le rectangle de sélection
     */
    private data class SelectRectangleData(var startPosition: Point, var endPosition: Point, var rectangleStarted: Boolean = false) {
        fun getRect(): Rect {
            val minX = Math.min(startPosition.x, endPosition.x)
            val minY = Math.min(startPosition.y, endPosition.y)
            val maxX = Math.max(startPosition.x, endPosition.x)
            val maxY = Math.max(startPosition.y, endPosition.y)

            return Rect(minX, minY, maxX - minX, maxY - minY)
        }
    }

    override var gameObjectContainer: GameObjectContainer = level

    private val shapeRenderer = ShapeRenderer()

    private val editorFont = BitmapFont(Constants.editorFontPath.toLocalFile())

    private val cameraMoveSpeed = 10f

    private var editorMode = EditorMode.NO_MODE

    private var selectGameObjectMode = SelectGOMode.NO_MODE
    private var resizeGameObjectMode = ResizeMode.PROPORTIONAL

    private val selectGameObjects = mutableSetOf<GameObject>()
    private var selectGameObject: GameObject? = null
        set(value) {
            field = value
            onSelectGameObjectChange(value)
        }

    private val onSelectGameObjectChange = Signal<GameObject?>()
    private val onSelectPoint = Signal<Point>()

    private val prefabs = mutableSetOf(*PrefabFactory.values().filter { it != PrefabFactory.Player }.map { it.prefab }.toTypedArray())

    private var selectLayer = 0

    /**
     * Est-ce que à la dernière frame, la bouton gauche était pressé
     */
    private var latestLeftButtonClick = false

    /**
     * Est-ce que à la dernière frame, la bouton droit était pressé
     */
    private var latestRightButtonClick = false

    /**
     * Position de la souris au dernier frame
     */
    private var latestMousePos = Point()

    /**
     * La position du début et de la fin du rectangle
     */
    private val selectRectangleData = SelectRectangleData(Point(), Point())

    /**
     * Permet de sauvegarder le dernier spriteSheet utilisé dans la fenêtre de sélection d'atlas
     */
    private var latestSelectTextureWindowAtlasIndex = 0

    private var backupTryModeCameraPos = Vector3()

    init {
        gameObjectContainer.allowUpdatingGO = false

        Utility.getFilesRecursivly(Constants.prefabsDirPath.toLocalFile(), Constants.prefabExtension).forEach {
            prefabs.add(SerializationFactory.deserializeFromFile(it))
        }

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(level.backgroundPath.toLocalFile().path()).asset

        level.removeEntityBelowY0 = false

        showInfoGameObjectWindow()

        onSelectGameObjectChange(null)
    }

    override fun postBatchRender() {
        super.postBatchRender()

        level.drawDebug()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        if (!level.drawDebugCells && editorMode != EditorMode.TRY_LEVEL) {
            shapeRenderer.line(0f, 0f, 10000f, 1f, Color.GOLD, Color.WHITE)
            shapeRenderer.line(0f, 0f, 1f, 10000f, Color.GOLD, Color.WHITE)
        }

        /**
         * Dessine les gameObjects qui n'ont pas de renderableComponent avec un rectangle noir
         */
        gameObjectContainer.getGameObjectsData().filter { it.active }.forEach { gameObject ->
            if (!gameObject.getCurrentState().hasComponent<RenderableComponent>()) {
                shapeRenderer.color = Color.GRAY
                shapeRenderer.rect(gameObject.rectangle)
            }
        }

        when (editorMode) {
            EditorMode.NO_MODE -> {
                /**
                 * Dessine le rectangle en cour de création
                 */
                if (selectRectangleData.rectangleStarted) {
                    shapeRenderer.color = Color.BLUE
                    shapeRenderer.rect(selectRectangleData.getRect())
                }
            }
            EditorMode.SELECT_GO -> {
                /**
                 * Dessine un rectangle autour des gameObjects sélectionnés
                 */
                selectGameObjects.forEach {
                    shapeRenderer.color = Color.RED
                    if (it === selectGameObject) {
                        shapeRenderer.color = Color.CORAL
                    }
                    shapeRenderer.rect(it.rectangle)
                }

                if (selectGameObject != null) {
                    val rect = selectGameObject!!.rectangle

                    when (selectGameObjectMode) {
                        SelectGOMode.NO_MODE -> {
                            shapeRenderer.color = Color.RED; shapeRenderer.circle(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat(), 10f)
                        }
                        SelectGOMode.RESIZE -> {
                            shapeRenderer.color = Color.BLUE; shapeRenderer.circle(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat(), 15f)
                        }
                        SelectGOMode.MOVE -> {
                        }
                    }
                }

                /**
                 * Affiche la position de l'entité sélectionné
                 */
                val rectFirstGO = selectGameObject?.rectangle
                if (rectFirstGO != null) {
                    PCGame.mainBatch.projectionMatrix = camera.combined
                    PCGame.mainBatch.use {
                        editorFont.draw(it, "(${rectFirstGO.x}, ${rectFirstGO.y})", rectFirstGO.x.toFloat(), rectFirstGO.y.toFloat() + rectFirstGO.height.toFloat() + 20f)
                    }
                }
            }
            EditorMode.COPY_GO -> {
                selectGameObjects.forEach {
                    if (it === selectGameObject)
                        shapeRenderer.color = Color.GREEN
                    else
                        shapeRenderer.color = Color.OLIVE
                    shapeRenderer.rect(it.rectangle)
                }
            }
            EditorMode.TRY_LEVEL -> {
            }
            EditorMode.SELECT_POINT -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un point")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight)
                }
            }
        }
        shapeRenderer.end()

        PCGame.mainBatch.use {
            it.projectionMatrix = PCGame.defaultProjection
            with(editorFont) {
                if (editorMode != EditorMode.TRY_LEVEL) {
                    draw(it, "Layer sélectionné : $selectLayer", 10f, Gdx.graphics.height - editorFont.lineHeight)
                    draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
                    draw(it, "Resize mode : ${resizeGameObjectMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 3)
                    draw(it, "Mode de l'éditeur : ${editorMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 4)
                } else {
                    draw(it, "Test du niveau..", 10f, Gdx.graphics.height - lineHeight)
                }
            }
            it.projectionMatrix = camera.combined
        }
    }

    override fun update() {
        super.update()

        if (!isUIHover())
            updateCamera()

        if (!isUIHover() && editorMode != EditorMode.TRY_LEVEL) {
            level.activeRect.position = Point(Math.max(0, camera.position.x.toInt() - level.activeRect.width / 2), Math.max(0, camera.position.y.toInt() - level.activeRect.height / 2))

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_UP_LAYER.key)) {
                selectLayer += 1
            }
            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_DOWN_LAYER.key)) {
                if (selectLayer > 0)
                    selectLayer -= 1
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_SWITCH_RESIZE_MODE.key)) {
                resizeGameObjectMode = if (resizeGameObjectMode == ResizeMode.PROPORTIONAL) ResizeMode.FREE else ResizeMode.PROPORTIONAL
            }

            val mousePosVec2 = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            val mousePos = mousePosVec2.toPoint()
            val mousePosInWorld = camera.unproject(Vector3(mousePosVec2, 0f)).toPoint()

            when (editorMode) {
                EditorMode.NO_MODE -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (latestLeftButtonClick) { // Rectangle
                            selectRectangleData.endPosition = mousePosInWorld
                        } else { // Select
                            val gameObject = findGameObjectUnderMouse()
                            if (gameObject != null) {
                                addSelectGameObject(gameObject)
                                editorMode = EditorMode.SELECT_GO
                            } else { // Maybe rectangle
                                selectRectangleData.rectangleStarted = true
                                selectRectangleData.startPosition = mousePosInWorld
                                selectRectangleData.endPosition = selectRectangleData.startPosition
                            }
                        }
                    } else if (latestLeftButtonClick && selectRectangleData.rectangleStarted) { // Bouton gauche de la souris relaché pendant cette frame
                        selectRectangleData.rectangleStarted = false

                        level.getAllGameObjectsInRect(selectRectangleData.getRect(), false).forEach {
                            addSelectGameObject(it)
                            editorMode = EditorMode.SELECT_GO
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        if (gameObject != null) {
                            addSelectGameObject(gameObject)
                            editorMode = EditorMode.COPY_GO
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        gameObject?.getCurrentState()?.inverseFlipRenderable(true, false)
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        gameObject?.getCurrentState()?.inverseFlipRenderable(true, false)
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        val gameOject = findGameObjectUnderMouse()
                        if (gameOject != null)
                            showEditGameObjectWindow(gameOject)
                    }
                }
                EditorMode.SELECT_GO -> {
                    if (selectGameObjects.isEmpty()) {
                        editorMode = EditorMode.NO_MODE
                        return
                    }

                    /**
                     * Permet de déplacer les gameObjects sélectionnés
                     */
                    fun moveGameObjects(moveX: Int, moveY: Int) {
                        if (selectGameObjects.let {
                            var canMove = true
                            it.forEach {
                                if (!level.matrixRect.contains(Rect(it.rectangle.x + moveX, it.rectangle.y + moveY, it.rectangle.width, it.rectangle.height))) {
                                    canMove = false
                                }
                            }
                            canMove
                        }) {
                            selectGameObjects.forEach {
                                it.rectangle.move(moveX, moveY)
                            }
                        }
                    }

                    /**
                     * On vérifie si le pointeur est dans le cercle de redimensionnement
                     */
                    fun checkCircleResize(): Boolean {
                        if (selectGameObject != null) {
                            val rect = selectGameObject!!.rectangle
                            return Circle(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat(), 10f).contains(mousePosInWorld)
                        }
                        return false
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                        if (!latestLeftButtonClick) {
                            selectGameObjectMode = SelectGOMode.NO_MODE

                            if (checkCircleResize()) {
                                selectGameObjectMode = SelectGOMode.RESIZE
                            } else {
                                val gameObject = findGameObjectUnderMouse()
                                when {
                                    gameObject == null -> { // Se produit lorsque le joueur clique dans le vide, dans ce cas on désélectionne les gameObjects sélectionnés
                                        clearSelectGameObjects()
                                        editorMode = EditorMode.NO_MODE
                                    }
                                    selectGameObjects.contains(gameObject) -> // Dans ce cas-ci, le joueur à sélectionné un autre gameObject dans ceux qui sont sélectionnés
                                        selectGameObject = gameObject
                                    else -> { // Dans ce cas-ci, il y a un groupe de gameObject sélectionné ou aucun et le joueur en sélectionne un nouveau ou un en dehors de la sélection
                                        clearSelectGameObjects()
                                        addSelectGameObject(gameObject)
                                    }
                                }
                            }
                        } else if (selectGameObject != null && latestMousePos != mousePos) { // Le joueur maintient le clique gauche durant plusieurs frames et a bougé la souris {
                            if (selectGameObjectMode == SelectGOMode.NO_MODE)
                                selectGameObjectMode = SelectGOMode.MOVE

                            val selectGORect = selectGameObject!!.rectangle

                            when (selectGameObjectMode) {
                                SelectGOMode.NO_MODE -> {
                                }
                                SelectGOMode.RESIZE -> {
                                    var resizeX = selectGORect.x + selectGORect.width - mousePosInWorld.x
                                    var resizeY = selectGORect.y + selectGORect.height - mousePosInWorld.y

                                    if (resizeGameObjectMode == ResizeMode.PROPORTIONAL) {
                                        if (Math.abs(resizeX) > Math.abs(resizeY))
                                            resizeX = resizeY
                                        else
                                            resizeY = resizeX
                                    }

                                    if (selectGameObjects.let {
                                        var canResize = true
                                        it.forEach {
                                            if (!level.matrixRect.contains(Rect(it.rectangle.x, it.rectangle.y, it.rectangle.width - resizeX, it.rectangle.height - resizeY))) {
                                                canResize = false
                                            }
                                        }
                                        canResize
                                    }) {
                                        selectGameObjects.forEach {
                                            // On vérifie si le gameObject à redimensionner a le même tag que le gameObject sélectionné et qu'il peut être redimensionné
                                            if (it.tag == selectGameObject!!.tag) {
                                                val newSizeX = it.rectangle.width - resizeX
                                                val newSizeY = it.rectangle.height - resizeY

                                                if (newSizeX in 1..Constants.maxGameObjectSize)
                                                    it.rectangle.width = newSizeX
                                                if (newSizeY in 1..Constants.maxGameObjectSize)
                                                    it.rectangle.height = newSizeY
                                            }
                                        }
                                    }
                                }
                                SelectGOMode.MOVE -> {
                                    val moveX = selectGORect.x + selectGORect.width / 2 - mousePosInWorld.x
                                    val moveY = selectGORect.y + selectGORect.height / 2 - mousePosInWorld.y

                                    moveGameObjects(-moveX, -moveY)
                                }
                            }
                        }
                        // Le bouton gauche n'est pas appuyé pendant cette frame
                    } else {
                        selectGameObjectMode = SelectGOMode.NO_MODE

                        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                            clearSelectGameObjects()
                            editorMode = EditorMode.NO_MODE
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_LEFT.key)) {
                            moveGameObjects(-1, 0)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_RIGHT.key)) {
                            moveGameObjects(1, 0)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_UP.key)) {
                            moveGameObjects(0, 1)
                        }
                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_MOVE_ENTITY_DOWN.key)) {
                            moveGameObjects(0, -1)
                        }

                        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key))
                            editorMode = EditorMode.COPY_GO
                    }
                }
                EditorMode.COPY_GO -> {
                    if (selectGameObjects.isEmpty() && selectGameObject == null /* Permet de vérifier si on est pas entrain d'ajouter un nouveau gameObject */) {
                        editorMode = EditorMode.NO_MODE
                        return
                    }

                    if ((Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key))
                            && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                        val underMouseGO = findGameObjectUnderMouse()
                        if (underMouseGO != null) {
                            if (selectGameObjects.contains(underMouseGO))
                                selectGameObject = underMouseGO
                            else {
                                clearSelectGameObjects()
                                addSelectGameObject(underMouseGO)
                            }
                        } else {
                            clearSelectGameObjects()
                            editorMode = EditorMode.NO_MODE
                        }
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        if (selectGameObject!!.unique)
                            return
                        val copySelectGO = SerializationFactory.copy(selectGameObject!!)

                        var posX = copySelectGO.rectangle.x
                        var posY = copySelectGO.rectangle.y

                        val width = copySelectGO.rectangle.width
                        val height = copySelectGO.rectangle.height

                        var moveToCopyGO = true

                        var useMousePos = false
                        if (selectGameObject!!.container != null) {
                            when {
                                Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_LEFT.key) -> {
                                    val minXPos = let {
                                        var x = selectGameObject!!.position().x
                                        selectGameObjects.forEach {
                                            x = Math.min(x, it.position().x)
                                        }
                                        x
                                    }
                                    posX = minXPos - width
                                }
                                Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_RIGHT.key) -> {
                                    val maxXPos = let {
                                        var x = selectGameObject!!.position().x
                                        selectGameObjects.forEach {
                                            x = Math.max(x, it.position().x)
                                        }
                                        x
                                    }
                                    posX = maxXPos + width
                                }
                                Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_DOWN.key) -> {
                                    val minYPos = let {
                                        var y = selectGameObject!!.position().y
                                        selectGameObjects.forEach {
                                            y = Math.min(y, it.position().y)
                                        }
                                        y
                                    }
                                    posY = minYPos - height
                                }
                                Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_UP.key) -> {
                                    val maxYPos = let {
                                        var y = selectGameObject!!.position().y
                                        selectGameObjects.forEach {
                                            y = Math.max(y, it.position().y)
                                        }
                                        y
                                    }
                                    posY = maxYPos + height
                                }
                                else -> useMousePos = true
                            }
                        } else
                            useMousePos = true

                        if (useMousePos) {
                            posX = Math.min(level.matrixRect.width - width, // Les min et max permettent de rester dans le cadre de la matrix
                                    Math.max(0, mousePosInWorld.x - width / 2))
                            posY = Math.min(level.matrixRect.height - height,
                                    Math.max(0, mousePosInWorld.y - height / 2))

                            // Permet de vérifier si le gameObject copié est nouveau ou pas (si il est nouveau, ça veut dire qu'il n'a pas encore de container)è
                            if (selectGameObject!!.container != null)
                                moveToCopyGO = false
                        }

                        copySelectGO.rectangle.position = Point(posX, posY)

                        level.addGameObject(copySelectGO)

                        val copyGameObjects = mutableListOf(copySelectGO)

                        selectGameObjects.filter { it !== selectGameObject && !it.unique }.forEach {
                            val deltaX = it.position().x - selectGameObject!!.position().x
                            val deltaY = it.position().y - selectGameObject!!.position().y

                            level.addGameObject(SerializationFactory.copy(it).apply {
                                this.rectangle.position = Point(copySelectGO.position().x + deltaX, copySelectGO.position().y + deltaY)
                                copyGameObjects += this
                            })
                        }

                        if (moveToCopyGO) {
                            clearSelectGameObjects()
                            copyGameObjects.forEach { addSelectGameObject(it) }
                        }
                    }
                }
                EditorMode.SELECT_POINT -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                        onSelectPoint(mousePosInWorld)
                        editorMode = EditorMode.NO_MODE
                    }
                }
                EditorMode.TRY_LEVEL -> {
                }
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                val gameObject = findGameObjectUnderMouse()
                if (gameObject != null) {
                    removeGameObject(gameObject)
                    if (selectGameObjects.contains(gameObject))
                        selectGameObjects.remove(gameObject)
                } else if (selectGameObjects.isNotEmpty()) {
                    selectGameObjects.forEach { removeGameObject(it) }
                    clearSelectGameObjects()
                    editorMode = EditorMode.NO_MODE
                }
            }

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick && Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                if (selectGameObjects.isNotEmpty()) {
                    val underMouseGO = findGameObjectUnderMouse()
                    if (underMouseGO != null) {
                        if (selectGameObjects.contains(underMouseGO)) {
                            selectGameObjects.remove(underMouseGO)
                            if (selectGameObject === underMouseGO)
                                selectGameObject = null
                        } else
                            addSelectGameObject(underMouseGO)
                    }
                }
            }

            latestLeftButtonClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
            latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
            latestMousePos = mousePos
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (editorMode == EditorMode.TRY_LEVEL)
                finishTryLevel()
            else
                showExitWindow()
        }
    }

    override fun dispose() {
        super.dispose()
        editorFont.dispose()
    }

    private fun updateCamera() {
        var moveCameraX = 0f
        var moveCameraY = 0f

        if (editorMode == EditorMode.TRY_LEVEL) {
            (gameObjectContainer as Level).moveCameraToFollowGameObject(camera, true)
        } else {
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_LEFT.key))
                moveCameraX -= cameraMoveSpeed
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_RIGHT.key))
                moveCameraX += cameraMoveSpeed
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_UP.key))
                moveCameraY += cameraMoveSpeed
            if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_DOWN.key))
                moveCameraY -= cameraMoveSpeed
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key))
                camera.zoom -= 0.02f
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key))
                camera.zoom += 0.02f
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key))
                camera.zoom = 1f

            val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
            val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)
            camera.position.set(x, y, 0f)
        }
        camera.update()
    }

    /**
     * Permet d'ajouter une nouvelle entité sélectionnée
     */
    private fun addSelectGameObject(gameObject: GameObject) {
        selectGameObjects.add(gameObject)

        if (selectGameObjects.size == 1) {
            selectGameObject = gameObject
        }
    }

    /**
     * Permet de supprimer les entités sélectionnées de la liste -> ils ne sont pas supprimés du niveau, juste déséléctionnés
     */
    private fun clearSelectGameObjects() {
        selectGameObjects.clear()
        selectGameObject = null
    }

    /**
     * Permet de retourner l'entité sous le pointeur par rapport à son layer
     */
    private fun findGameObjectUnderMouse(): GameObject? {
        stage.keyboardFocus = null // Enlève le focus sur la fenêtre active permettant d'utiliser par exemple les touches de déplacement même si le joueur était dans un textField l'étape avant

        val mousePosInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).toPoint()

        val gameObjectsUnderMouse = gameObjectContainer.getGameObjectsData().filter { it.rectangle.contains(mousePosInWorld) }

        if (!gameObjectsUnderMouse.isEmpty()) {
            val goodLayerGameObject = gameObjectsUnderMouse.find { it.layer == selectLayer }
            return if (goodLayerGameObject != null)
                goodLayerGameObject
            else {
                val gameObject = gameObjectsUnderMouse.first()
                selectLayer = gameObject.layer
                gameObject
            }
        }

        return null
    }

    private fun removeGameObject(gameObject: GameObject) {
        if (gameObject.id == level.playerUUID)
            return
        if (gameObject === selectGameObject) {
            selectGameObject = null
        }
        gameObject.removeFromParent()
    }

    private fun launchTryLevel() {
        editorMode = EditorMode.TRY_LEVEL

        gameObjectContainer = SerializationFactory.copy(level).apply { exit = { finishTryLevel() } }

        backupTryModeCameraPos = camera.position.cpy()

        hideUI()
    }

    private fun finishTryLevel() {
        clearSelectGameObjects()
        editorMode = EditorMode.NO_MODE

        gameObjectContainer = level
        camera.position.set(backupTryModeCameraPos)

        showUI()
    }

    private fun addPrefab(prefab: Prefab) {
        prefabs.add(prefab)
        SerializationFactory.serializeToFile(prefab, (Constants.prefabsDirPath + prefab.name + Constants.prefabExtension).toLocalFile())
    }

    //region UI

    /**
     * Permet d'afficher la fenêtre permettant de sauvegarder et quitter l'éditeur
     */
    private fun showExitWindow() {
        stage + window("Quitter") {
            setSize(150f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    onClick {
                        try {
                            SerializationFactory.serializeToFile(level, level.levelPath.toLocalFile())
                            this@window.remove()
                        } catch (e: Exception) {
                            Log.error(e, message = { "Erreur lors de l'enregistrement du niveau !" })
                        }
                    }
                }
                textButton("Essayer le niveau") {
                    onClick {
                        launchTryLevel()
                        this@window.remove()
                    }
                }
                textButton("Options du niveau") {
                    onClick {
                        showSettingsLevelWindow()
                    }
                }
                textButton("Quitter") {
                    onClick { PCGame.setScene(MainMenuScene()) }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre permettant de modifier les paramètres du niveau
     */
    private fun showSettingsLevelWindow() {
        fun switchBackground(i: Int) {
            var index = PCGame.getBackgrounds().indexOfFirst { it == level.backgroundPath?.toLocalFile() }
            if (index == -1)
                index = 0

            val newIndex = index + i
            if (newIndex >= 0 && newIndex < PCGame.getBackgrounds().size) {
                val newBackground = PCGame.getBackgrounds()[newIndex]
                level.backgroundPath = newBackground.path()
                backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(newBackground.path()).asset
            }

        }

        stage + window("Paramètres du niveau") {
            setSize(200f, 100f)
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                horizontalGroup {
                    space(10f)

                    textButton("<-") {
                        onClick {
                            switchBackground(-1)
                        }
                    }

                    label("Fond d'écran")

                    textButton("->") {
                        onClick {
                            switchBackground(1)
                        }
                    }
                }
            }
        }
    }

    fun addWidgetForValue(table: VisTable, get: () -> Any, set: (newValue: Any) -> Unit, exposeEditor: ExposeEditor) {
        val value = get()

        when (value) {
            is Boolean -> {
                table.add(VisCheckBox("", value).apply {
                    onChange {
                        set(isChecked)
                    }
                })
            }
            is Int -> {
                when (exposeEditor.customType) {
                    CustomType.DEFAULT -> {
                        val intModel = IntSpinnerModel(value, exposeEditor.minInt, exposeEditor.maxInt)
                        table.add(Spinner("", intModel).apply {
                            onChange {
                                set(intModel.value)
                            }
                        })
                    }
                    CustomType.KEY_INT -> {
                        table.add(VisTextArea(Input.Keys.toString(value)).apply {
                            isReadOnly = true
                            onKeyUp {
                                val keyStr = Input.Keys.toString(it)
                                val key = Input.Keys.valueOf(keyStr)

                                if (key != -1) {
                                    text = keyStr
                                    set(key)
                                }
                            }
                        }).width(50f)
                    }
                }
            }
            is String -> {
                table.add(VisTextField(value).apply {
                    onChange {
                        set(text)
                    }
                }).width(100f)
            }
            is Point -> {
                var value: Point = value

                val label = table.add(VisLabel(value.toString())).actor

                val xIntModel = IntSpinnerModel(value.x, 0, level.matrixRect.width)
                val yIntModel = IntSpinnerModel(value.y, 0, level.matrixRect.height)

                fun setPointValue(newValue: Point) {
                    value = newValue
                    label.setText(newValue.toString())
                    xIntModel.setValue(newValue.x, false)
                    yIntModel.setValue(newValue.y, false)
                    set(newValue)
                }

                table.add(verticalGroup {
                    space(10f)

                    spinner("X", xIntModel) {
                        onChange {
                            setPointValue(value.copy(x = xIntModel.value, y = value.y))
                        }
                    }

                    spinner("Y", yIntModel) {
                        onChange {
                            setPointValue(value.copy(x = value.x, y = yIntModel.value))
                        }
                    }
                })

                table.add(VisTextButton("Sélectionner").apply {
                    onClick {
                        editorMode = EditorScene.EditorMode.SELECT_POINT
                        onSelectPoint.register(true) {
                            setPointValue(it)
                            showUI()
                        }
                        hideUI()
                    }
                })
            }
            is Size -> {
                var value: Size = value

                val label = table.add(VisLabel(value.toString())).actor

                val widthIntModel = IntSpinnerModel(value.width, exposeEditor.minInt, exposeEditor.maxInt)
                val heightIntModel = IntSpinnerModel(value.height, exposeEditor.minInt, exposeEditor.maxInt)

                fun setSizeValue(newValue: Size) {
                    value = newValue
                    label.setText(newValue.toString())
                    widthIntModel.setValue(newValue.width, false)
                    heightIntModel.setValue(newValue.height, false)
                    set(newValue)
                }

                table.add(verticalGroup {
                    space(10f)

                    spinner("L", widthIntModel) {
                        onChange {
                            setSizeValue(value.copy(width = widthIntModel.value, height = value.height))
                        }
                    }

                    spinner("H", heightIntModel) {
                        onChange {
                            setSizeValue(value.copy(width = value.width, height = heightIntModel.value))
                        }
                    }
                })
            }
            is Enum<*> -> {
                table.add(VisSelectBox<String>().apply {
                    val enumConstants = value.javaClass.enumConstants

                    this.items = enumConstants.map { (it as Enum<*>).name }.toGdxArray()

                    val index = enumConstants.indexOfFirst { it == value }
                    selectedIndex = if (index == -1) 0 else index

                    onChange {
                        set(enumConstants[selectedIndex])
                    }
                }).width(125f)
            }
            is Action -> {
                table.add(VisTextButton("Éditer l'action").apply {
                    onClick {
                        showEditActionWindow(value) { set(it) }
                    }
                })
            }
        }
    }

    private fun insertExposeEditorFields(instance: Any, table: VisTable) {
        if (instance is CustomEditorImpl) {
            instance.insertChangeProperties(table, this@EditorScene)
            table.row()
        }

        ReflectionUtility.getAllFieldsOf(instance.javaClass).filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
            field.isAccessible = true

            val exposeField = field.getAnnotation(ExposeEditor::class.java)

            table.add(VisLabel("${if (exposeField.customName.isBlank()) field.name else exposeField.customName} : "))

            addWidgetForValue(table, { field.get(instance) }, { field.set(instance, it) }, exposeField)

            table.row()
        }
    }

    private fun showAddPrefabWindow(gameObject: GameObject) {
        stage + window("Ajouter un prefab") {
            setSize(200f, 200f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)

            addCloseButton()

            table(defaultSpacing = true) {
                verticalGroup {
                    space(10f)

                    label("Nom : ")
                    val nameField = textField { }

                    label("Auteur : ")
                    val authorField = textField { }

                    textButton("Ajouter") {
                        onClick {
                            if (!nameField.isEmpty) {
                                addPrefab(Prefab(nameField.text, authorField.text, gameObject))
                                this@window.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    fun showEditActionWindow(action: Action?, setAction: (Action) -> Unit) {
        stage + window("Éditer l'action") {
            setSize(300f, 225f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            verticalGroup vgroup@ {
                space(10f)

                selectBox<String> {
                    var instance = action ?: EmptyAction()

                    val actionsList = gdxArrayOf<KClass<out Action>>()
                    FastClasspathScanner(Action::class.java.`package`.name).matchClassesImplementing(Action::class.java, { actionsList.add(it.kotlin) }).scan()

                    actionsList.removeAll { it.isAbstract || !ReflectionUtility.hasNoArgConstructor(it) }

                    this.items = actionsList.map { it.simpleName ?: "Nom inconnu" }.toGdxArray()

                    val findIndex = actionsList.indexOfFirst { it.isInstance(instance) }

                    this.selectedIndex = if (findIndex == -1) 0 else findIndex

                    onChange {
                        if (!actionsList[selectedIndex].isInstance(instance))
                            instance = ReflectionUtility.findNoArgConstructor(actionsList[selectedIndex])!!.newInstance()

                        this@vgroup.clear()
                        this@vgroup.addActor(this@selectBox)

                        this@vgroup.table(defaultSpacing = true) {
                            insertExposeEditorFields(instance, this)

                            row()

                            this@vgroup.textButton(if (action == null) "Ajouter" else "Modifier") {
                                onClick {
                                    setAction(instance)
                                    this@window.remove()
                                }
                            }
                        }
                    }.changed(ChangeListener.ChangeEvent(), this)
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour créer une entité
     */
    private fun showAddGameObjectWindow() {
        stage + window("Ajouter un gameObject") {
            isModal = true
            addCloseButton()

            fun finishBuild(gameObject: GameObject) {
                clearSelectGameObjects()
                selectGameObject = gameObject
                editorMode = EditorMode.COPY_GO
                this.remove()
            }

            verticalGroup {
                space(10f)
                verticalGroup {
                    space(10f)

                    val prefabSelectBox = selectBox<Prefab> {
                        this.items = prefabs.toGdxArray()
                    }

                    textButton("Ajouter") {
                        onClick {
                            val go = prefabSelectBox.selected.create(Point())
                            finishBuild(go)

                            UIUtility.showDialog(this@EditorScene.stage, "Éditer le gameObject?", "Voulez-vous éditer le gameObject créer?", listOf("Éditer", "Fermer")) {
                                if (it == 0)
                                    showEditGameObjectWindow(go)
                            }

                        }
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }
    }

    private fun showAddGameObjectStateWindow(gameObjectStates: Set<GameObjectState>, onCreateState: (GameObjectState) -> Unit) {
        stage + window("Ajouter un state") {
            setSize(300f, 100f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)

            addCloseButton()

            table(defaultSpacing = true) {
                val selectBox = selectBox<String> {
                    this.items = gdxArrayOf("State vide", *gameObjectStates.map { "Copier de : ${it.name}" }.toTypedArray())
                }

                row()

                horizontalGroup {
                    space(10f)

                    label("Nom : ")
                    val nameField = textField { }

                    textButton("Ajouter") {
                        onClick {
                            if (!nameField.isEmpty) {
                                onCreateState(
                                        if (selectBox.selectedIndex == 0)
                                            GameObjectState(nameField.text)
                                        else
                                            SerializationFactory.copy(gameObjectStates.elementAt(selectBox.selectedIndex - 1)).apply { this.name = nameField.text }
                                )
                                this@window.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAddComponentWindow(onCreateComp: (Component) -> Unit) {
        val componentsList = gdxArrayOf<KClass<out Component>>()
        FastClasspathScanner(Component::class.java.`package`.name).matchSubclassesOf(Component::class.java, { componentsList.add(it.kotlin) }).scan()

        componentsList.removeAll { it.isAbstract || !ReflectionUtility.hasNoArgConstructor(it) }

        stage + window("Ajouter un component") {
            setSize(250f, 100f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table(defaultSpacing = true) {
                val selectBoxComp = selectBox<String> {
                    this.items = componentsList.map { it.simpleName?.removeSuffix("Component") ?: "Nom inconnu" }.toGdxArray()
                }

                textButton("Ajouter") {
                    onClick {
                        onCreateComp(ReflectionUtility.findNoArgConstructor(componentsList[selectBoxComp.selectedIndex])!!.newInstance())
                        this@window.remove()
                    }
                }
            }
        }
    }

    private fun showEditGameObjectWindow(gameObject: GameObject) {
        stage + window("Éditer un gameObject") {
            setSize(600f, 400f)
            isModal = true
            addCloseButton()

            table(defaultSpacing = true) {

                var onStateChanged: () -> Unit = {} // Permet de regénérer la liste de states pour le state initial

                table(defaultSpacing = true) gameObjectTable@ {
                    onStateChanged = {
                        this@gameObjectTable.clear()
                        insertExposeEditorFields(gameObject, this@gameObjectTable)

                        row()
                    }
                    onStateChanged()
                }

                table(defaultSpacing = true) stateTable@ {
                    selectBox<String> stateSelectBox@ {
                        fun generateItems() {
                            this.items = gameObject.getStates().map { it.name }.toGdxArray()
                        }
                        generateItems()

                        this.selectedIndex = gameObject.currentState

                        onChange {
                            this@stateTable.clear()

                            this@stateTable.horizontalGroup {
                                space(10f)

                                label("State : ")

                                addActor(this@stateSelectBox)

                                textButton("+") {
                                    onClick {
                                        showAddGameObjectStateWindow(gameObject.getStates()) {
                                            gameObject.addState(it)
                                            generateItems()
                                            this@stateSelectBox.selectedIndex = gameObject.getStates().indexOf(it)
                                            onStateChanged()
                                        }
                                    }
                                }
                                textButton("-") {
                                    this.touchable = if (this@stateSelectBox.items.size > 1) Touchable.enabled else Touchable.disabled
                                    onClick {
                                        gameObject.removeState(gameObject.currentState)
                                        generateItems()
                                        onStateChanged()
                                    }
                                }
                            }

                            this@stateTable.row()

                            this@stateTable.table(defaultSpacing = true) {
                                insertExposeEditorFields(gameObject.getCurrentState(), this)
                            }

                            this@stateTable.row()

                            this@stateTable.verticalGroup {
                                space(10f)

                                val state = gameObject.getStates().elementAt(this@stateSelectBox.selectedIndex)

                                val onAddComponent = Signal<Component>()

                                textButton("Ajouter un component") {
                                    onClick {
                                        showAddComponentWindow {
                                            state.addComponent(it)
                                            onAddComponent(it)
                                        }
                                    }
                                }

                                table(defaultSpacing = true) compTable@ {
                                    fun generateComponentsItems() = state.getComponents().mapIndexed { index, component ->
                                        val className = ReflectionUtility.simpleNameOf(component).removeSuffix("Component")
                                        "${index + 1} : " + if (component.name == className) className else component.name + "[$className]"
                                    }.toGdxArray()

                                    selectBox<String> {
                                        this.items = generateComponentsItems()

                                        onAddComponent.register { comp ->
                                            this.items = generateComponentsItems()
                                            this.selectedIndex = state.getComponents().indexOfFirst { it === comp }
                                        }

                                        onChange {
                                            this@compTable.clear()

                                            if (selectedIndex == -1)
                                                return@onChange

                                            this@compTable.add(ktx.vis.horizontalGroup {
                                                space(10f)
                                                addActor(this@selectBox)
                                                textButton("Supprimer") {
                                                    onClick {
                                                        state.removeComponent(state.getComponents().elementAt(this@selectBox.selectedIndex))
                                                        this@selectBox.items = generateComponentsItems()
                                                    }
                                                }
                                            })

                                            this@compTable.row()

                                            val instance = state.getComponents().elementAt(selectedIndex)

                                            val compPropertiesTable = VisTable()

                                            this@compTable.add(VisScrollPane(compPropertiesTable)).height(150f)

                                            insertExposeEditorFields(instance, compPropertiesTable)

                                        }.changed(ChangeListener.ChangeEvent(), this)
                                    }
                                }
                            }
                        }.changed(ChangeListener.ChangeEvent(), this)
                    }
                }

                row()

                textButton("Créer un prefab") {
                    onClick {
                        showAddPrefabWindow(gameObject)
                    }
                }
            }

            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
        }
    }


    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     * @param onAtlasSelected Méthode appelée quand l'atlasPath et la region ont été correctement sélectionnés par le joueur
     */
    fun showSelectAtlasRegionWindow(atlasFile: FileHandle? = null, onAtlasSelected: (atlasFile: FileHandle, region: String) -> Unit) {
        /**
         * Classe de donnée représentant la sélection d'une texture atlasPath
         */
        data class TextureAtlasSelect(val textureAtlas: TextureAtlas, val atlasFile: FileHandle, val atlasName: String) {
            override fun toString(): String {
                return atlasName
            }
        }

        stage + window("Sélectionner une atlas") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                val selectedBox = selectBox<TextureAtlasSelect> {
                    this.items = Utility.getFilesRecursivly(Constants.atlasDirPath.toLocalFile(), "atlas").let {
                        val list = mutableListOf<TextureAtlasSelect>()

                        it.forEachIndexed { index, file ->
                            val loadAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(file.path()).asset to file.file().nameWithoutExtension
                            if (atlasFile?.equals(file) == true) {
                                latestSelectTextureWindowAtlasIndex = index
                            }

                            list += TextureAtlasSelect(loadAtlas.first, file, loadAtlas.second)
                        }

                        list.toGdxArray()
                    }

                    if (latestSelectTextureWindowAtlasIndex < this.items.size)
                        this.selectedIndex = latestSelectTextureWindowAtlasIndex

                    onChange {
                        table.clearChildren()
                        var count = 0
                        selected.textureAtlas.regions.forEach {
                            val image = VisImage(it)

                            image.userObject = selected.atlasFile to it.name

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
                    }.changed(ChangeListener.ChangeEvent(), this)
                }
                row()

                val scroll = ScrollPane(table)
                add(scroll).size(300f, 200f).space(10f)

                row()

                add(selectedImage).size(50f, 50f).space(10f)

                row()

                textButton("Sélectionner") {
                    onClick {
                        if (selectedImage.userObject != null && selectedImage.userObject is Pair<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            val userObject = selectedImage.userObject as? Pair<FileHandle, String>
                            if (userObject != null) {
                                onAtlasSelected(userObject.first, userObject.second)
                                latestSelectTextureWindowAtlasIndex = selectedBox.selectedIndex
                                this@window.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une animation
     */
    fun showSelectAnimationWindow(atlasFile: FileHandle? = null, onAnimationSelected: (atlasFile: FileHandle, animationRegionName: String, frameDuration: Float) -> Unit) {
        /**
         * Classe de donnée représentant la sélection d'une texture atlasPath
         */
        data class TextureAtlasSelect(val textureAtlas: TextureAtlas, val atlasFile: FileHandle, val atlasName: String) {
            override fun toString(): String {
                return atlasName
            }
        }

        stage + window("Sélectionner une animation") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                val selectedBox = selectBox<TextureAtlasSelect> {
                    this.items = Utility.getFilesRecursivly(Constants.atlasDirPath.toLocalFile(), "atlas").let {
                        val list = mutableListOf<TextureAtlasSelect>()

                        it.forEachIndexed { index, file ->
                            val loadAtlas = PCGame.assetManager.loadOnDemand<TextureAtlas>(file.path()).asset to file.file().nameWithoutExtension
                            if (atlasFile?.equals(file) == true) {
                                latestSelectTextureWindowAtlasIndex = index
                            }

                            list += TextureAtlasSelect(loadAtlas.first, file, loadAtlas.second)
                        }

                        list.toGdxArray()
                    }

                    if (latestSelectTextureWindowAtlasIndex < this.items.size)
                        this.selectedIndex = latestSelectTextureWindowAtlasIndex

                    onChange {
                        table.clearChildren()
                        var count = 0

                        AnimationComponent.findAnimationRegionsNameInAtlas(selected.textureAtlas).forEach {
                            val image = VisImage(selected.textureAtlas.findRegion(it + "_0"))

                            image.userObject = selected.atlasFile to it

                            image.onClick {
                                selectedImage.drawable = image.drawable
                                selectedImage.userObject = image.userObject
                            }

                            table.add(image).size(50f, 50f).space(10f)

                            ++count
                            if (count >= 5) {
                                table.row()
                                count = 0
                            }
                        }
                    }.changed(ChangeListener.ChangeEvent(), this)
                }
                row()

                val scroll = ScrollPane(table)
                add(scroll).size(300f, 200f).space(10f)

                row()

                add(selectedImage).size(50f, 50f).space(10f)

                row()

                val floatModel = FloatSpinnerModel(1f.toString(), 0f.toString(), 10f.toString(), 0.1f.toString())
                spinner("Vitesse de l'animation", floatModel)

                row()

                textButton("Sélectionner") {
                    onClick {
                        if (selectedImage.userObject != null && selectedImage.userObject is Pair<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            val userObject = selectedImage.userObject as? Pair<FileHandle, String>
                            if (userObject != null) {
                                onAnimationSelected(userObject.first, userObject.second, floatModel.value.toFloat())
                                latestSelectTextureWindowAtlasIndex = selectedBox.selectedIndex
                                this@window.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     */
    fun showSelectTextureWindow(onTextureSelected: (textureFile: FileHandle) -> Unit) {
        stage + window("Sélectionner une texture") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                var count = 0
                Utility.getFilesRecursivly(Constants.texturesDirPath.toLocalFile(), "png").forEach {
                    val image = VisImage(PCGame.assetManager.loadOnDemand<Texture>(it.path()).asset)

                    image.userObject = it

                    image.onClick {
                        selectedImage.drawable = image.drawable
                        selectedImage.userObject = image.userObject
                    }

                    table.add(image).size(50f, 50f).space(10f)

                    ++count
                    if (count >= 5) {
                        table.row()
                        count = 0
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
                        if (selectedImage.userObject != null && selectedImage.userObject is FileHandle) {
                            onTextureSelected(selectedImage.userObject as FileHandle)
                            this@window.remove()
                        }
                    })
                }
            }
        }
    }


    /**
     * Permet d'afficher la fenêtre comportant les informations de l'entité sélectionnée
     */
    private fun showInfoGameObjectWindow() {
        stage + window("Réglages des gameObjects") {
            setSize(250f, 200f)
            setPosition(Gdx.graphics.width - width, Gdx.graphics.height - height)

            verticalGroup {
                space(10f)

                textButton("Ajouter un gameObject") {
                    onClick { showAddGameObjectWindow() }
                }

                val noGOSelectedText = "Aucun gameObject sélectionné"
                label(noGOSelectedText) {
                    onSelectGameObjectChange.register { gameObject ->
                        if (gameObject == null)
                            setText(noGOSelectedText)
                        else
                            setText(gameObject.tag.name)
                    }
                }

                textButton("Supprimer ce gameObject") {
                    touchable = Touchable.disabled

                    onSelectGameObjectChange.register { gameObject ->
                        this.touchable =
                                if (gameObject == null || gameObject.id == level.playerUUID)
                                    Touchable.disabled
                                else
                                    Touchable.enabled
                    }

                    onClick {
                        selectGameObject?.removeFromParent()
                        selectGameObject = null

                        clearSelectGameObjects()
                        editorMode = EditorMode.NO_MODE
                    }
                }

                horizontalGroup {
                    space(10f)

                    val layerLabelStartStr = "Layer : "
                    label(layerLabelStartStr) {
                        onSelectGameObjectChange.register { gameObject ->
                            this.setText(layerLabelStartStr + (gameObject?.layer ?: "/"))
                        }
                    }

                    textButton("+") {
                        onSelectGameObjectChange.register { gameObject ->
                            this.touchable = if (gameObject == null) Touchable.disabled else Touchable.enabled
                        }

                        onClick {
                            if (selectGameObject != null) {
                                selectGameObject!!.layer++
                                onSelectGameObjectChange(selectGameObject!!)
                            }
                        }
                    }
                    textButton("-") {
                        onSelectGameObjectChange.register { gameObject ->
                            this.touchable = if (gameObject == null) Touchable.disabled else Touchable.enabled
                        }

                        onClick {
                            if (selectGameObject != null) {
                                selectGameObject!!.layer--
                                onSelectGameObjectChange(selectGameObject!!)
                            }
                        }
                    }
                }

                textButton("Éditer") {
                    onSelectGameObjectChange.register { gameObject ->
                        this.touchable = if (gameObject == null) Touchable.disabled else Touchable.enabled
                    }

                    onClick {
                        if (selectGameObject != null)
                            showEditGameObjectWindow(selectGameObject!!)
                    }
                }
            }
        }
    }
//endregion
}