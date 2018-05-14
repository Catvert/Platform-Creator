package be.catvert.pc.scenes

import be.catvert.pc.GameKeys
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.components.Components
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.PackRegionData
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.factories.GroupFactory
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.factories.PrefabSetup
import be.catvert.pc.factories.PrefabType
import be.catvert.pc.managers.MusicsManager
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import glm_.func.common.clamp
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.functionalProgramming.mainMenuBar
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuItem
import ktx.app.use
import kotlin.math.roundToInt
import kotlin.reflect.full.findAnnotation

/**
 * Scène de l'éditeur de niveau.
 */
class EditorScene(val level: Level, applyMusicTransition: Boolean) : Scene(level.background, level.backgroundColor) {
    private enum class ResizeMode(val resizeName: String) {
        FREE("Libre"), PROPORTIONAL("Proportionnel")
    }

    private enum class SelectEntityMode {
        NO_MODE, MOVE, HORIZONTAL_RESIZE_LEFT, HORIZONTAL_RESIZE_RIGHT, VERTICAL_RESIZE_BOTTOM, VERTICAL_RESIZE_TOP, DIAGONALE_RESIZE
    }

    private data class GridMode(var active: Boolean = false, var offsetX: Int = 0, var offsetY: Int = 0, var cellWidth: Int = 50, var cellHeight: Int = 50) {
        fun putEntity(walkRect: Rect, point: Point, entity: Entity, level: Level): Boolean {
            getRectCellOf(walkRect, point)?.apply {
                if (walkRect.contains(this, true) && level.getAllEntitiesInCells(walkRect).none { it.box == this }) {
                    entity.box = this
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
            for (x in (walkRect.x.roundToInt() + offsetX)..(walkRect.x.roundToInt() + walkRect.width + offsetX) step cellWidth) {
                for (y in (walkRect.y.roundToInt() + offsetY)..(walkRect.y.roundToInt() + walkRect.height + offsetY) step cellHeight) {
                    walk(Rect(x.toFloat(), y.toFloat(), cellWidth, cellHeight))
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

            return Rect(minX, minY, (maxX - minX).roundToInt(), (maxY - minY).roundToInt())
        }
    }

    class EditorUI(background: Background?) {
        enum class EditorMode(val modeName: String) {
            NO_MODE("Aucun"), SELECT("Sélection"), COPY("Copie"), SELECT_POINT("Sélection d'un point"), SELECT_ENTITY("Sélection d'une entité"), TRY_LEVEL("Essai du niveau")
        }

        var editorMode = EditorMode.NO_MODE
            set(value) {
                previousEditorMode = field
                field = value
            }

        var previousEditorMode = EditorMode.NO_MODE
            private set

        var entityAddStateComboIndex = 0
        var entityAddComponentComboIndex = 0

        val onSelectPoint = Signal<Point>()
        val onSelectEntity = Signal<Entity?>()

        var entityCurrentStateIndex = 0

        var entityCurrentComponentIndex = 0

        val settingsLevelStandardBackgroundIndex = intArrayOf(-1)
        val settingsLevelParallaxBackgroundIndex = intArrayOf(-1)

        val settingsLevelBackgroundType: ImGuiHelper.Item<Enum<*>> = ImGuiHelper.Item(background?.type
                ?: BackgroundType.None)

        var showExitWindow = false

        var showInfoEntityWindow = false
        var showInfoEntityTextWindow = false
        var showInfoPrefabWindow = false

        var prefabInfoPrefabWindow: Prefab? = null

        var addTagPopupNameBuffer = ""

        enum class EntityFactoryMode {
            Sprite, Tag, Group;

            override fun toString() = when (this) {
                EditorScene.EditorUI.EntityFactoryMode.Sprite -> "Création"
                EditorScene.EditorUI.EntityFactoryMode.Tag -> "Inventaire"
                EditorScene.EditorUI.EntityFactoryMode.Group -> "Groupes"
            }
        }

        var showEntityFactoryWindow = true
        var entityFactoryTagIndex = 0
        val entityFactoryMode = ImGuiHelper.Item(EntityFactoryMode.Sprite)
        var entityFactoryTagShowOnlyUser = false
        var entityFactorySpritePackIndex = 0
        var entityFactorySpritePackTypeIndex = 0
        var entityFactorySpritePhysics = true
        var entityFactorySpriteRealSize = false
        var entityFactorySpriteCustomSize = Size(50, 50)

        var godModeTryMode = false

        var createStateInputText = "Nouveau état"
        var createPrefabInputText = "Nouveau prefab"

        val menuBarHeight = (g.fontBaseSize + g.style.framePadding.y * 2f).roundToInt()

        init {
            when (background?.type) {
                BackgroundType.Standard -> settingsLevelStandardBackgroundIndex[0] = PCGame.getStandardBackgrounds().indexOfFirst { it.backgroundFile == (background as StandardBackground).backgroundFile }
                BackgroundType.Parallax -> settingsLevelParallaxBackgroundIndex[0] = PCGame.getParallaxBackgrounds().indexOfFirst { it.parallaxDataFile == (background as ParallaxBackground).parallaxDataFile }
                BackgroundType.None -> {
                }
            }
        }
    }

    override var entityContainer: EntityContainer = level

    private val shapeRenderer = ShapeRenderer().apply { setAutoShapeType(true) }

    private val editorFont = BitmapFont(Constants.editorFontPath)

    private val cameraMoveSpeed = 10f

    private val gridMode = GridMode()

    private var selectEntityMode = SelectEntityMode.NO_MODE
    private var resizeEntityMode = ResizeMode.PROPORTIONAL

    private val selectEntities = mutableSetOf<Entity>()
    private var selectEntity: Entity? = null

    private var selectEntityTryMode: Entity? = null

    private var selectLayer = Constants.defaultLayer
        set(value) {
            if(value in 0..Constants.maxLayer)
                field = value
        }

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

    private val editorUI = EditorUI(level.background)

    private var targetCameraZoom = 1f

    private var smartPositioning = true

    init {
        entityContainer.allowUpdating = false
        level.updateCamera(camera, false)

        // Permet de décaler le viewport vers le bas pour afficher la totalité du niveau avec la barre de menu.
        viewport.screenHeight -= editorUI.menuBarHeight
        viewport.apply()

        if (applyMusicTransition) {
            MusicsManager.startMusic(PCGame.menuMusic, true)
        }

        PCInputProcessor.scrolledSignal.register {
            if (isUIHover || editorUI.editorMode == EditorUI.EditorMode.TRY_LEVEL)
                return@register
            targetCameraZoom = (targetCameraZoom + (it * targetCameraZoom * 0.05f)).clamp(0.5f, level.matrixRect.width / camera.viewportWidth).clamp(0.5f, level.matrixRect.height / camera.viewportHeight)
        }
    }

    override fun postBatchRender() {
        super.postBatchRender()

        drawUI()

        entityContainer.cast<Level>()?.drawDebug()

        shapeRenderer.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        shapeRenderer.withColor(Color.GOLDENROD) {
            rect(level.matrixRect)
        }

        if (gridMode.active && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
            shapeRenderer.withColor(Color.DARK_GRAY) {
                gridMode.walkCells(level.activeRect) {
                    rect(it)
                }
            }
        }

        /**
         * Dessine les entités qui n'ont pas d'TextureComponent avec un rectangle noir
         */
        entityContainer.cast<Level>()?.apply {
            getAllEntitiesInCells(getActiveGridCells()).forEach {
                if (it.getCurrentState().getComponent<TextureComponent>()?.groups?.isEmpty() != false) {
                    shapeRenderer.withColor(Color.GRAY) {
                        rect(it.box)
                    }
                }
            }
        }

        when (editorUI.editorMode) {
            EditorUI.EditorMode.NO_MODE -> {
                /**
                 * Dessine le rectangle en cour de création
                 */
                if (selectRectangleData.rectangleStarted) {
                    val rect = selectRectangleData.getRect()
                    shapeRenderer.set(ShapeRenderer.ShapeType.Filled)

                    shapeRenderer.withColor(Color(34 / 255f, 42 / 255f, 53 / 255f, 0.3f)) {
                        rect(rect)
                    }
                    shapeRenderer.set(ShapeRenderer.ShapeType.Line)
                    shapeRenderer.withColor(Color(40 / 255f, 44 / 255f, 52 / 255f, 1f)) {
                        rect(rect)
                    }
                }
            }
            EditorUI.EditorMode.SELECT -> {
                /**
                 * Dessine un rectangle autour des entités sélectionnées
                 */
                selectEntities.forEach {
                    shapeRenderer.withColor(if (it === selectEntity) Color(40 / 255f, 44 / 255f, 52 / 255f, 1f) else Color(34 / 255f, 42 / 255f, 53 / 255f, 0.3f)) {
                        rect(it.box)
                    }
                }

                if (selectEntity != null && selectEntityMode == SelectEntityMode.NO_MODE || selectEntityMode == SelectEntityMode.DIAGONALE_RESIZE) {
                    val rect = selectEntity!!.box

                    shapeRenderer.withColor(Color.RED) {
                        circle(rect.right(), rect.top(), if (selectEntityMode == SelectEntityMode.DIAGONALE_RESIZE) 12f else 10f)
                    }
                }
            }
            EditorUI.EditorMode.COPY -> {
                val mousePosInWorld: Point = viewport.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).let { Point(it.x, it.y) }

                selectEntities.forEach { entity ->
                    val rect: Rect? = if (gridMode.active && selectEntities.size == 1) {
                        val pos = if (entity === selectEntity) Point(mousePosInWorld.x, mousePosInWorld.y) else Point(mousePosInWorld.x + (entity.position().x - (selectEntity?.position()?.x
                                ?: 0f)), mousePosInWorld.y + (entity.position().y - (selectEntity?.position()?.y
                                ?: 0f)))
                        gridMode.getRectCellOf(level.activeRect, pos)
                    } else {
                        val pos = let {
                            val freePosSelect = Point(mousePosInWorld.x - (selectEntity?.size()?.width?.div(2f)
                                    ?: 0f), mousePosInWorld.y - (selectEntity?.size()?.height?.div(2f) ?: 0f))

                            val selectPoint = if (smartPositioning) {
                                val side = getSmartPositioningSide(mousePosInWorld)
                                if (side == null)
                                    freePosSelect
                                else when (side.first) {
                                    BoxSide.Left -> Point(side.second.box.left() - entity.box.width, side.second.box.bottom())
                                    BoxSide.Right -> Point(side.second.box.right(), side.second.box.bottom())
                                    BoxSide.Up -> Point(side.second.box.left(), side.second.box.top())
                                    BoxSide.Down -> Point(side.second.box.left(), side.second.box.bottom() - entity.box.height)
                                    BoxSide.All -> freePosSelect
                                }
                            } else
                                freePosSelect

                            if (entity === selectEntity) {
                                selectPoint
                            } else Point(selectPoint.x + (entity.position().x - (selectEntity?.position()?.x
                                    ?: 0f)), selectPoint.y + (entity.position().y - (selectEntity?.position()?.y
                                    ?: 0f)))
                        }

                        Rect(pos, entity.box.size)
                    }

                    if (rect != null) {
                        entity.getCurrentState().getComponent<TextureComponent>()?.apply {
                            if (this.currentIndex in groups.indices) {
                                val data = this.groups[currentIndex]

                                PCGame.mainBatch.use {
                                    it.withColor(Color.WHITE.apply { a = 0.5f }) {
                                        val frame = data.currentFrame()

                                        if (data.repeatRegion && data.regions.size == 1) {
                                            for (x in 0 until MathUtils.floor(rect.width / data.repeatRegionSize.width.toFloat())) {
                                                for (y in 0 until MathUtils.floor(rect.height / data.repeatRegionSize.height.toFloat())) {
                                                    frame.render(Rect(rect.x + x * data.repeatRegionSize.width, rect.y + y * data.repeatRegionSize.height, data.repeatRegionSize.width, data.repeatRegionSize.height), flipX, flipY, rotation, it)
                                                }
                                            }
                                        } else
                                            frame.render(rect, flipX, flipY, rotation, it)
                                    }
                                }
                            }
                        } ?: shapeRenderer.withColor(Color.GRAY) { rect(rect) }
                    }

                    if (entity.container != null) {
                        shapeRenderer.withColor(if (entity === selectEntity) Color.GREEN else Color.OLIVE) {
                            rect(entity.box)
                        }
                    }
                }
            }
            EditorUI.EditorMode.TRY_LEVEL -> {
            }
            EditorUI.EditorMode.SELECT_POINT -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner un point")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 3)
                }
            }
            EditorUI.EditorMode.SELECT_ENTITY -> {
                PCGame.mainBatch.projectionMatrix = PCGame.defaultProjection
                PCGame.mainBatch.use {
                    val layout = GlyphLayout(editorFont, "Sélectionner une entité")
                    editorFont.draw(it, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - editorFont.lineHeight * 3)
                }
            }
        }
        shapeRenderer.end()
    }

    override fun update() {
        super.update()

        updateCamera()

        // TODO Refactor
        if (!isUIHover && editorUI.editorMode == EditorUI.EditorMode.TRY_LEVEL) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                val entity = findEntityUnderMouse(false)
                if (entity != null) {
                    selectEntityTryMode = entity
                    editorUI.showInfoEntityTextWindow = true
                }
            }
        }

        if (!isUIHover && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
            level.activeRect.set(Size((Constants.viewportRatioWidth * camera.zoom).roundToInt(), (camera.viewportHeight * camera.zoom).roundToInt()), Point(camera.position.x - ((Constants.viewportRatioWidth * camera.zoom) / 2f).roundToInt(), camera.position.y - ((Constants.viewportRatioHeight * camera.zoom) / 2f).roundToInt()))

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_UP_LAYER.key)) {
                selectLayer += 1
            }
            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_DOWN_LAYER.key)) {
                selectLayer -= 1
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_SWITCH_RESIZE_MODE.key)) {
                resizeEntityMode = if (resizeEntityMode == ResizeMode.PROPORTIONAL) ResizeMode.FREE else ResizeMode.PROPORTIONAL
            }

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                val entity = findEntityUnderMouse(false)
                if (entity != null) {
                    removeEntity(entity)
                    if (selectEntities.contains(entity))
                        selectEntities.remove(entity)
                } else if (selectEntities.isNotEmpty()) {
                    selectEntities.forEach { removeEntity(it) }
                    clearSelectEntities()
                    editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                }
            }

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick && Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                if (selectEntities.isNotEmpty()) {
                    val underMouseEntity = findEntityUnderMouse(true)
                    if (underMouseEntity != null) {
                        if (selectEntities.contains(underMouseEntity)) {
                            selectEntities.remove(underMouseEntity)
                            if (selectEntity === underMouseEntity)
                                selectEntity = null
                        } else
                            addSelectEntity(underMouseEntity)
                    }
                }
            }

            updateMode()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (editorUI.editorMode == EditorUI.EditorMode.TRY_LEVEL)
                stopTryLevel()
            else
                editorUI.showExitWindow = true
        }

        if (!isUIHover && Gdx.input.isKeyJustPressed(GameKeys.EDITOR_GRID_MODE.key) && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
            gridMode.active = !gridMode.active
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_TRY_LEVEL.key)) {
            if (editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL)
                launchTryLevel()
            else
                stopTryLevel()
        }
    }

    override fun dispose() {
        super.dispose()
        editorFont.dispose()

        if (!level.levelPath.get().exists()) {
            level.deleteFiles()
        }
    }

    private fun updateMode() {
        val mousePosVec2 = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val mousePosInWorld = viewport.unproject(Vector3(mousePosVec2, 0f)).toPoint()
        val latestMousePosInWorld = viewport.unproject(Vector3(latestMousePos.x, latestMousePos.y, 0f)).toPoint()

        when (editorUI.editorMode) {
            EditorUI.EditorMode.NO_MODE -> {
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    val roundMousePosInWorld = Point(mousePosInWorld.x.roundToInt().toFloat(), mousePosInWorld.y.roundToInt().toFloat())
                    if (latestLeftButtonClick) { // Rectangle
                        selectRectangleData.endPosition = roundMousePosInWorld
                    } else { // Select
                        val entity = findEntityUnderMouse(true)
                        if (entity != null) {
                            addSelectEntity(entity)
                            editorUI.editorMode = EditorUI.EditorMode.SELECT
                        } else { // Maybe box
                            selectRectangleData.rectangleStarted = true
                            selectRectangleData.startPosition = roundMousePosInWorld
                            selectRectangleData.endPosition = selectRectangleData.startPosition
                        }
                    }
                } else if (latestLeftButtonClick && selectRectangleData.rectangleStarted) { // Bouton gauche de la souris relaché pendant cette frame
                    selectRectangleData.rectangleStarted = false

                    level.getAllEntitiesInCells(selectRectangleData.getRect()).forEach {
                        if (selectRectangleData.getRect().contains(it.box, true)) {
                            addSelectEntity(it)
                            editorUI.editorMode = EditorUI.EditorMode.SELECT
                        }
                    }
                }

                if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                    val entity = findEntityUnderMouse(true)
                    if (entity != null) {
                        addSelectEntity(entity)
                        editorUI.editorMode = EditorUI.EditorMode.COPY
                    }
                }

                if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                    findEntityUnderMouse(true)?.getCurrentState()?.getComponent<TextureComponent>()?.apply {
                        flipX = !flipX
                    }
                }
                if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                    findEntityUnderMouse(true)?.getCurrentState()?.getComponent<TextureComponent>()?.apply {
                        flipY = !flipY
                    }
                }
                if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_TEXTURE_PREVIOUS_FRAME.key)) {
                    findEntityUnderMouse(true)?.apply {
                        if (getStates().size == 1) {
                            getCurrentState().getComponent<TextureComponent>()?.apply {
                                if (groups.size == 1)
                                    groups.elementAt(0).regions.elementAt(0).cast<PackRegionData>()?.nextFrameRegion()
                            }
                        }
                    }
                }
                if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_TEXTURE_NEXT_FRAME.key)) {
                    findEntityUnderMouse(true)?.apply {
                        if (getStates().size == 1) {
                            getCurrentState().getComponent<TextureComponent>()?.apply {
                                if (groups.size == 1)
                                    groups.elementAt(0).regions.elementAt(0).cast<PackRegionData>()?.nextFrameRegion()
                            }
                        }
                    }
                }
            }
            EditorUI.EditorMode.SELECT -> {
                if (selectEntities.isEmpty() || selectEntity == null) {
                    editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                    return
                }

                /**
                 * Permet de déplacer les entités sélectionnées
                 */
                fun moveEntities(moveX: Int, moveY: Int) {
                    val (canMoveX, canMoveY) = let {
                        var canMoveX = true
                        var canMoveY = true

                        selectEntities.forEach {
                            if ((it.box.x + moveX) !in 0..level.matrixRect.width - it.box.width)
                                canMoveX = false
                            if ((it.box.y + moveY) !in 0..level.matrixRect.height - it.box.height)
                                canMoveY = false
                        }

                        canMoveX to canMoveY
                    }

                    selectEntities.forEach {
                        it.box.move(if (canMoveX) moveX.toFloat() else 0f, if (canMoveY) moveY.toFloat() else 0f)
                    }
                }

                val selectEntityRect = selectEntity!!.box

                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                    if (!latestLeftButtonClick) {
                        if (selectEntityMode == SelectEntityMode.NO_MODE) {
                            val entity = findEntityUnderMouse(true)
                            when {
                                entity == null -> { // Se produit lorsque le joueur clique dans le vide, dans ce cas on désélectionne les entités sélectionnées
                                    clearSelectEntities()
                                    editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                                }
                                selectEntities.contains(entity) -> // Dans ce cas-ci, le joueur à sélectionné une autre entité dans celles qui sont sélectionnées
                                    selectEntity = entity
                                else -> { // Dans ce cas-ci, il y a un groupe d'entité sélectionné ou aucun et le joueur en sélectionne un nouveau ou un en dehors de la sélection
                                    clearSelectEntities()
                                    addSelectEntity(entity)
                                }
                            }
                        }

                    } else if (selectEntity != null) { // Le joueur maintient le clique gauche durant plusieurs frames et a bougé la souris {
                        if (selectEntityMode == SelectEntityMode.NO_MODE)
                            selectEntityMode = SelectEntityMode.MOVE

                        val deltaMouseX = (latestMousePosInWorld.x - mousePosInWorld.x).roundToInt()
                        val deltaMouseY = (latestMousePosInWorld.y - mousePosInWorld.y).roundToInt()

                        when (selectEntityMode) {
                            SelectEntityMode.NO_MODE -> {
                            }
                            SelectEntityMode.HORIZONTAL_RESIZE_RIGHT -> {
                                selectEntityRect.width = (selectEntityRect.width - deltaMouseX).clamp(1, Constants.maxEntitySize)
                            }
                            SelectEntityMode.HORIZONTAL_RESIZE_LEFT -> {
                                selectEntityRect.width = (selectEntityRect.width + deltaMouseX).clamp(1, Constants.maxEntitySize)
                                selectEntityRect.x = (selectEntityRect.x - deltaMouseX).clamp(0f, level.matrixRect.width.toFloat() - selectEntityRect.width)
                            }
                            SelectEntityMode.VERTICAL_RESIZE_BOTTOM -> {
                                selectEntityRect.height = (selectEntityRect.height + deltaMouseY).clamp(1, Constants.maxEntitySize)
                                selectEntityRect.y = (selectEntityRect.y - deltaMouseY).clamp(0f, level.matrixRect.height.toFloat() - selectEntityRect.height)
                            }
                            SelectEntityMode.VERTICAL_RESIZE_TOP -> {
                                selectEntityRect.height = (selectEntityRect.height - deltaMouseY).clamp(1, Constants.maxEntitySize)
                            }
                            SelectEntityMode.DIAGONALE_RESIZE -> {
                                var deltaX = deltaMouseX
                                var deltaY = deltaMouseY

                                if (resizeEntityMode == ResizeMode.PROPORTIONAL) {
                                    if (Math.abs(deltaX) > Math.abs(deltaY))
                                        deltaY = deltaX
                                    else
                                        deltaX = deltaY
                                }

                                selectEntityRect.width = (selectEntityRect.width - deltaX).clamp(1, Constants.maxEntitySize)
                                selectEntityRect.height = (selectEntityRect.height - deltaY).clamp(1, Constants.maxEntitySize)
                            }
                            SelectEntityMode.MOVE -> {
                                val moveX = -deltaMouseX + (camera.position.x - latestCameraPos.x)
                                val moveY = -deltaMouseY + (camera.position.y - latestCameraPos.y)

                                moveEntities(moveX.roundToInt(), moveY.roundToInt())
                            }
                        }
                    }
                    // Le bouton gauche n'est pas appuyé pendant cette frame
                } else {
                    when {
                    // Diagonale resize
                        Circle(selectEntityRect.right(), selectEntityRect.top(), 10f).contains(mousePosInWorld) -> {
                            selectEntityMode = SelectEntityMode.DIAGONALE_RESIZE
                        }
                    // Horizontal right resize
                        mousePosInWorld.x in selectEntityRect.right() - 1..selectEntityRect.right() + 1 && mousePosInWorld.y in selectEntityRect.y..selectEntityRect.top() -> {
                            selectEntityMode = SelectEntityMode.HORIZONTAL_RESIZE_RIGHT
                            ImGui.mouseCursor = MouseCursor.ResizeEW
                        }
                    // Horizontal left resize
                        mousePosInWorld.x in selectEntityRect.left() - 1..selectEntityRect.left() + 1 && mousePosInWorld.y in selectEntityRect.y..selectEntityRect.top() -> {
                            selectEntityMode = SelectEntityMode.HORIZONTAL_RESIZE_LEFT
                            ImGui.mouseCursor = MouseCursor.ResizeEW
                        }
                    // Vertical top resize
                        mousePosInWorld.y in selectEntityRect.top() - 1..selectEntityRect.top() + 1 && mousePosInWorld.x in selectEntityRect.x..selectEntityRect.right() -> {
                            selectEntityMode = SelectEntityMode.VERTICAL_RESIZE_TOP
                            ImGui.mouseCursor = MouseCursor.ResizeNS
                        }
                    // Vertical bottom resize
                        mousePosInWorld.y in selectEntityRect.bottom() - 1..selectEntityRect.bottom() + 1 && mousePosInWorld.x in selectEntityRect.x..selectEntityRect.right() -> {
                            selectEntityMode = SelectEntityMode.VERTICAL_RESIZE_BOTTOM
                            ImGui.mouseCursor = MouseCursor.ResizeNS
                        }
                        else -> {
                            selectEntityMode = SelectEntityMode.NO_MODE
                            ImGui.mouseCursor = MouseCursor.Arrow
                        }
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        if (findEntityUnderMouse(false) == null) {
                            clearSelectEntities()
                            editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                        } else {
                            editorUI.showInfoEntityWindow = true
                        }
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

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key))
                        editorUI.editorMode = EditorUI.EditorMode.COPY
                }
            }
            EditorUI.EditorMode.COPY -> {
                if (selectEntities.isEmpty() && selectEntity == null /* Permet de vérifier si on est pas entrain d'ajouter une nouvelle entité */) {
                    editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                    return
                }

                if ((Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key))
                        && !Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                    val entityUnderMouse = findEntityUnderMouse(true)
                    if (entityUnderMouse != null) {
                        if (selectEntities.contains(entityUnderMouse))
                            selectEntity = entityUnderMouse
                        else {
                            clearSelectEntities()
                            addSelectEntity(entityUnderMouse)
                        }
                    } else {
                        clearSelectEntities()
                        editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                    }
                }

                if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && (!latestRightButtonClick || gridMode.active)) {
                    val copySelectEntity = level.copyEntity(selectEntity!!)

                    var posX = copySelectEntity.position().x
                    var posY = copySelectEntity.position().y

                    val width = copySelectEntity.size().width
                    val height = copySelectEntity.size().height

                    var moveToCopyEntity = true

                    var useMousePos = false
                    if (selectEntity!!.container != null) {
                        when {
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_LEFT.key) -> {
                                val minXPos = let {
                                    var x = selectEntity!!.position().x
                                    selectEntities.forEach {
                                        x = Math.min(x, it.position().x)
                                    }
                                    x
                                }
                                posX = minXPos - width
                            }
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_RIGHT.key) -> {
                                val maxXPos = let {
                                    var x = selectEntity!!.position().x
                                    selectEntities.forEach {
                                        x = Math.max(x, it.position().x)
                                    }
                                    x
                                }
                                posX = maxXPos + width
                            }
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_DOWN.key) -> {
                                val minYPos = let {
                                    var y = selectEntity!!.position().y
                                    selectEntities.forEach {
                                        y = Math.min(y, it.position().y)
                                    }
                                    y
                                }
                                posY = minYPos - height
                            }
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_UP.key) -> {
                                val maxYPos = let {
                                    var y = selectEntity!!.position().y
                                    selectEntities.forEach {
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
                        val freePos = Point(mousePosInWorld.x - width / 2f, mousePosInWorld.y - height / 2f)
                        val point = if (smartPositioning) {
                            val side = getSmartPositioningSide(mousePosInWorld)
                            if (side == null)
                                freePos
                            else when (side.first) {
                                BoxSide.Left -> Point(side.second.box.left() - width, side.second.box.bottom())
                                BoxSide.Right -> Point(side.second.box.right(), side.second.box.bottom())
                                BoxSide.Up -> Point(side.second.box.left(), side.second.box.top())
                                BoxSide.Down -> Point(side.second.box.left(), side.second.box.bottom() - height)
                                BoxSide.All -> freePos
                            }
                        } else freePos

                        posX = point.x
                        posY = point.y

                        // Permet de vérifier si l'entité copiée est nouvelle ou pas (si elle est nouvelle, ça veut dire qu'elle n'a pas encore de conteneur)
                        if (selectEntity!!.container != null)
                            moveToCopyEntity = false
                    }

                    posX = posX.clamp(0f, level.matrixRect.width.toFloat() - width).roundToInt().toFloat()
                    posY = posY.clamp(0f, level.matrixRect.height.toFloat() - height).roundToInt().toFloat()

                    // On vérifie si le clique droit viens juste d'être appuié si le gridMode est actif pour être sûr de ne pas placer trop de copie
                    if ((useMousePos && (selectEntities.size == 1 || !latestRightButtonClick)) || (!gridMode.active || !latestRightButtonClick)) {
                        var putSuccessful = true
                        if (gridMode.active && useMousePos && selectEntities.size == 1) {
                            putSuccessful = gridMode.putEntity(level.activeRect, Point(mousePosInWorld.x, mousePosInWorld.y), copySelectEntity, level)
                        } else
                            copySelectEntity.box.position = Point(posX, posY)

                        val copyEntities = mutableListOf<Entity>()

                        if (putSuccessful) {
                            level.addEntity(copySelectEntity)
                            copyEntities.add(copySelectEntity)
                        } else
                            moveToCopyEntity = false

                        selectEntities.filter { it !== selectEntity }.forEach {
                            val deltaX = it.position().x - selectEntity!!.position().x
                            val deltaY = it.position().y - selectEntity!!.position().y

                            level.addEntity(SerializationFactory.copy(it).apply {
                                val pos = Point((copySelectEntity.position().x + deltaX).clamp(0f, level.matrixRect.width.toFloat() - this.size().width), (copySelectEntity.position().y + deltaY).clamp(0f, level.matrixRect.height.toFloat() - this.size().height))

                                this.box.position = pos

                                copyEntities += this
                            })
                        }

                        if (moveToCopyEntity) {
                            clearSelectEntities()
                            copyEntities.forEach { addSelectEntity(it) }
                        }
                    }
                }
            }
            EditorUI.EditorMode.SELECT_POINT -> {
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                    editorUI.onSelectPoint(mousePosInWorld)
                    editorUI.editorMode = editorUI.previousEditorMode
                }
            }
            EditorUI.EditorMode.SELECT_ENTITY -> {
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !latestLeftButtonClick) {
                    val entity = findEntityUnderMouse(false)
                    editorUI.onSelectEntity(entity)
                    editorUI.editorMode = editorUI.previousEditorMode
                }
            }
        }

        latestLeftButtonClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
        latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
        latestMousePos = mousePosVec2.toPoint()
        latestCameraPos = camera.position.cpy()
    }

    private fun updateCamera() {
        if (editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
            var moveCameraX = 0f
            var moveCameraY = 0f

            if (targetCameraZoom != camera.zoom) {
                val px = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

                camera.zoom = MathUtils.lerp(camera.zoom, targetCameraZoom, 0.1f)

                camera.update()

                val nextPX = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

                camera.position.add(px.x - nextPX.x, px.y - nextPX.y, 0f)
            }

            if (camera.zoom > level.matrixRect.width / camera.viewportWidth || camera.zoom > level.matrixRect.height / camera.viewportHeight) {
                targetCameraZoom = camera.zoom.clamp(0.5f, level.matrixRect.width / camera.viewportWidth).clamp(0.5f, level.matrixRect.height / camera.viewportHeight)
            }

            if (!isUIHover) {
                if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_LEFT.key))
                    moveCameraX -= cameraMoveSpeed
                if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_RIGHT.key))
                    moveCameraX += cameraMoveSpeed
                if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_UP.key))
                    moveCameraY += cameraMoveSpeed
                if (Gdx.input.isKeyPressed(GameKeys.EDITOR_CAMERA_DOWN.key))
                    moveCameraY -= cameraMoveSpeed

                if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key))
                    targetCameraZoom = 1f
            }

            val minCameraX = camera.zoom * (camera.viewportWidth / 2)
            val maxCameraX = level.matrixRect.width - minCameraX
            val minCameraY = camera.zoom * (camera.viewportHeight / 2)
            val maxCameraY = level.matrixRect.height - minCameraY

            val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
            val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)

            camera.position.set(MathUtils.clamp(x, minCameraX, maxCameraX), MathUtils.clamp(y, minCameraY, maxCameraY), 0f)
        } else
            entityContainer.cast<Level>()?.updateCamera(camera, true)

        camera.update()
    }

    /**
     * Permet de retourner le côté le plus proche d'une entité par rapport à la souris
     *
     * -----
     * |GGHDD|
     * |G***D|
     * |GGBDD|
     * -----
     *
     * G = Gauche
     * R = Droite
     * H = Haut
     * B = Bas
     * * = Rien
     * @return Un côté de la box de l'entité le plus proche du point
     */
    private fun getSmartPositioningSide(point: Point): Pair<BoxSide, Entity>? {
        findEntityUnderMouse(false)?.apply {
            // Gauche
            if (Rect(box.position, Size(box.width / 4, box.height)).contains(point))
                return BoxSide.Left to this
            // Droite
            if (Rect(Point(box.right() - box.width / 4, box.bottom()), Size(box.width / 4, box.height)).contains(point))
                return BoxSide.Right to this
            // Haut
            if (Rect(Point(box.left() + box.width / 4f, box.top() - box.height / 4f), Size(box.width - box.width / 4, box.height / 4)).contains(point))
                return BoxSide.Up to this
            // Bas
            if (Rect(Point(box.left() + box.width / 4f, box.bottom()), Size(box.width - box.width / 4, box.height / 4)).contains(point))
                return BoxSide.Down to this
        }
        return null
    }

    private fun setCopyEntity(entity: Entity) {
        clearSelectEntities()
        addSelectEntity(entity)
        editorUI.editorMode = EditorUI.EditorMode.COPY
    }

    /**
     * Permet d'ajouter une nouvelle entité sélectionnée
     */
    private fun addSelectEntity(entity: Entity) {
        selectEntities.add(entity)

        if (selectEntities.size == 1) {
            selectEntity = entity
        }
    }

    /**
     * Permet de supprimer les entités sélectionnées de la liste -> ils ne sont pas supprimés du niveau, juste déséléctionnés
     */
    private fun clearSelectEntities() {
        selectEntities.clear()
        selectEntity = null
    }

    /**
     * Permet de retourner l'entité sous le pointeur par rapport à son layer
     */
    private fun findEntityUnderMouse(replaceEditorLayer: Boolean): Entity? {
        val mousePosInWorld = viewport.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)).toPoint()

        val entitiesUnderMouse = (entityContainer as Level).run { getAllEntitiesInCells(getActiveGridCells()).filter { it.box.contains(mousePosInWorld) } }

        if (!entitiesUnderMouse.isEmpty()) {
            val goodLayerEntity = entitiesUnderMouse.find { it.layer == selectLayer }
            return if (goodLayerEntity != null)
                goodLayerEntity
            else {
                val entity = entitiesUnderMouse.first()
                if (replaceEditorLayer)
                    selectLayer = entity.layer
                entity
            }
        }

        return null
    }

    private fun removeEntity(entity: Entity) {
        if (entity === selectEntity) {
            selectEntity = null
        }
        entity.removeFromParent()
    }

    private fun launchTryLevel() {
        // Permet de garantir que toutes les entités auront bien un alpha de 1 même si la transition vers cette scène n'est pas finie
        alpha = 1f

        backupTryModeCameraPos = camera.position.cpy()
        backupTryModeCameraZoom = camera.zoom

        editorUI.editorMode = EditorUI.EditorMode.TRY_LEVEL

        if (level.music != null) {
            MusicsManager.startMusic(level.music!!, true)
        }

        entityContainer = SerializationFactory.copy(level).apply {
            this.exit = {
                if (!editorUI.godModeTryMode) stopTryLevel()
            }
            this.activeRect.position = level.activeRect.position
            this.drawDebugCells = level.drawDebugCells

            this.update()
        }

        entityContainer.entitiesInitialStartActions()
    }

    private fun stopTryLevel() {
        clearSelectEntities()
        editorUI.editorMode = EditorUI.EditorMode.NO_MODE

        entityContainer = level
        camera.position.set(backupTryModeCameraPos)
        camera.zoom = backupTryModeCameraZoom

        MusicsManager.startMusic(PCGame.menuMusic, true)

        selectEntityTryMode = null
    }

    private fun saveLevelToFile() {
        try {
            SerializationFactory.serializeToFile(level, level.levelPath.get())

            val levelPreviewPath = level.levelPath.get().parent().child(Constants.levelPreviewFile)

            PCGame.scenesManager.takeScreenshotWithoutImGui {
                val resizePixmap = Pixmap(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight - editorUI.menuBarHeight, Pixmap.Format.RGBA8888)
                resizePixmap.drawPixmap(it, 0, -editorUI.menuBarHeight)

                PixmapIO.writePNG(levelPreviewPath, resizePixmap)

                resizePixmap.dispose()
                it.dispose()
            }

            try {
                ResourcesManager.unloadAsset(levelPreviewPath)
            } catch(e: GdxRuntimeException) {
                // L'erreur peut se produire si l'image de prévisualisation n'existait pas avant, c'est le cas lors de la première sauvegarde d'un nouveau niveau.
                // On peut sans soucis l'ignorer
            }
        } catch (e: Exception) {
            Log.error(e) { "Erreur lors de l'enregistrement du niveau !" }
        }
    }

    //region UI

    private fun drawUI() {
        with(ImGui) {
            drawMainMenuBar()

            drawInfoEditorWindow()

            if (editorUI.showEntityFactoryWindow && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL)
                drawEntityFactoryWindow()

            if (gridMode.active && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL)
                drawGridSettingsWindow()

            if (editorUI.showInfoPrefabWindow && editorUI.prefabInfoPrefabWindow != null && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL)
                drawInfoPrefabWindow(editorUI.prefabInfoPrefabWindow!!)

            val entityUnderMouse = findEntityUnderMouse(false)
            if (entityUnderMouse != null && !isUIHover) {
                functionalProgramming.withTooltip {
                    ImGuiHelper.textColored(Color.RED, "${entityUnderMouse.name} #${entityUnderMouse.id()}")
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "couche :", entityUnderMouse.layer)

                    ImGuiHelper.textPropertyColored(Color.ORANGE, "x :", entityUnderMouse.box.x)
                    sameLine()
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "y :", entityUnderMouse.box.y)

                    ImGuiHelper.textPropertyColored(Color.ORANGE, "w :", entityUnderMouse.box.width)
                    sameLine()
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "h :", entityUnderMouse.box.height)
                }
            }

            if (editorUI.showInfoEntityTextWindow && selectEntityTryMode != null && editorUI.editorMode == EditorUI.EditorMode.TRY_LEVEL) {
                drawInfoEntityTextWindow(selectEntityTryMode!!)
            }

            if (editorUI.showInfoEntityWindow && selectEntity != null && selectEntity?.container != null
                    && editorUI.editorMode != EditorUI.EditorMode.SELECT_POINT && editorUI.editorMode != EditorUI.EditorMode.SELECT_ENTITY && editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
                drawInfoEntityWindow(selectEntity!!)
            }

            if (editorUI.showExitWindow) {
                drawExitWindow()
            }
        }
    }

    private fun drawInfoEditorWindow() {
        with(ImGui) {
            setNextWindowPos(Vec2(10f, 10f + (g.fontBaseSize + g.style.framePadding.y * 2f).roundToInt()), Cond.Once)
            functionalProgramming.withWindow("editor info", null, WindowFlag.AlwaysAutoResize.i or WindowFlag.NoTitleBar.i or WindowFlag.NoBringToFrontOnFocus.i) {
                ImGuiHelper.textPropertyColored(Color.ORANGE, "Nombre d'entités :", entityContainer.getEntitiesData().size)

                if (editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "Couche sélectionnée :", selectLayer)
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "Redimensionnement :", resizeEntityMode.resizeName)
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "Mode de l'éditeur :", editorUI.editorMode.modeName)
                    ImGuiHelper.textPropertyColored(Color.ORANGE, "Niveau de zoom :", Math.round(camera.zoom * 100f) / 100f)
                    functionalProgramming.collapsingHeader("Paramètres de l'éditeur") {
                        checkbox("Afficher la fenêtre Fabrique d'entités", editorUI::showEntityFactoryWindow)
                        checkbox("Afficher la grille", gridMode::active)
                        checkbox("Placement intelligent", ::smartPositioning)
                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            inputInt("Couche sélectionnée", ::selectLayer, 1, 2)
                        }
                    }
                } else {
                    ImGuiHelper.textColored(Color.ORANGE, "Test du niveau..")
                    checkbox("Mode dieu", editorUI::godModeTryMode)
                    checkbox("Mettre à jour", entityContainer::allowUpdating)
                }
            }
        }
    }

    private fun drawExitWindow() {
        ImGuiHelper.withCenteredWindow("Sauvegarder le niveau ?", editorUI::showExitWindow, Vec2(240f, 105f), WindowFlag.NoResize.i or WindowFlag.NoCollapse.i or WindowFlag.NoTitleBar.i) {
            fun showMainMenu() {
                PCGame.scenesManager.loadScene(MainMenuScene(null, false))
            }

            if (ImGui.button("Sauvegarder", Vec2(225f, 0))) {
                saveLevelToFile()
                showMainMenu()
            }
            if (ImGui.button("Abandonner les modifications", Vec2(225f, 0))) {
                if (!level.levelPath.get().exists()) {
                    level.deleteFiles()
                }
                showMainMenu()
            }
            if (ImGui.button("Annuler", Vec2(225f, 0))) {
                editorUI.showExitWindow = false
            }
        }
    }

    private fun drawMainMenuBar() {
        with(ImGui) {
            mainMenuBar {
                if (editorUI.editorMode != EditorUI.EditorMode.TRY_LEVEL) {
                    menu("Fichier") {
                        menuItem("Importer des ressources..") {
                            try {
                                val files = Utility.openFileDialog("Importer des ressources", "Ressources", arrayOf(*Constants.levelTextureExtension, *Constants.levelPackExtension, *Constants.levelSoundExtension, *Constants.levelScriptExtension), true)
                                level.addResources(*files.toTypedArray())
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
                            editorUI.showExitWindow = true
                        }
                    }

                    menu("Éditer") {
                        menu("Tags") {
                            val popupTitle = "add tag popup"
                            val it = level.tags.iterator()

                            var counter = 0
                            while (it.hasNext()) {
                                val tag = it.next()

                                functionalProgramming.withId("del btn ${++counter}") {
                                    pushItemFlag(ItemFlag.Disabled.i, Tags.values().any { it.tag == tag })
                                    if (button("Suppr.")) {
                                        it.remove()
                                    }
                                    popItemFlag()
                                }

                                sameLine(0f, style.itemInnerSpacing.x)

                                text(tag)
                            }

                            if (button("Ajouter un tag", Vec2(-1, 0))) {
                                openPopup(popupTitle)
                            }

                            functionalProgramming.popup(popupTitle) {
                                ImGuiHelper.inputText("nom", editorUI::addTagPopupNameBuffer)
                                if (button("Ajouter", Vec2(-1, 0))) {
                                    if (editorUI.addTagPopupNameBuffer.isNotBlank() && level.tags.none { it == editorUI.addTagPopupNameBuffer }) {
                                        level.tags.add(editorUI.addTagPopupNameBuffer)
                                        closeCurrentPopup()
                                    }
                                }
                            }
                        }
                        menu("Options du niveau") {
                            fun updateBackground(newBackground: Background) {
                                editorUI.settingsLevelBackgroundType.obj = newBackground.type
                                level.background = newBackground
                                background = newBackground
                            }

                            functionalProgramming.collapsingHeader("arrière plan") {
                                functionalProgramming.withIndent {
                                    val colorPopupTitle = "level background color popup"

                                    text("couleur : ")
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    if (colorButton("level background color", Vec4(level.backgroundColor[0], level.backgroundColor[1], level.backgroundColor[2], 0f))) {
                                        openPopup(colorPopupTitle)
                                    }

                                    ImGuiHelper.enum("type de fond d'écran", editorUI.settingsLevelBackgroundType)

                                    when (editorUI.settingsLevelBackgroundType.obj.cast<BackgroundType>()) {
                                        BackgroundType.Standard -> {
                                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                                if (sliderInt("fond d'écran", editorUI.settingsLevelStandardBackgroundIndex, 0, PCGame.getStandardBackgrounds().size - 1)) {
                                                    updateBackground(PCGame.getStandardBackgrounds()[editorUI.settingsLevelStandardBackgroundIndex[0]])
                                                }
                                            }
                                        }
                                        BackgroundType.Parallax -> {
                                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                                if (sliderInt("fond d'écran", editorUI.settingsLevelParallaxBackgroundIndex, 0, PCGame.getParallaxBackgrounds().size - 1)) {
                                                    updateBackground(PCGame.getParallaxBackgrounds()[editorUI.settingsLevelParallaxBackgroundIndex[0]])
                                                }
                                            }
                                        }
                                        BackgroundType.Imported -> {
                                            if(button("Importer...", Vec2(Constants.defaultWidgetsWidth, 0))) {
                                                try {
                                                    val backgroundFile =   Utility.openFileDialog("Importer un fond d'écran", "Image", arrayOf("png"), false)
                                                    backgroundFile.firstOrNull()?.also {
                                                        val copyPath = level.customBackgroundPath()
                                                        it.copyTo(copyPath)
                                                        updateBackground(ImportedBackground(copyPath.toFileWrapper()))
                                                    }
                                                } catch (e: Exception) {
                                                    Log.error(e) { "Erreur lors de l'importation du fond d'écran !" }
                                                }
                                            }
                                        }
                                        BackgroundType.None -> {
                                            level.background = null
                                            background = null
                                        }
                                    }

                                    functionalProgramming.popup(colorPopupTitle) {
                                        if (colorEdit3("couleur de l'arrière plan", level.backgroundColor))
                                            backgroundColors = level.backgroundColor
                                    }
                                }
                            }
                            val musics = PCGame.gameMusics.toMutableList()

                            if (level.music != null && musics.none { it.path == level.music!!.path })
                                musics.add(level.music!!)

                            val currentMusicIndex = intArrayOf(musics.indexOfFirst { it.path == level.music?.path })

                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                if (ImGuiHelper.comboWithSettingsButton("musique", currentMusicIndex, musics.map { it.path.toString() }, {
                                            if (button("Importer")) {
                                                Utility.openFileDialog("Importer une musique", "Musique", arrayOf("mp3"), false).firstOrNull()?.apply {
                                                    val dest = level.levelPath.get().parent().child(Constants.levelCustomMusicFile)
                                                    this.copyTo(dest)
                                                    level.music = resourceWrapperOf(dest.toFileWrapper())
                                                }
                                            }
                                        }, searchBar = true))
                                    level.music = musics[currentMusicIndex[0]]
                            }

                            ImGuiHelper.entity(level.followEntity, level, editorUI, "entité suivie")

                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                inputInt("gravité", level::gravitySpeed)
                                inputInt("largeur", level::matrixWidth)
                                inputInt("hauteur", level::matrixHeight)
                                sliderFloat("zoom initial", level::initialZoom, 0.1f, 2f, "%.1f")
                            }
                        }
                    }
                    menu("Créer une entité") {
                        fun createEntity(prefab: Prefab) {
                            val entity = prefab.create(Point())
                            setCopyEntity(entity)
                        }

                        PrefabType.values().forEach { type ->
                            menu(type.name) {
                                if (type == PrefabType.All) {
                                    level.resourcesPrefabs().forEach {
                                        menuItem(it.name) {
                                            createEntity(it)
                                        }
                                    }
                                }
                                PrefabFactory.values().filter { it.type == type }.forEach {
                                    menuItem(it.name.removeSuffix("_${type.name}")) {
                                        createEntity(it.prefab)
                                    }
                                }
                            }
                        }
                    }
                    ImGui.cursorPosX = Gdx.graphics.width.toFloat() - 175f

                    val btns = {
                        if (smallButton("Sauvegarder"))
                            saveLevelToFile()

                        sameLine(0f, style.itemInnerSpacing.x)
                        functionalProgramming.withStyleColor(Col.Text, Vec4.fromColor(102, 255, 147, 255)) {
                            if (smallButton("Essayer"))
                                launchTryLevel()
                        }
                    }

                    if (PCGame.darkUI) {
                        functionalProgramming.withStyleColor(Col.Button, Vec4.fromColor(49, 54, 62, 200), Col.ButtonHovered, Vec4.fromColor(49, 54, 62, 255), Col.ButtonActive, Vec4.fromColor(49, 54, 62, 255)) {
                            btns()
                        }
                    } else
                        btns()

                } else {
                    menuItem("Arrêter l'essai") {
                        stopTryLevel()
                    }
                }
            }
        }
    }

    private fun drawGridSettingsWindow() {
        with(ImGui) {
            functionalProgramming.withWindow("Réglages de la grille", null, WindowFlag.AlwaysAutoResize.i) {
                val size = intArrayOf(gridMode.cellWidth, gridMode.cellHeight)

                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    if (inputInt2("taille", size)) {
                        gridMode.cellWidth = size[0].clamp(1, Constants.maxEntitySize)
                        gridMode.cellHeight = size[1].clamp(1, Constants.maxEntitySize)
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

    private fun drawEntityFactoryWindow() {
        with(ImGui) {
            val nextWindowSize = Vec2(210f, Gdx.graphics.height - editorUI.menuBarHeight)
            setNextWindowSize(nextWindowSize, Cond.Once)
            setNextWindowPos(Vec2(Gdx.graphics.width - nextWindowSize.x, editorUI.menuBarHeight), Cond.Once)
            setNextWindowSizeConstraints(Vec2(nextWindowSize.x, 250f), Vec2(nextWindowSize.x, Gdx.graphics.height))
            functionalProgramming.withWindow("Fabrique d'entités", editorUI::showEntityFactoryWindow) {
                ImGuiHelper.enum("mode", editorUI.entityFactoryMode.cast())

                separator()

                fun addImageBtn(region: TextureRegion, prefab: Prefab, showTooltip: Boolean) {
                    // Dé-flip la texture
                    region.flip(region.isFlipX, region.isFlipY)

                    if (imageButton(region.texture.textureObjectHandle, Vec2(50f, 50f), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                        setCopyEntity(prefab.create(Point()).apply { this.layer = selectLayer })
                    }

                    if (showTooltip && isItemHovered()) {
                        functionalProgramming.withTooltip {
                            text(prefab.name)
                        }
                    }
                }

                when (editorUI.entityFactoryMode.obj) {
                    EditorScene.EditorUI.EntityFactoryMode.Sprite -> {
                        var importedPacks = false
                        functionalProgramming.withGroup {
                            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                                val types = PCGame.gamePacks.map { it.key.name() } + "packs importés"
                                combo("type", editorUI::entityFactorySpritePackTypeIndex, types)
                                importedPacks = editorUI.entityFactorySpritePackTypeIndex == types.lastIndex
                                combo("pack", editorUI::entityFactorySpritePackIndex,
                                        if (importedPacks) level.resourcesPacks().map { it.path.toString() }
                                        else PCGame.gamePacks.entries.elementAtOrNull(editorUI.entityFactorySpritePackTypeIndex)?.value?.map { it.path.toString() }
                                                ?: arrayListOf())
                                if (!editorUI.entityFactorySpriteRealSize)
                                    ImGuiHelper.size(editorUI::entityFactorySpriteCustomSize, Size(1), Size(Constants.maxEntitySize))

                                checkbox("Taille réelle", editorUI::entityFactorySpriteRealSize)
                                checkbox("Physique", editorUI::entityFactorySpritePhysics)
                            }
                        }

                        (if (importedPacks) level.resourcesPacks().getOrNull(editorUI.entityFactorySpritePackIndex) else PCGame.gamePacks.entries.elementAtOrNull(editorUI.entityFactorySpritePackTypeIndex)?.value?.getOrNull(editorUI.entityFactorySpritePackIndex))?.also { pack ->
                            pack()?.regions?.sortedBy { it.name }?.forEachIndexed { index, region ->
                                val size = if (editorUI.entityFactorySpriteRealSize) region.let { Size(it.regionWidth, it.regionHeight) } else editorUI.entityFactorySpriteCustomSize
                                val prefab = if (editorUI.entityFactorySpritePhysics) PrefabSetup.setupPhysicsSprite(pack, region.name, size) else PrefabSetup.setupSprite(pack, region.name, size)

                                addImageBtn(region, prefab, false)

                                if ((index + 1) % 3 != 0)
                                    sameLine(0f, style.itemInnerSpacing.x)
                            }
                        }
                    }
                    EditorScene.EditorUI.EntityFactoryMode.Tag -> {
                        val tags = level.tags.apply { remove(Tags.Empty.tag) }

                        ImGuiHelper.comboWithSettingsButton("type", editorUI::entityFactoryTagIndex, level.tags.apply { remove(Tags.Empty.tag) }, {
                            checkbox("afficher seulement les préfabs créés", editorUI::entityFactoryTagShowOnlyUser)
                        })

                        val tag = tags.elementAtOrElse(editorUI.entityFactoryTagIndex, { Tags.Sprite.tag })

                        var index = 0
                        val prefabs = level.resourcesPrefabs() + if (editorUI.entityFactoryTagShowOnlyUser) listOf() else PrefabFactory.values().map { it.prefab }
                        prefabs.filter { it.prefabEntity.tag == tag }.forEach {
                            it.prefabEntity.getCurrentState().getComponent<TextureComponent>()?.apply {
                                groups.elementAtOrNull(currentIndex)?.apply {
                                    addImageBtn(currentFrame().getTextureRegion(), it, true)

                                    if (isItemHovered() && Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                                        editorUI.prefabInfoPrefabWindow = it
                                        editorUI.showInfoPrefabWindow = true
                                    }

                                    if ((++index) % 3 != 0)
                                        sameLine(0f, style.itemInnerSpacing.x)
                                }
                            }
                        }

                    }
                    EditorScene.EditorUI.EntityFactoryMode.Group -> {
                        GroupFactory.values().forEach {
                            val it = it.group()
                            var region: TextureRegion = ResourcesManager.defaultPackRegion
                            it.mainEntity.prefabEntity.getCurrentState().getComponent<TextureComponent>()?.apply {
                                groups.elementAtOrNull(currentIndex)?.apply {
                                    region = this.currentFrame().getTextureRegion()
                                }
                            }

                            if (imageButton(region.texture.textureObjectHandle, Vec2(50f, 50f), Vec2(region.u, region.v), Vec2(region.u2, region.v2))) {
                                clearSelectEntities()
                                it.create(Point(), level, false).forEach(::addSelectEntity)
                                editorUI.editorMode = EditorUI.EditorMode.COPY
                            }

                            if (isItemHovered()) {
                                functionalProgramming.withTooltip {
                                    text(it.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawInfoPrefabWindow(prefab: Prefab) {
        with(ImGui) {
            functionalProgramming.withWindow("Réglages du prefab", editorUI::showInfoPrefabWindow, WindowFlag.AlwaysAutoResize.i) {
                val isGamePrefab = PrefabFactory.values().any { it.prefab === prefab }

                if (isGamePrefab) {
                    ImGuiHelper.textColored(Color.RED, "Modifications temporaires !")
                    if (isItemHovered()) {
                        functionalProgramming.withTooltip {
                            text("Les modifications apportés à ce prefab seront temporaires !\nMerci de créer un nouveau prefab pour sauvegarder les changements apportés.")
                        }
                    }
                } else {
                    if (button("Supprimer ce prefab", Vec2(-1, 0))) {
                        level.removePrefab(prefab)
                        editorUI.showInfoPrefabWindow = false
                        editorUI.prefabInfoPrefabWindow = null
                    }
                }

                drawInfoEntityProps(prefab.prefabEntity)
            }
        }
    }

    private fun drawInfoEntityProps(entity: Entity) {
        val newStateTitle = "Nouveau état"
        val addComponentTitle = "Ajouter un component"

        with(ImGui) {
            ImGuiHelper.insertUIFields(entity, entity, level, editorUI)

            separator()

            ImGuiHelper.comboWithSettingsButton("état", editorUI::entityCurrentStateIndex, entity.getStates().map { it.name }, {
                if (button("Ajouter un état")) {
                    openPopup(newStateTitle)
                }

                if (entity.getStates().size > 1) {
                    sameLine()
                    if (button("Supprimer ${entity.getStateOrDefault(editorUI.entityCurrentStateIndex).name}")) {
                        entity.removeState(editorUI.entityCurrentStateIndex)
                        editorUI.entityCurrentStateIndex = Math.max(0, editorUI.entityCurrentStateIndex - 1)
                    }
                }

                functionalProgramming.popup(newStateTitle) {
                    val comboItems = mutableListOf("État vide").apply { addAll(entity.getStates().map { "Copier de : ${it.name}" }) }

                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        combo("type", editorUI::entityAddStateComboIndex, comboItems)
                    }

                    ImGuiHelper.inputText("nom", editorUI::createStateInputText)

                    if (button("Ajouter", Vec2(Constants.defaultWidgetsWidth, 0))) {
                        if (editorUI.entityAddStateComboIndex == 0)
                            entity.addState(editorUI.createStateInputText) {}
                        else
                            entity.addState(SerializationFactory.copy(entity.getStateOrDefault(editorUI.entityAddStateComboIndex - 1)).apply { name = editorUI.createStateInputText })
                        closeCurrentPopup()
                    }
                }
            })

            ImGuiHelper.action("action de départ", entity.getStateOrDefault(editorUI.entityCurrentStateIndex)::startAction, entity, level, editorUI)

            val components = entity.getStateOrDefault(editorUI.entityCurrentStateIndex).getComponents()

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("component", editorUI::entityCurrentComponentIndex, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })
            }

            val component = components.elementAtOrNull(editorUI.entityCurrentComponentIndex)
            if (component != null) {
                functionalProgramming.withIndent {
                    val requiredComponent = component.javaClass.kotlin.findAnnotation<RequiredComponent>()
                    val incorrectComponent = let {
                        requiredComponent?.component?.forEach {
                            if (!entity.getStateOrDefault(editorUI.entityCurrentStateIndex).hasComponent(it))
                                return@let true
                        }
                        false
                    }
                    if (!incorrectComponent)
                        ImGuiHelper.insertUIFields(component, entity, level, editorUI)
                    else {
                        text("Il manque le(s) component(s) :")
                        functionalProgramming.withIndent {
                            text("${requiredComponent!!.component.map { it.simpleName }}")
                        }
                    }
                }
                if (button("Supprimer ce comp.", Vec2(-1, 0))) {
                    entity.getStateOrDefault(editorUI.entityCurrentStateIndex).removeComponent(component)
                }
            }
            separator()
            pushItemFlag(ItemFlag.Disabled.i, entity.getStateOrDefault(editorUI.entityCurrentStateIndex).getComponents().size == Components.values().size)
            if (button("Ajouter un component", Vec2(-1, 0)))
                openPopup(addComponentTitle)
            popItemFlag()

            functionalProgramming.popup(addComponentTitle) {
                val components = Components.values().filter { comp -> entity.getStateOrDefault(editorUI.entityCurrentStateIndex).getComponents().none { comp.component.isInstance(it) } }
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("component", editorUI::entityAddComponentComboIndex, components.map { it.name })
                }

                if (editorUI.entityAddComponentComboIndex in components.indices) {
                    val componentClass = components[editorUI.entityAddComponentComboIndex].component

                    val description = componentClass.findAnnotation<Description>()
                    if (description != null) {
                        sameLine(0f, style.itemInnerSpacing.x)
                        text("(?)")

                        if (isItemHovered()) {
                            functionalProgramming.withTooltip {
                                text(description.description)
                            }
                        }
                    }

                    if (button("Ajouter", Vec2(-1, 0))) {
                        if (editorUI.entityAddComponentComboIndex in components.indices) {
                            val newComp = ReflectionUtility.createInstance(components[editorUI.entityAddComponentComboIndex].component.java)
                            val state = entity.getStateOrDefault(editorUI.entityCurrentStateIndex)

                            if (entity.getCurrentState() === state)
                                newComp.onStateActive(entity, state, level)

                            state.addComponent(newComp)
                            closeCurrentPopup()
                        }
                    }
                }
            }
        }
    }

    private fun drawInfoEntityWindow(entity: Entity) {
        val createPrefabTitle = "Créer un prefab"

        with(ImGui) {
            functionalProgramming.withWindow("Réglages de l'entité", editorUI::showInfoEntityWindow, WindowFlag.AlwaysAutoResize.i) {
                val favChecked = booleanArrayOf(level.favoris.contains(entity.id()))


                if (ImGuiHelper.favButton(tintColor = Vec4(1f, 1f, 1f, if (favChecked[0]) 1f else 0.2f))) {
                    if (!favChecked[0])
                        level.favoris.add(entity.id())
                    else
                        level.favoris.remove(entity.id())
                }

                if (isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text("Favoris")
                    }
                }

                sameLine(0f, style.itemInnerSpacing.x)

                if (button("Créer un prefab", Vec2(-1, 0)))
                    openPopup(createPrefabTitle)

                if (button("Supprimer cette entité", Vec2(-1, 0))) {
                    entity.removeFromParent()
                    if (selectEntity === entity) {
                        selectEntity = null
                        clearSelectEntities()
                        editorUI.editorMode = EditorUI.EditorMode.NO_MODE
                    }
                }

                drawInfoEntityProps(entity)

                functionalProgramming.popup(createPrefabTitle) {
                    ImGuiHelper.inputText("nom", editorUI::createPrefabInputText)

                    if (button("Ajouter", Vec2(-1, 0))) {
                        level.addPrefab(Prefab(editorUI.createPrefabInputText, SerializationFactory.copy(entity)))
                        closeCurrentPopup()
                    }
                }
            }
        }
    }

    private fun drawInfoEntityTextWindow(entity: Entity) {
        with(ImGui) {
            setNextWindowSizeConstraints(Vec2(), Vec2(500f, 500f))
            functionalProgramming.withWindow("Données de l'entité", editorUI::showInfoEntityTextWindow, WindowFlag.AlwaysAutoResize.i) {
                ImGuiHelper.insertUITextFields(entity)
                separator()

                val components = entity.getCurrentState().getComponents()
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    combo("component", editorUI::entityCurrentComponentIndex, components.map { ReflectionUtility.simpleNameOf(it).removeSuffix("Component") })
                }

                val component = components.elementAtOrNull(editorUI.entityCurrentComponentIndex)
                if (component != null) {
                    functionalProgramming.withIndent {
                        val requiredComponent = component.javaClass.kotlin.findAnnotation<RequiredComponent>()
                        val incorrectComponent = let {
                            requiredComponent?.component?.forEach {
                                if (!entity.getStateOrDefault(editorUI.entityCurrentStateIndex).hasComponent(it))
                                    return@let true
                            }
                            false
                        }
                        if (!incorrectComponent)
                            ImGuiHelper.insertUITextFields(component)
                        else {
                            ImGuiHelper.textColored(Color.RED, "Il manque le(s) component(s) :")
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