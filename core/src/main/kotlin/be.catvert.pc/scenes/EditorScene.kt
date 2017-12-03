package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import glm_.vec2.Vec2
import imgui.*
import imgui.functionalProgramming.mainMenuBar
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuItem
import ktx.app.use
import ktx.assets.toAbsoluteFile
import ktx.assets.toLocalFile
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog


/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(val level: Level) : Scene(level.background) {
    enum class EditorMode {
        NO_MODE, SELECT, COPY, SELECT_POINT, SELECT_GO, TRY_LEVEL
    }

    private enum class ResizeMode {
        FREE, PROPORTIONAL
    }

    private enum class SelectGOMode {
        NO_MODE, MOVE, RESIZE
    }

    private data class GridMode(var active: Boolean = false, var offsetX: Int = 0, var offsetY: Int = 0, var cellWidth: Int = 50, var cellHeight: Int = 50) {
        fun putGameObject(walkRect: Rect, point: Point, gameObject: GameObject) {
            walkCells(walkRect) {
                if (it.contains(point)) {
                    gameObject.box = it
                }
            }
        }

        fun walkCells(walkRect: Rect, walk: (cellRect: Rect) -> Unit) {
            for (x in (walkRect.x + offsetX)..(walkRect.x + walkRect.width + offsetX) step cellWidth) {
                for (y in (walkRect.y + offsetY)..(walkRect.y + walkRect.height + offsetY) step cellHeight) {
                    walk(Rect(x, y, cellWidth, cellHeight))
                }
            }
        }
    }

    /**
     * Classe de donnée permettant de créer le box de sélection
     */
    private data class SelectRectangleData(var startPosition: Point, var endPosition: Point, var rectangleStarted: Boolean = false) {
        fun getRect(): Rect {
            val minX = Math.min(startPosition.x, endPosition.x)
            val minY = Math.min(startPosition.y, endPosition.y)
            val maxX = Math.max(startPosition.x, endPosition.x)
            val maxY = Math.max(startPosition.y, endPosition.y)

            return Rect(minX, minY, (maxX - minX), (maxY - minY))
        }
    }

    private class EditorSceneUI(background: Background) {
        var gameObjectAddStateComboIndex = 0
        var gameObjectAddComponentComboIndex = 0

        var gameObjectCurrentStateIndex = 0

        var gameObjectCurrentComponentIndex = 0

        val settingsLevelStandardBackgroundIndex = intArrayOf(-1)
        val settingsLevelParallaxBackgroundIndex = intArrayOf(-1)

        val settingsLevelBackgroundType: ImguiHelper.Item<Enum<*>> = ImguiHelper.Item(background.type)

        var showSaveLevelExitWindow = false

        init {
            when (background.type) {
                BackgroundType.Standard -> settingsLevelStandardBackgroundIndex[0] = PCGame.standardBackgrounds().indexOfFirst { it.backgroundFile.get() == (background as StandardBackground).backgroundFile.get() }
                BackgroundType.Parallax -> settingsLevelParallaxBackgroundIndex[0] = PCGame.parallaxBackgrounds().indexOfFirst { it.parallaxDataFile.get() == (background as ParallaxBackground).parallaxDataFile.get() }
            }
        }
    }

    override var gameObjectContainer: GameObjectContainer = level

    override val camera: OrthographicCamera = OrthographicCamera(Constants.levelCameraRatio, Constants.levelCameraRatio * (Gdx.graphics.height.toFloat() / Gdx.graphics.width))

    private val shapeRenderer = ShapeRenderer()

    private val editorFont = BitmapFont(Constants.editorFontPath)

    private val cameraMoveSpeed = 10f

    private var editorMode = EditorMode.NO_MODE

    private val gridMode = GridMode()

    private var selectGameObjectMode = SelectGOMode.NO_MODE
    private var resizeGameObjectMode = ResizeMode.PROPORTIONAL

    private val selectGameObjects = mutableSetOf<GameObject>()
    private var selectGameObject: GameObject? = null

    private val onSelectPoint = Signal<Point>()
    private val onSelectGO = Signal<GameObject>()

    private val prefabs = mutableSetOf(*PrefabFactory.values().map { it.prefab }.toTypedArray())

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
     * La position du début et de la fin du box
     */
    private val selectRectangleData = SelectRectangleData(Point(), Point())

    private var backupTryModeCameraPos = Vector3()

    private val editorSceneUI = EditorSceneUI(level.background)

    init {
        gameObjectContainer.allowUpdatingGO = false

        Utility.getFilesRecursivly(Constants.prefabsDirPath, Constants.prefabExtension).forEach {
            prefabs.add(SerializationFactory.deserializeFromFile(it))
        }
    }

    override fun postBatchRender() {
        super.postBatchRender()

        drawUI()

        (gameObjectContainer as Level).drawDebug()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        shapeRenderer.color = Color.GOLDENROD
        shapeRenderer.rect(level.matrixRect)

        if (gridMode.active) {
            shapeRenderer.color = Color.DARK_GRAY
            gridMode.walkCells(level.activeRect) {
                shapeRenderer.rect(it)
            }
        }

        /**
         * Dessine les gameObjects qui n'ont pas de renderableComponent avec un box noir
         */
        (gameObjectContainer as? Level)?.apply {
            getAllGameObjectsInCells(getActiveGridCells()).forEach {
                if (it.getCurrentState().getComponent<AtlasComponent>()?.data?.isEmpty() != false) {
                    shapeRenderer.color = Color.GRAY
                    shapeRenderer.rect(it.box)
                }

                if (it.layer == selectLayer || editorMode == EditorMode.TRY_LEVEL) {
                    it.getCurrentState().setAlphaRenderable(1f)
                } else {
                    it.getCurrentState().setAlphaRenderable(0.5f)
                }
            }
        }

        when (editorMode) {
            EditorMode.NO_MODE -> {
                /**
                 * Dessine le box en cour de création
                 */
                if (selectRectangleData.rectangleStarted) {
                    shapeRenderer.color = Color.BLUE
                    shapeRenderer.rect(selectRectangleData.getRect())
                }
            }
            EditorMode.SELECT -> {
                /**
                 * Dessine un box autour des gameObjects sélectionnés
                 */
                selectGameObjects.forEach {
                    shapeRenderer.color = Color.RED
                    if (it === selectGameObject) {
                        shapeRenderer.color = Color.CORAL
                    }
                    shapeRenderer.rect(it.box)
                }

                if (selectGameObject != null) {
                    val rect = selectGameObject!!.box

                    when (selectGameObjectMode) {
                        SelectGOMode.NO_MODE -> {
                            shapeRenderer.color = Color.RED; shapeRenderer.circle(rect.x + rect.width.toFloat(), rect.y + rect.height.toFloat(), 10f)
                        }
                        SelectGOMode.RESIZE -> {
                            shapeRenderer.color = Color.BLUE; shapeRenderer.circle(rect.x + rect.width.toFloat(), rect.y + rect.height.toFloat(), 15f)
                        }
                        SelectGOMode.MOVE -> {
                        }
                    }
                }
            }
            EditorMode.COPY -> {
                selectGameObjects.forEach {
                    if (it === selectGameObject)
                        shapeRenderer.color = Color.GREEN
                    else
                        shapeRenderer.color = Color.OLIVE
                    shapeRenderer.rect(it.box)
                }
            }
            EditorMode.TRY_LEVEL -> {
            }
            EditorMode.SELECT_POINT -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un point")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 2)
                }
            }
            EditorMode.SELECT_GO -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un gameObject")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 2)
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
            level.activeRect.set(Size((camera.viewportWidth * camera.zoom).toInt(), (camera.viewportHeight * camera.zoom).toInt()), Point(camera.position.x.toInt() - (camera.viewportWidth * camera.zoom).toInt() / 2, camera.position.y.toInt() - (camera.viewportHeight * camera.zoom).toInt() / 2))

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_UP_LAYER.key)) {
                selectLayer += 1
            }
            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_DOWN_LAYER.key)) {
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
                            val gameObject = findGameObjectUnderMouse(true)
                            if (gameObject != null) {
                                addSelectGameObject(gameObject)
                                editorMode = EditorMode.SELECT
                            } else { // Maybe box
                                selectRectangleData.rectangleStarted = true
                                selectRectangleData.startPosition = mousePosInWorld
                                selectRectangleData.endPosition = selectRectangleData.startPosition
                            }
                        }
                    } else if (latestLeftButtonClick && selectRectangleData.rectangleStarted) { // Bouton gauche de la souris relaché pendant cette frame
                        selectRectangleData.rectangleStarted = false

                        level.getAllGameObjectsInRect(selectRectangleData.getRect(), false).forEach {
                            addSelectGameObject(it)
                            editorMode = EditorMode.SELECT
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        val gameObject = findGameObjectUnderMouse(true)
                        if (gameObject != null) {
                            addSelectGameObject(gameObject)
                            editorMode = EditorMode.COPY
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                        val gameObject = findGameObjectUnderMouse(true)
                        gameObject?.getCurrentState()?.inverseFlipRenderable(true, false)
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                        val gameObject = findGameObjectUnderMouse(true)
                        gameObject?.getCurrentState()?.inverseFlipRenderable(true, false)
                    }
                }
                EditorMode.SELECT -> {
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
                                if (!level.matrixRect.contains(Rect(it.box.x + moveX, it.box.y + moveY, it.box.width, it.box.height))) {
                                    canMove = false
                                }
                            }
                            canMove
                        }) {
                            selectGameObjects.forEach {
                                it.box.move(moveX, moveY)
                            }
                        }
                    }

                    /**
                     * On vérifie si le pointeur est dans le cercle de redimensionnement
                     */
                    fun checkCircleResize(): Boolean {
                        if (selectGameObject != null) {
                            val rect = selectGameObject!!.box
                            return Circle(rect.x + rect.width.toFloat(), rect.y + rect.height.toFloat(), 10f).contains(mousePosInWorld)
                        }
                        return false
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                        if (!latestLeftButtonClick) {
                            selectGameObjectMode = SelectGOMode.NO_MODE

                            if (checkCircleResize()) {
                                selectGameObjectMode = SelectGOMode.RESIZE
                            } else {
                                val gameObject = findGameObjectUnderMouse(true)
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

                            val selectGORect = selectGameObject!!.box

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
                                            if (!level.matrixRect.contains(Rect(it.box.x, it.box.y, it.box.width - resizeX, it.box.height - resizeY))) {
                                                canResize = false
                                            }
                                        }
                                        canResize
                                    }) {
                                        selectGameObjects.forEach {
                                            // On vérifie si le gameObject à redimensionner a le même tag que le gameObject sélectionné et qu'il peut être redimensionné
                                            if (it.tag == selectGameObject!!.tag) {
                                                val newSizeX = it.box.width - resizeX
                                                val newSizeY = it.box.height - resizeY

                                                if (newSizeX in 1..Constants.maxGameObjectSize)
                                                    it.box.width = newSizeX
                                                if (newSizeY in 1..Constants.maxGameObjectSize)
                                                    it.box.height = newSizeY
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
                            editorMode = EditorMode.COPY
                    }
                }
                EditorMode.COPY -> {
                    if (selectGameObjects.isEmpty() && selectGameObject == null /* Permet de vérifier si on est pas entrain d'ajouter un nouveau gameObject */) {
                        editorMode = EditorMode.NO_MODE
                        return
                    }

                    if ((Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key))
                            && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                        val underMouseGO = findGameObjectUnderMouse(true)
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
                        val copySelectGO = SerializationFactory.copy(selectGameObject!!)

                        var posX = copySelectGO.box.x
                        var posY = copySelectGO.box.y

                        val width = copySelectGO.box.width
                        val height = copySelectGO.box.height

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

                        if (gridMode.active && useMousePos) {
                            gridMode.putGameObject(level.activeRect, Point(mousePosInWorld.x, mousePosInWorld.y), copySelectGO)
                        } else
                            copySelectGO.box.position = Point(posX, posY)

                        level.addGameObject(copySelectGO)

                        val copyGameObjects = mutableListOf(copySelectGO)

                        selectGameObjects.filter { it !== selectGameObject }.forEach {
                            val deltaX = it.position().x - selectGameObject!!.position().x
                            val deltaY = it.position().y - selectGameObject!!.position().y

                            level.addGameObject(SerializationFactory.copy(it).apply {
                                this.box.position = Point(copySelectGO.position().x + deltaX, copySelectGO.position().y + deltaY)
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
                EditorMode.SELECT_GO -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        val gameObject = findGameObjectUnderMouse(true)
                        if (gameObject != null) {
                            onSelectGO(gameObject)
                            editorMode = EditorMode.NO_MODE
                        }
                    }
                }
                EditorMode.TRY_LEVEL -> {
                }
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                val gameObject = findGameObjectUnderMouse(false)
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
                    val underMouseGO = findGameObjectUnderMouse(true)
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
                editorSceneUI.showSaveLevelExitWindow = true
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_GRID_MODE.key)) {
            gridMode.active = !gridMode.active
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_TRY_LEVEL.key)) {
            if (editorMode != EditorMode.TRY_LEVEL)
                launchTryLevel()
            else
                finishTryLevel()
        }
    }

    override fun dispose() {
        super.dispose()
        editorFont.dispose()

        if (!level.levelPath.toLocalFile().exists()) {
            level.deleteFiles()
        }
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
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key)) {
                if (camera.zoom > 1f)
                    camera.zoom -= 0.02f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
                if (level.matrixRect.width > camera.zoom * (camera.viewportWidth))
                    camera.zoom += 0.02f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key))
                camera.zoom = 1f

            val minCameraX = camera.zoom * (camera.viewportWidth / 2)
            val maxCameraX = level.matrixRect.width - minCameraX
            val minCameraY = camera.zoom * (camera.viewportHeight / 2)
            val maxCameraY = level.matrixRect.height - minCameraY

            val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
            val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)
            camera.position.set(Math.min(maxCameraX, Math.max(x, minCameraX)),
                    Math.min(maxCameraY, Math.max(y, minCameraY)), 0f)
        }
        camera.update()

        if (background is ParallaxBackground)
            (background as ParallaxBackground).updateCamera(camera)
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
    private fun findGameObjectUnderMouse(replaceEditorLayer: Boolean): GameObject? {
        val mousePosInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).toPoint()

        val gameObjectsUnderMouse = level.getAllGameObjectsInCells(level.getActiveGridCells()).filter { it.box.contains(mousePosInWorld) }

        if (!gameObjectsUnderMouse.isEmpty()) {
            val goodLayerGameObject = gameObjectsUnderMouse.find { it.layer == selectLayer }
            return if (goodLayerGameObject != null)
                goodLayerGameObject
            else {
                val gameObject = gameObjectsUnderMouse.first()
                if (replaceEditorLayer)
                    selectLayer = gameObject.layer
                gameObject
            }
        }

        return null
    }

    private fun removeGameObject(gameObject: GameObject) {
        if (gameObject === selectGameObject) {
            selectGameObject = null
        }
        gameObject.removeFromParent()
    }

    private fun launchTryLevel() {
        editorMode = EditorMode.TRY_LEVEL

        gameObjectContainer = SerializationFactory.copy(level).apply { exit = { finishTryLevel() } }

        backupTryModeCameraPos = camera.position.cpy()
    }

    private fun finishTryLevel() {
        clearSelectGameObjects()
        editorMode = EditorMode.NO_MODE

        gameObjectContainer = level
        camera.position.set(backupTryModeCameraPos)
    }

    private fun addPrefab(prefab: Prefab) {
        prefabs.add(prefab)
        SerializationFactory.serializeToFile(prefab, (Constants.prefabsDirPath.child("${prefab.name}.${Constants.prefabExtension}")))
    }

    private fun saveLevelToFile() {
        try {
            SerializationFactory.serializeToFile(level, level.levelPath.toLocalFile())
        } catch (e: Exception) {
            Log.error(e) { "Erreur lors de l'enregistrement du niveau !" }
        }
    }

    //region UI

    private fun drawUI() {
        with(ImGui) {
            drawMainMenuBar()

            if (gridMode.active)
                drawGridSettingsWindow()

            val goUnderMouse = findGameObjectUnderMouse(false)
            if (goUnderMouse != null) {
                functionalProgramming.withTooltip {
                    text(goUnderMouse.name)
                    text("layer: %s", goUnderMouse.layer)
                    text("x: %s y: %s", goUnderMouse.box.x, goUnderMouse.box.y)
                    text("w: %s h: %s", goUnderMouse.box.width, goUnderMouse.box.height)
                }
            }

            if (selectGameObject != null && selectGameObject?.container != null
                    && editorMode != EditorMode.SELECT_POINT && editorMode != EditorMode.SELECT_GO && editorMode != EditorMode.TRY_LEVEL) {
                drawInfoGameObjectWindow(selectGameObject!!)
            }

            if (editorSceneUI.showSaveLevelExitWindow) {
                val windowSize = Vec2(375f, 60f)
                setNextWindowSize(windowSize, Cond.Once)
                setNextWindowPos(Vec2(Gdx.graphics.width / 2f - windowSize.x / 2f, Gdx.graphics.height / 2f - windowSize.y / 2f), Cond.Once)
                functionalProgramming.withWindow("Sauvegarder avant de quitter?", editorSceneUI::showSaveLevelExitWindow, WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
                    fun showMainMenu() {
                        editorSceneUI.showSaveLevelExitWindow = false
                        SceneManager.loadScene(MainMenuScene())
                    }

                    if (button("Sauvegarder")) {
                        saveLevelToFile()
                        showMainMenu()
                    }
                    sameLine()
                    if (button("Abandonner les modifications")) {
                        if (!level.levelPath.toLocalFile().exists()) {
                            level.deleteFiles()
                        }
                        showMainMenu()
                    }
                    sameLine()
                    if (button("Annuler")) {
                        editorSceneUI.showSaveLevelExitWindow = false
                    }
                }
            }
        }
    }

    private fun drawMainMenuBar() {
        with(ImGui) {
            mainMenuBar {
                if (editorMode != EditorMode.TRY_LEVEL) {
                    menu("Fichier") {
                        menuItem("Importer une ressource..") {
                            try {
                                stackPush().also { stack ->
                                    val aFilterPatterns = stack.mallocPointer(Constants.levelTextureExtension.size + Constants.levelAtlasExtension.size + Constants.levelSoundExtension.size)

                                    val extensions = Constants.levelTextureExtension + Constants.levelAtlasExtension + Constants.levelSoundExtension
                                    extensions.forEach {
                                        aFilterPatterns.put(stack.UTF8("*.$it"))
                                    }

                                    aFilterPatterns.flip()

                                    val extensionsStr = let {
                                        var str = String()
                                        extensions.forEach {
                                            str += "*.$it "
                                        }
                                        str
                                    }

                                    val files = tinyfd_openFileDialog("Importer une ressource..", "", aFilterPatterns, "Ressources ($extensionsStr)", true)
                                    if (files != null) {
                                        level.addResources(*files.split("|").map { it.toAbsoluteFile() }.toTypedArray())
                                    }
                                }
                            } catch (e: Exception) {
                                Log.error(e) { "Erreur lors de l'importation d'une ressource !" }
                            }
                        }
                        menuItem("Essayer le niveau") {
                            launchTryLevel()
                        }
                        menuItem("Sauvegarder") {
                            saveLevelToFile()
                        }
                        separator()
                        menuItem("Quitter") {
                            editorSceneUI.showSaveLevelExitWindow = true
                        }
                    }

                    menu("Éditer") {
                        menu("Options du niveau") {
                            fun updateBackground(newBackground: Background) {
                                editorSceneUI.settingsLevelBackgroundType.obj = newBackground.type
                                level.background = newBackground
                                background = newBackground
                            }

                            if (ImguiHelper.enum("type de fond d'écran", editorSceneUI.settingsLevelBackgroundType)) {
                                when (editorSceneUI.settingsLevelBackgroundType.obj as BackgroundType) {
                                    BackgroundType.Standard -> {
                                        updateBackground(PCGame.standardBackgrounds()[0])
                                    }
                                    BackgroundType.Parallax -> {
                                        updateBackground(PCGame.parallaxBackgrounds()[0])
                                    }
                                }
                            }

                            when (editorSceneUI.settingsLevelBackgroundType.obj as BackgroundType) {
                                BackgroundType.Standard -> {
                                    if (editorSceneUI.settingsLevelStandardBackgroundIndex[0] == -1) {
                                        editorSceneUI.settingsLevelStandardBackgroundIndex[0] = PCGame.standardBackgrounds().indexOfFirst { it == level.background }
                                    }

                                    if (sliderInt("Fond d'écran", editorSceneUI.settingsLevelStandardBackgroundIndex, 0, PCGame.standardBackgrounds().size - 1)) {
                                        updateBackground(PCGame.standardBackgrounds()[editorSceneUI.settingsLevelStandardBackgroundIndex[0]])
                                    }
                                }
                                BackgroundType.Parallax -> {
                                    if (editorSceneUI.settingsLevelParallaxBackgroundIndex[0] == -1) {
                                        editorSceneUI.settingsLevelParallaxBackgroundIndex[0] = PCGame.parallaxBackgrounds().indexOfFirst { it == level.background }
                                    }

                                    if (sliderInt("Fond d'écran", editorSceneUI.settingsLevelParallaxBackgroundIndex, 0, PCGame.parallaxBackgrounds().size - 1)) {
                                        updateBackground(PCGame.parallaxBackgrounds()[editorSceneUI.settingsLevelParallaxBackgroundIndex[0]])
                                    }
                                }
                            }

                            if (button("Sélectionner le gameObject suivi", Vec2(-1, 20f))) {
                                editorMode = EditorMode.SELECT_GO
                                onSelectGO.register(true) {
                                    level.followGameObjectID = it.id
                                }
                            }

                            inputInt("largeur", level::matrixWidth)
                            inputInt("hauteur", level::matrixHeight)
                        }
                    }
                    menu("Créer un gameObject") {
                        GameObject.Tag.values().forEach { tag ->
                            menu(tag.name) {
                                prefabs.filter { it.prefabGO.tag == tag }.forEach {
                                    menuItem(it.name) {
                                        val gameObject = it.create(Point())

                                        clearSelectGameObjects()
                                        selectGameObject = gameObject
                                        editorMode = EditorMode.COPY
                                    }
                                }
                            }
                        }
                    }
                } else {
                    menuItem("Arrêter l'essai") {
                        finishTryLevel()
                    }
                }
            }
        }
    }

    private fun drawGridSettingsWindow() {
        with(ImGui) {
            functionalProgramming.withWindow("Réglages de la grille", null, WindowFlags.AlwaysAutoResize.i) {
                val size = intArrayOf(gridMode.cellWidth, gridMode.cellHeight)
                if (sliderInt2("taille", size, 1, Constants.maxGameObjectSize)) {
                    gridMode.cellWidth = size[0]
                    gridMode.cellHeight = size[1]
                }

                val offset = intArrayOf(gridMode.offsetX, gridMode.offsetY)
                if (dragInt2("décalage", offset, 1f, 0)) {
                    gridMode.offsetX = offset[0]
                    gridMode.offsetY = offset[1]
                }
            }
        }
    }

    private fun drawInfoGameObjectWindow(gameObject: GameObject) {
        val createPrefabTitle = "Créer un prefab"
        val newStateTitle = "Nouveau state"
        val addComponentTitle = "Ajouter un component"

        with(ImGui) {
            functionalProgramming.withWindow("Réglages du gameObject", null, WindowFlags.AlwaysAutoResize.i) {
                if (button("Supprimer ce gameObject", Vec2(-1, 20f))) {
                    gameObject.removeFromParent()
                    if (selectGameObject === gameObject) {
                        selectGameObject = null
                        clearSelectGameObjects()
                        editorMode = EditorMode.NO_MODE
                    }
                }

                if (button("Créer un prefab", Vec2(-1, 20f)))
                    openPopup(createPrefabTitle)

                ImguiHelper.insertImguiExposeEditorField(gameObject, gameObject, level)

                separator()

                functionalProgramming.withItemWidth(100f) {
                    combo("state", editorSceneUI::gameObjectCurrentStateIndex, gameObject.getStates().map { it.name })
                }

                if (button("Ajouter un state")) {
                    openPopup(newStateTitle)
                }
                sameLine()
                if (button("Suppr. ce state")) {
                    if (gameObject.getStates().size > 1)
                        gameObject.removeState(editorSceneUI.gameObjectAddStateComboIndex - 1)
                }

                functionalProgramming.withIndent {
                    if (collapsingHeader("components")) {
                        val components = gameObject.getStates().elementAt(editorSceneUI.gameObjectCurrentStateIndex).getComponents()

                        functionalProgramming.withItemWidth(150f) {
                            combo("component", editorSceneUI::gameObjectCurrentComponentIndex, components.map { it.name })
                        }

                        val component = components.elementAtOrNull(editorSceneUI.gameObjectCurrentComponentIndex)

                        if (component != null) {
                            functionalProgramming.withIndent {
                                ImguiHelper.insertImguiExposeEditorField(component, gameObject, level)
                                if (button("Supprimer ce comp.", Vec2(-1, 20f))) {
                                    gameObject.getStates().elementAtOrNull(editorSceneUI.gameObjectCurrentStateIndex)?.removeComponent(component)
                                }
                            }
                        }
                        separator()
                        pushItemFlag(ItemFlags.Disabled.i, gameObject.getStates().elementAtOrNull(editorSceneUI.gameObjectCurrentStateIndex)?.getComponents()?.size == PCGame.componentsClasses.size)
                        if (button("Ajouter un component", Vec2(-1, 20f)))
                            openPopup(addComponentTitle)
                        popItemFlag()
                    }
                }
                if (beginPopup(createPrefabTitle)) {
                    val name = "test".toCharArray() // TODO inputtext

                    inputText("Nom", name)

                    if (button("Ajouter")) {
                        addPrefab(Prefab(String(name), gameObject))
                        closeCurrentPopup()
                    }

                    endPopup()
                }

                if (beginPopup(newStateTitle)) {
                    val comboItems = mutableListOf("State vide").apply { addAll(gameObject.getStates().map { "Copier de : ${it.name}" }) }

                    functionalProgramming.withItemWidth(150f) {
                        combo("type", editorSceneUI::gameObjectAddStateComboIndex, comboItems)
                    }
                    inputText("Nom", "test".toCharArray())

                    if (button("Ajouter")) {
                        val stateName = "test" // TODO wait for inputText imgui
                        if (editorSceneUI.gameObjectAddStateComboIndex == 0)
                            gameObject.addState(stateName) {}
                        else
                            gameObject.addState(SerializationFactory.copy(gameObject.getStates().elementAt(editorSceneUI.gameObjectAddStateComboIndex - 1)).apply { name = stateName })
                        closeCurrentPopup()
                    }

                    sameLine()

                    if (button("Annuler"))
                        closeCurrentPopup()

                    endPopup()
                }

                if (beginPopup(addComponentTitle)) {
                    val components = PCGame.componentsClasses.filter { comp -> gameObject.getStates().elementAt(editorSceneUI.gameObjectCurrentStateIndex).getComponents().none { comp.isInstance(it) } }
                    functionalProgramming.withItemWidth(150f) {
                        combo("component", editorSceneUI::gameObjectAddComponentComboIndex, components.map { it.simpleName ?: "Nom inconnu" })
                    }
                    if (button("Ajouter", Vec2(-1, 20))) {
                        if (editorSceneUI.gameObjectAddComponentComboIndex in components.indices) {
                            gameObject.getStates().elementAt(editorSceneUI.gameObjectCurrentStateIndex).addComponent(ReflectionUtility.findNoArgConstructor(components[editorSceneUI.gameObjectAddComponentComboIndex])!!.newInstance())
                            closeCurrentPopup()
                        }
                    }

                    endPopup()
                }
            }
        }
    }
//endregion
}