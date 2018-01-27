package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.GameObjectContainer
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.factories.PrefabSetup
import be.catvert.pc.factories.PrefabType
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import glm_.func.common.clamp
import glm_.func.common.min
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
import kotlin.math.roundToInt
import kotlin.reflect.full.findAnnotation


/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(val level: Level) : Scene(level.background) {
    private enum class ResizeMode {
        FREE, PROPORTIONAL
    }

    private enum class SelectGOMode {
        NO_MODE, MOVE, RESIZE
    }

    private data class GridMode(var active: Boolean = false, var offsetX: Int = 0, var offsetY: Int = 0, var cellWidth: Int = 50, var cellHeight: Int = 50) {
        fun putGameObject(walkRect: Rect, point: Point, gameObject: GameObject, level: Level): Boolean {
            getRectCellOf(walkRect, point)?.apply {
                if (walkRect.contains(this, true) && level.getAllGameObjectsInCells(walkRect).none { it.box == this }) {
                    gameObject.box = this
                    return true
                }
            }
            return false
        }

        fun getRectCellOf(walkRect: Rect, point: Point): Rect? {
            var rect: Rect? = null
            walkCells(walkRect) {
                if (it.contains(point))
                    rect = it
            }
            return rect
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

    class EditorSceneUI(background: Background) {
        enum class EditorMode {
            NO_MODE, SELECT, COPY, SELECT_POINT, SELECT_GO, TRY_LEVEL
        }

        var editorMode = EditorMode.NO_MODE
        var gameObjectAddStateComboIndex = 0
        var gameObjectAddComponentComboIndex = 0

        val onSelectPoint = Signal<Point>()
        val onSelectGO = Signal<GameObject>()

        var gameObjectCurrentStateIndex = 0

        var gameObjectCurrentComponentIndex = 0

        val settingsLevelStandardBackgroundIndex = intArrayOf(-1)
        val settingsLevelParallaxBackgroundIndex = intArrayOf(-1)

        val settingsLevelBackgroundType: ImguiHelper.Item<Enum<*>> = ImguiHelper.Item(background.type)

        var showExitWindow = false

        var showInfoGameObjectWindow = false
        var showInfoGameObjectTextWindow = false

        var showGameObjectsWindow = true
        var gameObjectsTypeIndex = 0
        var gameObjectsSpriteShowImportedPack = false
        var gameObjectsSpritePackIndex = 0
        var gameObjectsSpritePackTypeIndex = 0
        var gameObjectsSpritePhysics = true
        var gameObjectsSpriteRealSize = false
        var gameObjectsSpriteCustomSize = Size(50, 50)

        var godModeTryMode = false

        var createStateInputText = "Nouveau state"
        var createPrefabInputText = "Nouveau prefab"

        init {
            when (background.type) {
                BackgroundType.Standard -> settingsLevelStandardBackgroundIndex[0] = PCGame.standardBackgrounds().indexOfFirst { it.backgroundFile == (background as StandardBackground).backgroundFile }
                BackgroundType.Parallax -> settingsLevelParallaxBackgroundIndex[0] = PCGame.parallaxBackgrounds().indexOfFirst { it.parallaxDataFile == (background as ParallaxBackground).parallaxDataFile }
            }
        }
    }

    override var gameObjectContainer: GameObjectContainer = level

    private val shapeRenderer = ShapeRenderer().apply { setAutoShapeType(true) }

    private val editorFont = BitmapFont(Constants.editorFontPath)

    private val cameraMoveSpeed = 10f

    private val gridMode = GridMode()

    private var selectGameObjectMode = SelectGOMode.NO_MODE
    private var resizeGameObjectMode = ResizeMode.FREE

    private val selectGameObjects = mutableSetOf<GameObject>()
    private var selectGameObject: GameObject? = null

    private var selectGameObjectTryMode: GameObject? = null

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

    private var latestCameraPos = camera.position.cpy()

    /**
     * La position du début et de la fin du box
     */
    private val selectRectangleData = SelectRectangleData(Point(), Point())

    private var backupTryModeCameraPos = Vector3()
    private var backupTryModeCameraZoom = 1f

    private val editorSceneUI = EditorSceneUI(level.background)

    init {
        gameObjectContainer.allowUpdatingGO = false
        level.updateCamera(camera, false)
    }

    override fun postBatchRender() {
        super.postBatchRender()

        drawUI()

        gameObjectContainer.cast<Level>()?.drawDebug()

        shapeRenderer.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        shapeRenderer.withColor(Color.GOLDENROD) {
            rect(level.matrixRect)
        }

        if (gridMode.active && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
            shapeRenderer.withColor(Color.DARK_GRAY) {
                gridMode.walkCells(level.activeRect) {
                    rect(it)
                }
            }
        }

        /**
         * Dessine les gameObjects qui n'ont pas de renderableComponent avec un box noir
         */
        gameObjectContainer.cast<Level>()?.apply {
            getAllGameObjectsInCells(getActiveGridCells()).forEach {
                if (it.getCurrentState().getComponent<AtlasComponent>()?.data?.isEmpty() != false) {
                    shapeRenderer.withColor(Color.GRAY) {
                        rect(it.box)
                    }
                }
            }
        }

        when (editorSceneUI.editorMode) {
            EditorSceneUI.EditorMode.NO_MODE -> {
                /**
                 * Dessine le box en cour de création
                 */
                if (selectRectangleData.rectangleStarted) {
                    shapeRenderer.set(ShapeRenderer.ShapeType.Filled)
                    shapeRenderer.withColor(Color.FIREBRICK.apply { a = 0.5f }) {
                        rect(selectRectangleData.getRect())
                    }
                    shapeRenderer.set(ShapeRenderer.ShapeType.Line)
                    shapeRenderer.withColor(Color.RED) {
                        rect(selectRectangleData.getRect())
                    }
                }
            }
            EditorSceneUI.EditorMode.SELECT -> {
                /**
                 * Dessine un box autour des gameObjects sélectionnés
                 */
                selectGameObjects.forEach {
                    shapeRenderer.withColor(if (it === selectGameObject) Color.CORAL else Color.RED) {
                        rect(it.box)
                    }
                }

                if (selectGameObject != null) {
                    val rect = selectGameObject!!.box

                    when (selectGameObjectMode) {
                        SelectGOMode.NO_MODE -> {
                            shapeRenderer.withColor(Color.RED) {
                                circle(rect.x + rect.width.toFloat(), rect.y + rect.height.toFloat(), 10f)
                            }
                        }
                        SelectGOMode.RESIZE -> {
                            shapeRenderer.withColor(Color.BLUE) {
                                circle(rect.x + rect.width.toFloat(), rect.y + rect.height.toFloat(), 15f)
                            }
                        }
                        SelectGOMode.MOVE -> {
                        }
                    }
                }
            }
            EditorSceneUI.EditorMode.COPY -> {
                val mousePosInWorld: Point = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).let { Point(it.x.roundToInt(), it.y.roundToInt()) }

                selectGameObjects.forEach { go ->
                    val rect: Rect? = if (gridMode.active && selectGameObjects.size == 1) {
                        val pos = if (go === selectGameObject) Point(mousePosInWorld.x, mousePosInWorld.y) else Point(mousePosInWorld.x + (go.position().x - (selectGameObject?.position()?.x
                                ?: 0)), mousePosInWorld.y + (go.position().y - (selectGameObject?.position()?.y ?: 0)))
                        gridMode.getRectCellOf(level.activeRect, pos)
                    } else {
                        val pos = if (go === selectGameObject) Point(mousePosInWorld.x - go.size().width / 2, mousePosInWorld.y - go.size().height / 2) else Point(mousePosInWorld.x + (go.position().x - (selectGameObject?.let { it.position().x + it.size().width / 2 }
                                ?: 0)), mousePosInWorld.y + (go.position().y - (selectGameObject?.let { it.position().y + it.size().height / 2 }
                                ?: 0)))
                        Rect(pos, go.box.size)
                    }

                    if (rect != null) {
                        go.getCurrentState().getComponent<AtlasComponent>()?.apply {
                            if (this.currentIndex in data.indices) {
                                val data = this.data[currentIndex]
                                PCGame.mainBatch.use {
                                    it.withColor(Color.WHITE.apply { a = 0.5f }) {
                                        it.draw(data.currentKeyFrame(), rect)
                                    }
                                }
                            }
                        } ?: shapeRenderer.withColor(Color.GRAY) { rect(rect) }
                    }

                    if (go.container != null) {
                        shapeRenderer.withColor(if (go === selectGameObject) Color.GREEN else Color.OLIVE) {
                            rect(go.box)
                        }
                    }
                }
            }
            EditorSceneUI.EditorMode.TRY_LEVEL -> {
            }
            EditorSceneUI.EditorMode.SELECT_POINT -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un point")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 3)
                }
            }
            EditorSceneUI.EditorMode.SELECT_GO -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un game object")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 3)
                }
            }
        }
        shapeRenderer.end()

        PCGame.mainBatch.use {
            it.projectionMatrix = PCGame.defaultProjection
            with(editorFont) {
                if (editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
                    draw(it, "Layer sélectionné : $selectLayer", 10f, Gdx.graphics.height - lineHeight * 2)
                    draw(it, "Resize mode : ${resizeGameObjectMode.name}", 10f, Gdx.graphics.height - lineHeight * 4)
                    draw(it, "Mode de l'éditeur : ${editorSceneUI.editorMode.name}", 10f, Gdx.graphics.height - lineHeight * 5)
                } else {
                    draw(it, "Test du niveau..", 10f, Gdx.graphics.height - lineHeight * 2)
                }
                draw(it, "Nombre d'entités : ${gameObjectContainer.getGameObjectsData().size}", 10f, Gdx.graphics.height - lineHeight * 3)
            }
            it.projectionMatrix = camera.combined
        }
    }

    override fun update() {
        super.update()

        updateCamera()

        // TODO Refactor
        if (!isUIHover && editorSceneUI.editorMode == EditorSceneUI.EditorMode.TRY_LEVEL) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                val gameObject = findGameObjectUnderMouse(false)
                if (gameObject != null) {
                    selectGameObjectTryMode = gameObject
                    editorSceneUI.showInfoGameObjectTextWindow = true
                }
            }
        }

        if (!isUIHover && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
            level.activeRect.set(Size((Constants.viewportRatioWidth * camera.zoom).roundToInt(), (camera.viewportHeight * camera.zoom).roundToInt()), Point(camera.position.x.roundToInt() - ((Constants.viewportRatioWidth * camera.zoom) / 2f).roundToInt(), camera.position.y.roundToInt() - ((Constants.viewportRatioHeight * camera.zoom) / 2f).roundToInt()))

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
            val latestMousePosInWorld = camera.unproject(Vector3(latestMousePos.x.toFloat(), latestMousePos.y.toFloat(), 0f)).toPoint()

            when (editorSceneUI.editorMode) {
                EditorSceneUI.EditorMode.NO_MODE -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (latestLeftButtonClick) { // Rectangle
                            selectRectangleData.endPosition = mousePosInWorld
                        } else { // Select
                            val gameObject = findGameObjectUnderMouse(true)
                            if (gameObject != null) {
                                addSelectGameObject(gameObject)
                                editorSceneUI.editorMode = EditorSceneUI.EditorMode.SELECT
                            } else { // Maybe box
                                selectRectangleData.rectangleStarted = true
                                selectRectangleData.startPosition = mousePosInWorld
                                selectRectangleData.endPosition = selectRectangleData.startPosition
                            }
                        }
                    } else if (latestLeftButtonClick && selectRectangleData.rectangleStarted) { // Bouton gauche de la souris relaché pendant cette frame
                        selectRectangleData.rectangleStarted = false

                        level.getAllGameObjectsInCells(selectRectangleData.getRect()).forEach {
                            if (selectRectangleData.getRect().contains(it.box, true)) {
                                addSelectGameObject(it)
                                editorSceneUI.editorMode = EditorSceneUI.EditorMode.SELECT
                            }
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        val gameObject = findGameObjectUnderMouse(true)
                        if (gameObject != null) {
                            addSelectGameObject(gameObject)
                            editorSceneUI.editorMode = EditorSceneUI.EditorMode.COPY
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                        findGameObjectUnderMouse(true)?.getCurrentState()?.getComponent<AtlasComponent>()?.apply {
                            flipX = !flipX
                        }
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                        findGameObjectUnderMouse(true)?.getCurrentState()?.getComponent<AtlasComponent>()?.apply {
                            flipY = !flipY
                        }
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_ATLAS_PREVIOUS_FRAME.key)) {
                        findGameObjectUnderMouse(true)?.apply {
                            if (getStates().size == 1) {
                                getCurrentState().getComponent<AtlasComponent>()?.apply {
                                    if (data.size == 1)
                                        data.elementAt(0).previousFrameRegion(0)
                                }
                            }
                        }
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_ATLAS_NEXT_FRAME.key)) {
                        findGameObjectUnderMouse(true)?.apply {
                            if (getStates().size == 1) {
                                getCurrentState().getComponent<AtlasComponent>()?.apply {
                                    if (data.size == 1)
                                        data.elementAt(0).nextFrameRegion(0)
                                }
                            }
                        }
                    }
                }
                EditorSceneUI.EditorMode.SELECT -> {
                    if (selectGameObjects.isEmpty()) {
                        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                        return
                    }

                    /**
                     * Permet de déplacer les gameObjects sélectionnés
                     */
                    fun moveGameObjects(moveX: Int, moveY: Int) {
                        val (canMoveX, canMoveY) = let {
                            var canMoveX = true
                            var canMoveY = true

                            selectGameObjects.forEach {
                                if ((it.box.x + moveX) !in 0..level.matrixRect.width - it.box.width)
                                    canMoveX = false
                                if ((it.box.y + moveY) !in 0..level.matrixRect.height - it.box.height)
                                    canMoveY = false
                            }

                            canMoveX to canMoveY
                        }

                        selectGameObjects.forEach {
                            it.box.move(if (canMoveX) moveX else 0, if (canMoveY) moveY else 0)
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
                                        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                                    }
                                    selectGameObjects.contains(gameObject) -> // Dans ce cas-ci, le joueur à sélectionné un autre gameObject dans ceux qui sont sélectionnés
                                        selectGameObject = gameObject
                                    else -> { // Dans ce cas-ci, il y a un groupe de gameObject sélectionné ou aucun et le joueur en sélectionne un nouveau ou un en dehors de la sélection
                                        clearSelectGameObjects()
                                        addSelectGameObject(gameObject)
                                    }
                                }
                            }
                        } else if (selectGameObject != null) { // Le joueur maintient le clique gauche durant plusieurs frames et a bougé la souris {
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
                                                    if (!level.matrixRect.contains(Rect(it.box.x, it.box.y, it.box.width - resizeX, it.box.height - resizeY), true)) {
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
                                    val moveX = (mousePosInWorld.x - latestMousePosInWorld.x) + (camera.position.x - latestCameraPos.x).roundToInt()
                                    val moveY = (mousePosInWorld.y - latestMousePosInWorld.y) + (camera.position.y - latestCameraPos.y).roundToInt()

                                    moveGameObjects(moveX, moveY)
                                }
                            }
                        }
                        // Le bouton gauche n'est pas appuyé pendant cette frame
                    } else {
                        selectGameObjectMode = SelectGOMode.NO_MODE

                        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                            if (findGameObjectUnderMouse(false) == null) {
                                clearSelectGameObjects()
                                editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                            } else {
                                editorSceneUI.showInfoGameObjectWindow = true
                            }
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
                            editorSceneUI.editorMode = EditorSceneUI.EditorMode.COPY
                    }
                }
                EditorSceneUI.EditorMode.COPY -> {
                    if (selectGameObjects.isEmpty() && selectGameObject == null /* Permet de vérifier si on est pas entrain d'ajouter un nouveau gameObject */) {
                        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
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
                            editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                        }
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && (!latestRightButtonClick || gridMode.active)) {
                        val copySelectGO = SerializationFactory.copy(selectGameObject!!)

                        var posX = copySelectGO.position().x
                        var posY = copySelectGO.position().y

                        val width = copySelectGO.size().width
                        val height = copySelectGO.size().height

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
                            posX = mousePosInWorld.x - width / 2
                            posY = mousePosInWorld.y - height / 2

                            // Permet de vérifier si le gameObject copié est nouveau ou pas (si il est nouveau, ça veut dire qu'il n'a pas encore de container)
                            if (selectGameObject!!.container != null)
                                moveToCopyGO = false
                        }

                        posX = posX.clamp(0, level.matrixRect.width - width)
                        posY = posY.clamp(0, level.matrixRect.height - height)

                        var putSuccessful = true
                        if (gridMode.active && useMousePos && selectGameObjects.size == 1) {
                            putSuccessful = gridMode.putGameObject(level.activeRect, Point(mousePosInWorld.x, mousePosInWorld.y), copySelectGO, level)
                        } else
                            copySelectGO.box.position = Point(posX, posY)

                        val copyGameObjects = mutableListOf<GameObject>()

                        if (putSuccessful) {
                            level.addGameObject(copySelectGO)
                            copyGameObjects.add(copySelectGO)
                        } else
                            moveToCopyGO = false

                        selectGameObjects.filter { it !== selectGameObject }.forEach {
                            val deltaX = it.position().x - selectGameObject!!.position().x
                            val deltaY = it.position().y - selectGameObject!!.position().y

                            level.addGameObject(SerializationFactory.copy(it).apply {
                                val pos = Point((copySelectGO.position().x + deltaX).clamp(0, level.matrixRect.width - this.size().width), (copySelectGO.position().y + deltaY).clamp(0, level.matrixRect.height - this.size().height))

                                this.box.position = pos

                                copyGameObjects += this
                            })
                        }

                        if (moveToCopyGO) {
                            clearSelectGameObjects()
                            copyGameObjects.forEach { addSelectGameObject(it) }
                        }
                    }
                }
                EditorSceneUI.EditorMode.SELECT_POINT -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                        editorSceneUI.onSelectPoint(mousePosInWorld)
                        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                    }
                }
                EditorSceneUI.EditorMode.SELECT_GO -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                        val go = findGameObjectUnderMouse(false)
                        if (go != null) {
                            editorSceneUI.onSelectGO(go)
                            editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                        }
                    }
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
                    editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
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
            latestCameraPos = camera.position.cpy()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (editorSceneUI.editorMode == EditorSceneUI.EditorMode.TRY_LEVEL)
                finishTryLevel()
            else
                editorSceneUI.showExitWindow = true
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_GRID_MODE.key) && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
            gridMode.active = !gridMode.active
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_TRY_LEVEL.key)) {
            if (editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL)
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

        if (editorSceneUI.editorMode == EditorSceneUI.EditorMode.TRY_LEVEL) {
            gameObjectContainer.cast<Level>()?.updateCamera(camera, true)
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
                if (camera.zoom > 0.5f)
                    camera.zoom -= 0.01f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
                if (level.matrixRect.width > camera.zoom * (camera.viewportWidth))
                    camera.zoom += 0.01f
            }
            if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key))
                camera.zoom = 1f

            val minCameraX = camera.zoom * (camera.viewportWidth / 2)
            val maxCameraX = level.matrixRect.width - minCameraX
            val minCameraY = camera.zoom * (camera.viewportHeight / 2)
            val maxCameraY = level.matrixRect.height - minCameraY

            val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
            val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)

            camera.position.set(MathUtils.clamp(x, minCameraX, maxCameraX), MathUtils.clamp(y, minCameraY, maxCameraY), 0f)
        }
        camera.update()
    }

    private fun setCopyGameObject(gameObject: GameObject) {
        clearSelectGameObjects()
        addSelectGameObject(gameObject)
        editorSceneUI.editorMode = EditorSceneUI.EditorMode.COPY
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
        val mousePosInWorld = viewport.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).toPoint()

        val gameObjectsUnderMouse = (gameObjectContainer as Level).run { getAllGameObjectsInCells(getActiveGridCells()).filter { it.box.contains(mousePosInWorld) } }

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
        backupTryModeCameraPos = camera.position.cpy()
        backupTryModeCameraZoom = camera.zoom

        editorSceneUI.editorMode = EditorSceneUI.EditorMode.TRY_LEVEL

        gameObjectContainer = SerializationFactory.copy(level).apply { exit = { if (!editorSceneUI.godModeTryMode) finishTryLevel() }; }
    }

    private fun finishTryLevel() {
        clearSelectGameObjects()
        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE

        gameObjectContainer = level
        camera.position.set(backupTryModeCameraPos)
        camera.zoom = backupTryModeCameraZoom

        selectGameObjectTryMode = null
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

            if (editorSceneUI.showGameObjectsWindow && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL)
                drawGameObjectsWindow()

            if (gridMode.active && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL)
                drawGridSettingsWindow()

            val goUnderMouse = findGameObjectUnderMouse(false)
            if (goUnderMouse != null && !isUIHover) {
                functionalProgramming.withTooltip {
                    ImguiHelper.textColored(Color.RED, goUnderMouse.name)
                    ImguiHelper.textPropertyColored(Color.ORANGE, "layer :", goUnderMouse.layer)

                    ImguiHelper.textPropertyColored(Color.ORANGE, "x :", goUnderMouse.box.x)
                    sameLine()
                    ImguiHelper.textPropertyColored(Color.ORANGE, "y :", goUnderMouse.box.y)

                    ImguiHelper.textPropertyColored(Color.ORANGE, "w :", goUnderMouse.box.width)
                    sameLine()
                    ImguiHelper.textPropertyColored(Color.ORANGE, "h :", goUnderMouse.box.height)
                }
            }

            if (editorSceneUI.showInfoGameObjectTextWindow && selectGameObjectTryMode != null && editorSceneUI.editorMode == EditorSceneUI.EditorMode.TRY_LEVEL) {
                drawInfoGameObjectTextWindow(selectGameObjectTryMode!!)
            }

            if (editorSceneUI.showInfoGameObjectWindow && selectGameObject != null && selectGameObject?.container != null
                    && editorSceneUI.editorMode != EditorSceneUI.EditorMode.SELECT_POINT && editorSceneUI.editorMode != EditorSceneUI.EditorMode.SELECT_GO && editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
                drawInfoGameObjectWindow(selectGameObject!!)
            }

            if (editorSceneUI.showExitWindow) {
                drawExitWindow()
            }
        }
    }

    private fun drawExitWindow() {
        ImguiHelper.withCenteredWindow("Sauvegarder le niveau ?", editorSceneUI::showExitWindow, Vec2(240f, 100f), WindowFlags.NoResize.i or WindowFlags.NoCollapse.i) {
            fun showMainMenu() {
                PCGame.sceneManager.loadScene(MainMenuScene())
            }

            if (ImGui.button("Sauvegarder", Vec2(225f, 0))) {
                saveLevelToFile()
                showMainMenu()
            }
            if (ImGui.button("Abandonner les modifications", Vec2(225f, 0))) {
                if (!level.levelPath.toLocalFile().exists()) {
                    level.deleteFiles()
                }
                showMainMenu()
            }
            if (ImGui.button("Annuler", Vec2(225f, 0))) {
                editorSceneUI.showExitWindow = false
            }
        }
    }

    private fun drawMainMenuBar() {
        with(ImGui) {
            mainMenuBar {
                if (editorSceneUI.editorMode != EditorSceneUI.EditorMode.TRY_LEVEL) {
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
                            ImGui.cursorPosY = 100f
                            launchTryLevel()
                        }
                        menuItem("Sauvegarder") {
                            saveLevelToFile()
                        }
                        separator()
                        menuItem("Quitter") {
                            editorSceneUI.showExitWindow = true
                        }
                    }

                    menu("Éditer") {
                        menu("Tags") {
                            ImguiHelper.addImguiWidgetsArray("tags", level.tags, { it }, { "tag" }, {
                                ImguiHelper.inputText("tag", it)
                            }, editorSceneUI)
                        }
                        menu("Options du niveau") {
                            fun updateBackground(newBackground: Background) {
                                editorSceneUI.settingsLevelBackgroundType.obj = newBackground.type
                                level.background = newBackground
                                background = newBackground
                            }

                            ImguiHelper.enum("type de fond d'écran", editorSceneUI.settingsLevelBackgroundType)

                            when (editorSceneUI.settingsLevelBackgroundType.obj.cast<BackgroundType>()) {
                                BackgroundType.Standard -> {
                                    if (editorSceneUI.settingsLevelStandardBackgroundIndex[0] == -1) {
                                        editorSceneUI.settingsLevelStandardBackgroundIndex[0] = PCGame.standardBackgrounds().indexOfFirst { it == level.background }
                                    }

                                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                        if (sliderInt("Fond d'écran", editorSceneUI.settingsLevelStandardBackgroundIndex, 0, PCGame.standardBackgrounds().size - 1)) {
                                            updateBackground(PCGame.standardBackgrounds()[editorSceneUI.settingsLevelStandardBackgroundIndex[0]])
                                        }
                                    }
                                }
                                BackgroundType.Parallax -> {
                                    if (editorSceneUI.settingsLevelParallaxBackgroundIndex[0] == -1) {
                                        editorSceneUI.settingsLevelParallaxBackgroundIndex[0] = PCGame.parallaxBackgrounds().indexOfFirst { it == level.background }
                                    }

                                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                        if (sliderInt("Fond d'écran", editorSceneUI.settingsLevelParallaxBackgroundIndex, 0, PCGame.parallaxBackgrounds().size - 1)) {
                                            updateBackground(PCGame.parallaxBackgrounds()[editorSceneUI.settingsLevelParallaxBackgroundIndex[0]])
                                        }
                                    }
                                }
                            }

                            ImguiHelper.gameObject(level::followGameObject, editorSceneUI, "caméra go")

                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                inputInt("largeur", level::matrixWidth)
                                inputInt("hauteur", level::matrixHeight)
                                sliderFloat("zoom initial", level::initialZoom, 0.1f, 2f, "%.1f")
                            }
                        }
                    }
                    menu("Créer un gameObject") {
                        fun createGO(prefab: Prefab) {
                            val gameObject = prefab.create(Point()).apply { loadResources() }
                            setCopyGameObject(gameObject)
                        }

                        PrefabType.values().forEach { type ->
                            menu(type.name) {
                                if (type == PrefabType.All) {
                                    level.resourcesPrefabs().forEach {
                                        menuItem(it.name) {
                                            createGO(it)
                                        }
                                    }
                                }
                                PrefabFactory.values().filter { it.type == type }.forEach {
                                    menuItem(it.name.removeSuffix("_${type.name}")) {
                                        createGO(it.prefab)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    menuItem("Arrêter l'essai") {
                        finishTryLevel()
                    }
                    checkbox("God mode", editorSceneUI::godModeTryMode)
                }
            }
        }
    }

    private fun drawGridSettingsWindow() {
        with(ImGui) {
            functionalProgramming.withWindow("Réglages de la grille", null, WindowFlags.AlwaysAutoResize.i) {
                val size = intArrayOf(gridMode.cellWidth, gridMode.cellHeight)

                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    if (inputInt2("taille", size)) {
                        gridMode.cellWidth = size[0].clamp(1, Constants.maxGameObjectSize)
                        gridMode.cellHeight = size[1].clamp(1, Constants.maxGameObjectSize)
                        gridMode.offsetX = gridMode.offsetX.min(gridMode.cellWidth)
                        gridMode.offsetY = gridMode.offsetY.min(gridMode.cellHeight)
                    }
                }

                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    dragInt("décalage x", gridMode::offsetX, 1f, 0, gridMode.cellWidth)
                    dragInt("décalage y", gridMode::offsetY, 1f, 0, gridMode.cellHeight)
                }
            }
        }
    }

    private fun drawGameObjectsWindow() {
        with(ImGui) {
            val nextWindowSize = Vec2(220f, Gdx.graphics.height)
            setNextWindowSize(nextWindowSize, Cond.Once)
            setNextWindowPos(Vec2(Gdx.graphics.width - nextWindowSize.x, 0), Cond.Once)
            setNextWindowSizeConstraints(Vec2(nextWindowSize.x, 250f), Vec2(nextWindowSize.x, Gdx.graphics.height))
            functionalProgramming.withWindow("Game objects", editorSceneUI::showGameObjectsWindow) {
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("type", editorSceneUI::gameObjectsTypeIndex, level.tags.apply { remove(Tags.Empty.tag) })
                }
                val tag = level.tags.elementAtOrElse(editorSceneUI.gameObjectsTypeIndex, { Tags.Sprite.tag })

                fun addImageBtn(region: TextureAtlas.AtlasRegion, prefab: Prefab, showTooltip: Boolean) {
                    if (imageButton(region.texture.textureObjectHandle, Vec2(50f, 50f), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                        setCopyGameObject(prefab.create(Point()).apply { loadResources() })
                    }

                    if (showTooltip && isMouseHoveringRect(itemRectMin, itemRectMax)) {
                        functionalProgramming.withTooltip {
                            text(prefab.name)
                        }
                    }
                }

                when (tag) {
                    Tags.Sprite.tag -> {
                        functionalProgramming.withGroup {
                            checkbox("pack importés", editorSceneUI::gameObjectsSpriteShowImportedPack)
                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                if (!editorSceneUI.gameObjectsSpriteShowImportedPack) {
                                    combo("dossier", editorSceneUI::gameObjectsSpritePackTypeIndex, PCGame.gameAtlas.map { it.key.name() })
                                }
                                combo("pack", editorSceneUI::gameObjectsSpritePackIndex,
                                        if (editorSceneUI.gameObjectsSpriteShowImportedPack) level.resourcesAtlas().map { it.nameWithoutExtension() }
                                        else PCGame.gameAtlas.entries.elementAtOrNull(editorSceneUI.gameObjectsSpritePackTypeIndex)?.value?.map { it.nameWithoutExtension() }
                                                ?: arrayListOf())
                                checkbox("Physics", editorSceneUI::gameObjectsSpritePhysics)
                                checkbox("Taille réelle", editorSceneUI::gameObjectsSpriteRealSize)
                                if (!editorSceneUI.gameObjectsSpriteRealSize)
                                    ImguiHelper.size(editorSceneUI::gameObjectsSpriteCustomSize, Size(1), Size(Constants.maxGameObjectSize))
                            }
                        }
                        separator()
                        (if (editorSceneUI.gameObjectsSpriteShowImportedPack) level.resourcesAtlas().getOrNull(editorSceneUI.gameObjectsSpritePackIndex) else PCGame.gameAtlas.entries.elementAtOrNull(editorSceneUI.gameObjectsSpritePackTypeIndex)?.value?.getOrNull(editorSceneUI.gameObjectsSpritePackIndex))?.also { atlasPath ->
                            val atlas = ResourceManager.getPack(atlasPath)
                            atlas.regions.sortedBy { it.name }.forEachIndexed { index, region ->
                                val atlasRegion = atlasPath.toFileWrapper() to region.name
                                val size = if (editorSceneUI.gameObjectsSpriteRealSize) region.let { Size(it.regionWidth, it.regionHeight) } else Size(50, 50)
                                val prefab = if (editorSceneUI.gameObjectsSpritePhysics) PrefabSetup.setupPhysicsSprite(atlasRegion, size) else PrefabSetup.setupSprite(atlasRegion, size)
                                addImageBtn(region, prefab, false)

                                if ((index + 1) % 3 != 0)
                                    sameLine()
                            }
                        }
                    }
                    else -> {
                        var index = 0
                        (PrefabFactory.values().map { it.prefab } + level.resourcesPrefabs()).filter { it.prefabGO.tag == tag }.forEach {
                            it.prefabGO.loadResources()

                            it.prefabGO.getCurrentState().getComponent<AtlasComponent>()?.apply {
                                data.elementAtOrNull(currentIndex)?.apply {
                                    addImageBtn(currentKeyFrame(), it, true)

                                    if ((++index) % 3 != 0)
                                        sameLine()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawInfoGameObjectWindow(gameObject: GameObject) {
        val createPrefabTitle = "Créer un prefab"
        val newStateTitle = "Nouveau state"
        val addComponentTitle = "Ajouter un component"

        with(ImGui) {
            functionalProgramming.withWindow("Réglages du gameObject", editorSceneUI::showInfoGameObjectWindow, WindowFlags.AlwaysAutoResize.i) {
                if (button("Supprimer ce gameObject", Vec2(-1, 0))) {
                    gameObject.removeFromParent()
                    if (selectGameObject === gameObject) {
                        selectGameObject = null
                        clearSelectGameObjects()
                        editorSceneUI.editorMode = EditorSceneUI.EditorMode.NO_MODE
                    }
                }

                if (button("Créer un prefab", Vec2(-1, 0)))
                    openPopup(createPrefabTitle)

                ImguiHelper.insertImguiExposeEditorFields(gameObject, gameObject, level, editorSceneUI)

                separator()

                ImguiHelper.comboWithSettingsButton("state", editorSceneUI::gameObjectCurrentStateIndex, gameObject.getStates().map { it.name }, {
                    if (button("Ajouter un state")) {
                        openPopup(newStateTitle)
                    }

                    if (gameObject.getStates().size > 1) {
                        sameLine()
                        if (button("Supprimer ${gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).name}")) {
                            gameObject.removeState(editorSceneUI.gameObjectCurrentStateIndex)
                            editorSceneUI.gameObjectCurrentStateIndex = Math.max(0, editorSceneUI.gameObjectCurrentStateIndex - 1)
                        }
                    }

                    functionalProgramming.popup(newStateTitle) {
                        val comboItems = mutableListOf("State vide").apply { addAll(gameObject.getStates().map { "Copier de : ${it.name}" }) }

                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            combo("type", editorSceneUI::gameObjectAddStateComboIndex, comboItems)
                        }

                        ImguiHelper.inputText("nom", editorSceneUI::createStateInputText)

                        if (button("Ajouter", Vec2(Constants.defaultWidgetsWidth, 0))) {
                            if (editorSceneUI.gameObjectAddStateComboIndex == 0)
                                gameObject.addState(editorSceneUI.createStateInputText) {}
                            else
                                gameObject.addState(SerializationFactory.copy(gameObject.getStateOrDefault(editorSceneUI.gameObjectAddStateComboIndex - 1)).apply { name = editorSceneUI.createStateInputText })
                            closeCurrentPopup()
                        }
                    }
                })

                ImguiHelper.action("start action", gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex)::startAction, gameObject, level, editorSceneUI)

                separator()

                val components = gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).getComponents()

                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("component", editorSceneUI::gameObjectCurrentComponentIndex, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })
                }

                val component = components.elementAtOrNull(editorSceneUI.gameObjectCurrentComponentIndex)
                if (component != null) {
                    functionalProgramming.withIndent {
                        val requiredComponent = component.javaClass.kotlin.findAnnotation<RequiredComponent>()
                        val incorrectComponent = let {
                            requiredComponent?.component?.forEach {
                                if (!gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).hasComponent(it))
                                    return@let true
                            }
                            false
                        }
                        if (!incorrectComponent)
                            ImguiHelper.insertImguiExposeEditorFields(component, gameObject, level, editorSceneUI)
                        else {
                            text("Il manque le(s) component(s) :")
                            functionalProgramming.withIndent {
                                text("${requiredComponent!!.component.map { it.simpleName }}")
                            }
                        }
                    }
                    if (button("Supprimer ce comp.", Vec2(-1, 0))) {
                        gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).removeComponent(component)
                    }
                }
                separator()
                pushItemFlag(ItemFlags.Disabled.i, gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).getComponents().size == PCGame.componentsClasses.size)
                if (button("Ajouter un component", Vec2(-1, 0)))
                    openPopup(addComponentTitle)
                popItemFlag()

                functionalProgramming.popup(createPrefabTitle) {
                    ImguiHelper.inputText("nom", editorSceneUI::createPrefabInputText)

                    if (button("Ajouter", Vec2(-1, 0))) {
                        level.addPrefab(Prefab(editorSceneUI.createPrefabInputText, gameObject))
                        closeCurrentPopup()
                    }
                }

                functionalProgramming.popup(addComponentTitle) {
                    val components = PCGame.componentsClasses.filter { comp -> gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).getComponents().none { comp.isInstance(it) } }
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        combo("component", editorSceneUI::gameObjectAddComponentComboIndex, components.map {
                            it.simpleName ?: "Nom inconnu"
                        })
                    }
                    if (button("Ajouter", Vec2(-1, 0))) {
                        if (editorSceneUI.gameObjectAddComponentComboIndex in components.indices) {
                            val newComp = ReflectionUtility.findNoArgConstructor(components[editorSceneUI.gameObjectAddComponentComboIndex])!!.newInstance()
                            val state = gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex)

                            if (gameObject.getCurrentState() === state)
                                newComp.onStateActive(gameObject, state, level)

                            state.addComponent(newComp)
                            closeCurrentPopup()
                        }
                    }
                }
            }
        }
    }

    private fun drawInfoGameObjectTextWindow(gameObject: GameObject) {
        with(ImGui) {
            functionalProgramming.withWindow("Données du gameObject", editorSceneUI::showInfoGameObjectTextWindow, WindowFlags.AlwaysAutoResize.i) {
                ImguiHelper.insertImguiTextExposeEditorFields(gameObject)
                separator()

                val components = gameObject.getCurrentState().getComponents()
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("component", editorSceneUI::gameObjectCurrentComponentIndex, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })
                }

                val component = components.elementAtOrNull(editorSceneUI.gameObjectCurrentComponentIndex)
                if (component != null) {
                    functionalProgramming.withIndent {
                        val requiredComponent = component.javaClass.kotlin.findAnnotation<RequiredComponent>()
                        val incorrectComponent = let {
                            requiredComponent?.component?.forEach {
                                if (!gameObject.getStateOrDefault(editorSceneUI.gameObjectCurrentStateIndex).hasComponent(it))
                                    return@let true
                            }
                            false
                        }
                        if (!incorrectComponent)
                            ImguiHelper.insertImguiTextExposeEditorFields(component)
                        else {
                            ImguiHelper.textColored(Color.RED, "Il manque le(s) component(s) :")
                            functionalProgramming.withIndent {
                                text("${requiredComponent!!.component.map { it.simpleName }}")
                            }
                        }
                    }
                }
            }
        }
    }
//endregion
}