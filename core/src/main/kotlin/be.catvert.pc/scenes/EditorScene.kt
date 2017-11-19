package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.kotcrab.vis.ui.widget.*
import imgui.ImGui
import imgui.WindowFlags
import imgui.functionalProgramming
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray
import ktx.vis.window
/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(private val level: Level) : Scene() {
    private enum class EditorMode {
        NO_MODE, SELECT_GO, COPY_GO, SELECT_POINT, TRY_LEVEL
    }

    private enum class ResizeMode {
        FREE, PROPORTIONAL
    }

    private enum class SelectGOMode {
        NO_MODE, MOVE, RESIZE
    }

    private object EditorSceneUI {
        var gameObjectAddStateComboIndex = 0
        var gameObjectAddComponentComboIndex = 0

        var gameObjectCurrentSelectedComponent = 0

        var addGameObjectPrefabComboIndex = 0
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

    private var backupTryModeCameraPos = Vector3()

    init {
        gameObjectContainer.allowUpdatingGO = false

        Utility.getFilesRecursivly(Constants.prefabsDirPath.toLocalFile(), Constants.prefabExtension).forEach {
            prefabs.add(SerializationFactory.deserializeFromFile(it))
        }

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(level.backgroundPath.toLocalFile().path()).asset

        level.removeEntityBelowY0 = false

        onSelectGameObjectChange(null)
    }

    override fun postBatchRender() {
        super.postBatchRender()

        drawUI()

        (gameObjectContainer as Level).drawDebug()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        if (!(gameObjectContainer as Level).drawDebugCells && editorMode != EditorMode.TRY_LEVEL) {
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
                    draw(it, "Layer sélectionné : $selectLayer", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
                    draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 3)
                    draw(it, "Resize mode : ${resizeGameObjectMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 4)
                    draw(it, "Mode de l'éditeur : ${editorMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 5)
                } else {
                    draw(it, "Test du niveau..", 10f, Gdx.graphics.height - lineHeight * 2)
                }
            }
            it.projectionMatrix = camera.combined
        }
    }

    override fun update() {
        super.update()

        updateCamera()

        if (!isUIHover && editorMode != EditorMode.TRY_LEVEL) {
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

    /*inline fun <reified T : Any> addWidgetArray(table: VisTable, gameObject: GameObject, crossinline getItemName: (index: Int) -> String, crossinline getItemExposeEditor: (index: Int) -> ExposeEditor, crossinline createItem: () -> T, crossinline getArray: () -> Array<T>, crossinline setArray: (newArray: Array<T>) -> Unit) {
        val valueTables = table.add(VisTable()).height(100f).actor

        var onListChanged: () -> Unit = {}
        val setArray: (Array<T>) -> Unit = { setArray(it); onListChanged() }

        onListChanged = {
            valueTables.clear()

            valueTables.add(VisScrollPane(ktx.vis.table() {
                getArray().forEachIndexed { index, it ->
                    //addWidgetValue(this, gameObject, getItemName(index), { it }, { setArray(getArray().apply { set(index, it as T) }) }, getItemExposeEditor(index), false)

                    textButton("Suppr.") {
                        onClick {
                            setArray(getArray().toGdxArray().apply { removeIndex(index) }.toArray())
                        }
                    }

                    row()
                }
            })).height(100f)
        }
        onListChanged()

        table.row()

        table.add(VisTextButton("Ajouter").apply { onClick { setArray(getArray() + createItem()); } })

        table.row()
    }*/

    fun addImguiWidget(gameObject: GameObject, labelName: String, get: () -> Any, set: (Any) -> Unit, exposeEditor: ExposeEditor) {
        val value = get()
        with(ImGui) {
            when(value) {
                is Action -> {
                    if(treeNode(labelName)) {
                        val index = intArrayOf(PCGame.actionsClasses.indexOfFirst { it.isInstance(value) })

                        if(combo("action", index, PCGame.actionsClasses.map { it.simpleName ?: "Nom inconnu" })) {
                            set(ReflectionUtility.findNoArgConstructor(PCGame.actionsClasses[index[0]])!!.newInstance())
                        }

                        if(treeNode("Propriétés")) {
                            insertImguiExposeEditorField(value, gameObject)
                            treePop()
                        }
                        treePop()
                    }
                }
                is CustomEditorImpl -> value.insertImgui(gameObject, this@EditorScene)
                is Boolean -> {
                    if(checkbox(labelName, booleanArrayOf(value)))
                        set(!value)
                }
                is Int -> {
                    when(exposeEditor.customType) {
                        CustomType.DEFAULT -> {
                            val value = intArrayOf(value) // TODO changer en inputInt
                            if(sliderInt(labelName, value, exposeEditor.minInt, exposeEditor.maxInt))
                                set(value[0])
                        }
                        CustomType.KEY_INT -> {
                            val value = Input.Keys.toString(value).toCharArray()
                            if(inputText(labelName, value))
                                set(Input.Keys.valueOf(String(value)))
                        }
                    }
                }
                is Rect -> {
                    if(collapsingHeader(labelName)) {
                        addImguiWidget(gameObject, "Position", { value.position }, { value.position = it as Point }, ExposeEditorFactory.createExposeEditor(maxInt = level.matrixRect.width))
                        addImguiWidget(gameObject, "Taille", { value.size }, { value.size = it as Size }, ExposeEditorFactory.createExposeEditor(maxInt = Constants.maxGameObjectSize))
                    }
                }
                is Size -> {
                    if(treeNode(labelName)) {
                        val width = intArrayOf(value.width)
                        if(sliderInt("Largeur", width, exposeEditor.minInt, exposeEditor.maxInt))
                            set(value.copy(width = width[0], height = value.height))

                        val height = intArrayOf(value.height)
                        if(sliderInt("Hauteur", height, exposeEditor.minInt, exposeEditor.maxInt))
                            set(value.copy(width = value.width, height = height[0]))

                        treePop()
                    }
                }
                is Point -> {
                    if(treeNode(labelName)) {
                        val x = intArrayOf(value.x)
                        if (sliderInt("X", x, exposeEditor.minInt, exposeEditor.maxInt))
                            set(value.copy(x = x[0], y = value.y))

                        val y = intArrayOf(value.y)
                        if(sliderInt("Y", y, exposeEditor.minInt, exposeEditor.maxInt))
                            set(value.copy(x = value.x, y = y[0]))

                        treePop()
                    }
                }
                is String -> {
                    val value = value.toCharArray()
                    if(inputText(labelName, value))
                        set(value.toString())
                }
                is Enum<*> -> {
                    val enumConstants = value.javaClass.enumConstants

                    val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == value })

                    pushItemWidth(150f)
                    if(combo(labelName, selectedIndex, enumConstants.map { (it as Enum<*>).name}))
                        set(enumConstants[selectedIndex[0]])
                    popItemWidth()
                }
                else -> {
                    text(ReflectionUtility.simpleNameOf(value))
                }
            }
        }
    }

    private fun insertImguiExposeEditorField(instance: Any, gameObject: GameObject) {
        val dialogsImgui = mutableListOf<CustomEditorImpl>()
        ReflectionUtility.getAllFieldsOf(instance.javaClass).filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
            field.isAccessible = true

            val exposeField = field.getAnnotation(ExposeEditor::class.java)

            val value = field.get(instance)
            if(value is CustomEditorImpl)
                dialogsImgui.add(value)
            addImguiWidget(gameObject, if (exposeField.customName.isBlank()) field.name else exposeField.customName, { field.get(instance) }, { field.set(instance, it) }, exposeField)
        }

        if (instance is CustomEditorImpl) {
            instance.insertImgui(gameObject, this@EditorScene)
            instance.insertImguiPopup(gameObject, this@EditorScene)

            dialogsImgui.forEach { it.insertImguiPopup(gameObject, this@EditorScene) }
        }
    }

    private fun drawUI() {
        with(ImGui) {

            drawMainMenuBar()

            if(selectGameObject != null)
                drawInfoGameObjectWindow(selectGameObject!!)
        }
    }

    private fun drawMainMenuBar() {
        val addGameObjectTitle = "Ajouter un gameObject"
        with(ImGui) {
            functionalProgramming.mainMenuBar {
                functionalProgramming.menu("Fichier") {
                    functionalProgramming.menuItem("Quitter") {
                        showExitWindow()
                    }
                }

                functionalProgramming.menuItem("Ajouter un gameObject") {
                    openPopup(addGameObjectTitle)
                }

                if(beginPopupModal(addGameObjectTitle, extraFlags = WindowFlags.AlwaysAutoResize.i)) {
                    combo("prefabs", EditorSceneUI::addGameObjectPrefabComboIndex, prefabs.map { it.name })
                    if(button("Créer")) {
                        val gameObject = prefabs.elementAt(EditorSceneUI.addGameObjectPrefabComboIndex).create(Point())

                        clearSelectGameObjects()
                        selectGameObject = gameObject
                        editorMode = EditorMode.COPY_GO

                        closeCurrentPopup()
                    }
                    sameLine()
                    if(button("Annuler"))
                        closeCurrentPopup()
                    endPopup()
                }
            }
        }
    }

    private fun drawInfoGameObjectWindow(gameObject: GameObject) {
        val createPrefabTitle = "Créer un prefab"
        val newStateTitle = "Nouveau state"
        val addComponentTitle = "Ajouter un component"

        with(ImGui) {
            functionalProgramming.window("Réglages du gameObject", null, WindowFlags.AlwaysAutoResize.i) {
                if(button("Supprimer ce gameObject")) {
                    gameObject.removeFromParent()
                    if(selectGameObject === gameObject) {
                        selectGameObject = null
                        clearSelectGameObjects()
                        editorMode = EditorMode.NO_MODE
                    }
                }

                if(button("Créer un prefab"))
                    openPopup(createPrefabTitle)

                insertImguiExposeEditorField(gameObject, gameObject)

                separator()

                pushItemWidth(100f)
                combo("state", gameObject::currentState, gameObject.getStates().map { it.name })
                popItemWidth()

                sameLine()

                if(button("Ajouter un state")) {
                    openPopup(newStateTitle)
                }
                sameLine()
                if(button("Suppr. cet state")) {
                    if(gameObject.getStates().size > 1)
                        gameObject.removeState(EditorSceneUI.gameObjectAddStateComboIndex - 1)
                }

                pushItemWidth(100f)
                combo("components", EditorSceneUI::gameObjectCurrentSelectedComponent, gameObject.getCurrentState().getComponents().map { it.name })
                popItemWidth()

                sameLine()
                if(button("Ajouter un comp.")) {
                    openPopup(addComponentTitle)
                }
                sameLine()
                if(button("Suppr. ce comp.")) {
                    gameObject.getCurrentState().removeComponent(gameObject.getCurrentState().getComponents().elementAt(EditorSceneUI.gameObjectCurrentSelectedComponent))
                }

                insertImguiExposeEditorField(gameObject.getCurrentState().getComponents().elementAt(EditorSceneUI.gameObjectCurrentSelectedComponent), gameObject)

                if(beginPopupModal(createPrefabTitle, extraFlags = WindowFlags.AlwaysAutoResize.i)) {
                    val name = "test".toCharArray() // TODO inputtext
                    val author = "Catvert".toCharArray()

                    inputText("Nom", name)
                    inputText("Auteur", author)

                    if(button("Ajouter")) {
                        addPrefab(Prefab(String(name), String(author), gameObject))
                        closeCurrentPopup()
                    }

                    endPopup()
                }

                if(beginPopupModal(newStateTitle, extraFlags = WindowFlags.AlwaysAutoResize.i)) {
                    val comboItems = mutableListOf("State vide").apply { addAll(gameObject.getStates().map { "Copier de : ${it.name}" }) }
                    combo("type", EditorSceneUI::gameObjectAddStateComboIndex, comboItems)
                    inputText("Nom", "test".toCharArray())

                    if(button("Ajouter")) {
                        val stateName = "test" // TODO wait for inputText imgui
                        if(EditorSceneUI.gameObjectAddStateComboIndex == 0)
                            gameObject.addState(stateName) {}
                        else
                            gameObject.addState(SerializationFactory.copy(gameObject.getStates().elementAt(EditorSceneUI.gameObjectAddStateComboIndex - 1)).apply { name = stateName })
                        closeCurrentPopup()
                    }

                    sameLine()

                    if(button("Annuler"))
                        closeCurrentPopup()

                    endPopup()
                }

                if(beginPopupModal(addComponentTitle, extraFlags = WindowFlags.AlwaysAutoResize.i)) {
                    combo("component", EditorSceneUI::gameObjectAddComponentComboIndex, PCGame.componentsClasses.map { it.simpleName?: "Nom inconnu" })

                    if(button("Ajouter")) {
                        gameObject.getCurrentState().addComponent(ReflectionUtility.findNoArgConstructor(PCGame.componentsClasses[EditorSceneUI.gameObjectAddComponentComboIndex])!!.newInstance())
                        closeCurrentPopup()
                    }

                    endPopup()
                }
            }
        }
    }
//endregion
}