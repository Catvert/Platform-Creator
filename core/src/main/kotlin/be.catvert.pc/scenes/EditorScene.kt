package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.components.Component
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.components.graphics.AnimationComponent
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
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
import ktx.vis.window
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(private val level: Level) : Scene() {
    private enum class EditorMode {
        NO_MODE, SELECT_GO, COPY_GO, TRY_LEVEL
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
        set(value) {
            field = value
            onEditorModeChange(value)
        }
    private var selectGameObjectMode = SelectGOMode.NO_MODE
    private var resizeGameObjectMode = ResizeMode.PROPORTIONAL

    private val selectGameObjects = mutableListOf<GameObject>()
    private var selectGameObject: GameObject? = null

    private val onSelectGameObjectChange = Signal<GameObject?>()

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

    private val maxGameObjectSize = 500

    /**
     * Permet de sauvegarder le dernier spriteSheet utilisé dans la fenêtre de sélection d'atlas
     */
    private var latestSelectTextureWindowAtlasIndex = 0

    private var backupTryModeCameraPos = Vector3()

    private val onEditorModeChange = Signal<EditorMode>()

    init {
        gameObjectContainer.allowUpdatingGO = false

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
            if (!gameObject.hasComponent<RenderableComponent>()) {
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

                if (selectGameObject != null && selectGameObject?.fixedSize == false) {
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
                /**
                 * Dessine un rectangle autour du gameObject à copier
                 * Vérifie si le gameObject se trouve bien dans le niveau, dans le cas contraire,
                 * ça signifie que le gameObject vient d'être créer et qu'il ne faut donc pas afficher un rectangle car il n'est pas encore dans le niveau
                 */
                if (selectGameObject != null && level.getGameObjectsData().contains(selectGameObject!!)) {
                    shapeRenderer.color = Color.GREEN
                    shapeRenderer.rect(selectGameObject!!.rectangle)
                }
            }
            EditorMode.TRY_LEVEL -> {
            }
        /*
        EditorScene.EditorMode.SelectPoint -> {
            game.batch.projectionMatrix = game.defaultProjection
            game.batch.use { gameBatch ->
                val layout = GlyphLayout(game.mainFont, "Sélectionner un point")
                game.mainFont.draw(gameBatch, layout, Gdx.graphics.width / 2f - layout.width / 2, Gdx.graphics.height - game.mainFont.lineHeight)
            }
        }*/ //TODO
        }
        shapeRenderer.end()

        PCGame.mainBatch.use {
            it.projectionMatrix = PCGame.defaultProjection
            with(editorFont) {
                if (editorMode != EditorMode.TRY_LEVEL) {
                    draw(it, "Layer sélectionné : $selectLayer", 10f, Gdx.graphics.height - editorFont.lineHeight)
                    draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
                    draw(it, "Resize mode : ${resizeGameObjectMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 3)
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
                if (resizeGameObjectMode == ResizeMode.PROPORTIONAL) resizeGameObjectMode = ResizeMode.FREE else resizeGameObjectMode = ResizeMode.PROPORTIONAL
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
                            if (gameObject != null)
                                addSelectGameObject(gameObject)
                            else { // Maybe rectangle
                                selectRectangleData.rectangleStarted = true
                                selectRectangleData.startPosition = mousePosInWorld
                                selectRectangleData.endPosition = selectRectangleData.startPosition
                            }
                        }
                    } else if (latestLeftButtonClick && selectRectangleData.rectangleStarted) { // Bouton gauche de la souris relaché pendant cette frame
                        selectRectangleData.rectangleStarted = false

                        level.getAllGameObjectsInRect(selectRectangleData.getRect(), false).forEach {
                            addSelectGameObject(it)
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        if (gameObject != null && !gameObject.unique) {
                            selectGameObject = gameObject
                            editorMode = EditorMode.COPY_GO
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        gameObject?.inverseFlipRenderable(true, false)
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        gameObject?.inverseFlipRenderable(true, false)
                    }
                }
                EditorMode.SELECT_GO -> {
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
                            if (selectGameObjects.size == 1 && selectGameObject != null)
                                onSelectGameObjectChange(selectGameObject) // TODO utilise le signal du rect
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

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (!latestLeftButtonClick) {
                            selectGameObjectMode = SelectGOMode.NO_MODE

                            if (checkCircleResize()) {
                                selectGameObjectMode = SelectGOMode.RESIZE
                            } else {
                                val gameObject = findGameObjectUnderMouse()
                                if (gameObject == null) { // Se produit lorsque le joueur clique dans le vide, dans ce cas on désélectionne les gameObjects sélectionnés
                                    clearSelectGameObjects()
                                } else if (selectGameObjects.isEmpty() || Gdx.input.isKeyPressed(GameKeys.EDITOR_APPEND_SELECT_ENTITIES.key)) {
                                    addSelectGameObject(gameObject)
                                } else if (selectGameObjects.contains(gameObject)) { // Dans ce cas-ci, le joueur à sélectionné un autre gameObject dans ceux qui sont sélectionnés
                                    selectGameObject = gameObject
                                } else { // Dans ce cas-ci, il y a un groupe de gameObject sélectionné ou aucun et le joueur en sélectionne un nouveau ou un en dehors de la sélection
                                    clearSelectGameObjects()
                                    addSelectGameObject(gameObject)
                                }
                            }
                        } else if (selectGameObject != null && latestMousePos != mousePos) { // Le joueur maintient le clique gauche durant plusieurs frames et a bougé la souris {
                            if (selectGameObjectMode == SelectGOMode.NO_MODE)
                                selectGameObjectMode = SelectGOMode.MOVE

                            val selectGORect = selectGameObject!!.rectangle
                            when (selectGameObjectMode) {
                                SelectGOMode.NO_MODE -> {
                                } // Ne devrait jamais ce produire
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
                                            if (it.tag == selectGameObject!!.tag && !it.fixedSize) {
                                                val newSizeX = it.rectangle.width - resizeX
                                                val newSizeY = it.rectangle.height - resizeY

                                                if (newSizeX in 1..maxGameObjectSize)
                                                    it.rectangle.width = newSizeX
                                                if (newSizeY in 1..maxGameObjectSize)
                                                    it.rectangle.height = newSizeY
                                            }
                                        }

                                        if (selectGameObjects.size == 1 && selectGameObject != null)
                                            onSelectGameObjectChange(selectGameObject)
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
                    }
                }
                EditorMode.COPY_GO -> {
                    if (selectGameObject == null) {
                        editorMode = EditorMode.NO_MODE
                        return
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        selectGameObject = findGameObjectUnderMouse()
                        if (selectGameObject == null)
                            editorMode = EditorMode.NO_MODE
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        val copyGameObject = SerializationFactory.copy(selectGameObject!!)

                        var posX = copyGameObject.rectangle.x
                        var posY = copyGameObject.rectangle.y

                        val width = copyGameObject.rectangle.width
                        val height = copyGameObject.rectangle.height

                        var moveToNextEntity = true

                        when {
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_LEFT.key) -> posX -= width
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_RIGHT.key) -> posX += width
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_DOWN.key) -> posY -= height
                            Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_UP.key) -> posY += height
                            else -> {
                                posX = Math.min(level.matrixRect.width - width, // Les min et max permettent de rester dans le cadre de la matrix
                                        Math.max(0, mousePosInWorld.x - width / 2))
                                posY = Math.min(level.matrixRect.height - height,
                                        Math.max(0, mousePosInWorld.y - height / 2))

                                // Permet de vérifier si le gameObject copié est nouveau ou pas (si il est nouveau, ça veut dire qu'il n'a pas encore de container)è
                                if (selectGameObject!!.container != null)
                                    moveToNextEntity = false
                            }
                        }

                        copyGameObject.rectangle.position = Point(posX, posY)

                        level.addGameObject(copyGameObject)

                        if (moveToNextEntity)
                            selectGameObject = copyGameObject
                    }
                }
            /*EditorScene.EditorMode.SelectPoint -> {
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !leftButtonPressedLastFrame) {
                    onSelectPoint.dispatch(Point(mousePosInWorld.x.toInt(), mousePosInWorld.y.toInt()))
                    editorMode = EditorMode.NoMode
                }
            }*/ // TODO
            }

            latestLeftButtonClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
            latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
            latestMousePos = mousePos

            if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                val gameObject = findGameObjectUnderMouse()
                if (gameObject != null) {
                    removeGameObject(gameObject)
                } else if (selectGameObjects.isNotEmpty()) {
                    selectGameObjects.forEach { removeGameObject(it) }
                }
                selectGameObjects.removeAll { it.isRemoving }
            }
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
            onSelectGameObjectChange(gameObject)
        } else
            onSelectGameObjectChange(null)

        editorMode = EditorMode.SELECT_GO
    }

    /**
     * Permet de supprimer les entités sélectionnées de la liste -> ils ne sont pas supprimés du niveau, juste déséléctionnés
     */
    private fun clearSelectGameObjects() {
        selectGameObjects.clear()
        selectGameObject = null
        onSelectGameObjectChange(null)

        editorMode = EditorMode.NO_MODE
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

        gameObjectContainer = SerializationFactory.copy(level)

        backupTryModeCameraPos = camera.position.cpy()

        if (level.playerUUID != null) {
            gameObjectContainer.findGameObjectByID(level.playerUUID!!)?.onRemoveAction = object : Action {
                override fun perform(gameObject: GameObject) {
                    finishTryLevel()
                }
            }
        }
    }

    private fun finishTryLevel() {
        editorMode = EditorMode.NO_MODE
        gameObjectContainer = level
        camera.position.set(backupTryModeCameraPos)
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

    /**
     * Permet d'afficher la fenêtre pour créer une entité
     */
    private fun showAddGameObjectWindow() {
        stage + window("Ajouter un gameObject") {
            isModal = true
            addCloseButton()

            fun finishBuild(gameObject: GameObject) {
                selectGameObject = gameObject
                editorMode = EditorMode.COPY_GO
                this.remove()
            }

            verticalGroup {
                space(10f)
                verticalGroup {
                    space(10f)

                    val prefabSelectBox = selectBox<PrefabFactory> {
                        items = PrefabFactory.values().toGdxArray().let {
                            it.removeValue(PrefabFactory.Player, false)
                            it
                        }
                    }

                    textButton("Ajouter") {
                        onClick {
                            val go = prefabSelectBox.selected.generate().createWithoutContainer(Point())
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

    fun addWidgetForField(parent: Table, field: Field, instance: Any, exposeEditor: ExposeEditor?) {

        fun setValue(value: Any) {
            field.set(instance, value)
        }

        fun <T : Any> getValueOrAssign(assign: T): T {
            var value = field.get(instance) as? T
            if (value == null) {
                value = assign
                setValue(assign)
            }
            return value
        }

        when (field.type) {
            Boolean::class.java -> {
                val checkbox = VisCheckBox("", getValueOrAssign(true)).apply {
                    onChange {
                        setValue(isChecked)
                    }
                }
                parent.add(checkbox)
            }
            Int::class.java -> {
                if (exposeEditor?.customType == CustomType.KEY_INT) {
                    val keyArea = VisTextArea(Input.Keys.toString(getValueOrAssign(0))).apply {
                        isReadOnly = true
                        onKeyUp {
                            val keyStr = Input.Keys.toString(it)
                            val key = Input.Keys.valueOf(keyStr)

                            if (key != -1) {
                                text = keyStr
                                setValue(key)
                            }
                        }
                    }
                    parent.add(keyArea).width(50f)
                } else {
                    val intModel = IntSpinnerModel(getValueOrAssign(0), exposeEditor?.minInt ?: 0, exposeEditor?.maxInt ?: 100)
                    val spinner = Spinner("", intModel).apply {
                        onChange {
                            setValue(intModel.value)
                        }
                    }
                    parent.add(spinner)
                }
            }
            String::class.java -> {
                val textField = VisTextField(getValueOrAssign("")).apply {
                    onChange {
                        setValue(text)
                    }
                }
                parent.add(textField)
            }
            Array<Action>::class.java -> {

            }
        }

        if (field.type.isEnum) {
            val selectBox = VisSelectBox<String>().apply {
                val enumConstants = field.type.enumConstants

                this.items = enumConstants.map { (it as Enum<*>).name }.toGdxArray()

                val index = enumConstants.indexOfFirst { it == getValueOrAssign(enumConstants[0]) }
                selectedIndex = if (index == -1) 0 else index

                onChange {
                    setValue(enumConstants[selectedIndex])
                }
            }

            parent.add(selectBox)
        }

        if (field.type.isAssignableFrom(Action::class.java)) {
            val editBtn = VisTextButton("Éditer").apply {
                onClick {
                    showEditActionWindow(getValueOrAssign(EmptyAction()), { setValue(it) })
                }
            }

            parent.add(editBtn)
        }
    }

    private fun showEditActionWindow(action: Action, setAction: (Action) -> Unit) {
        stage + window("Éditer l'action") editActionWindow@ {
            setSize(300f, 250f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            verticalGroup vgroup@ {
                space(10f)

                selectBox<String> {
                    var instance = action

                    val actionsList = gdxArrayOf<KClass<out Action>>()
                    FastClasspathScanner(Action::class.java.`package`.name).matchClassesImplementing(Action::class.java, { actionsList.add(it.kotlin) }).scan()

                    actionsList.removeAll { it.isAbstract}

                    this.items = actionsList.map { it.simpleName ?: "Nom inconnu" }.toGdxArray()

                    val findIndex = actionsList.indexOfFirst { it.isInstance(instance) }

                    this.selectedIndex = if (findIndex == -1) 0 else findIndex

                    onChange {
                        if (!actionsList[selectedIndex].isInstance(instance))
                            instance = Utility.findNoArgConstructor(actionsList[selectedIndex])!!.newInstance()

                        val constructorsParamsValue = mutableMapOf<Int, Any>()

                        this@vgroup.clear();
                        this@vgroup.addActor(this@selectBox)

                        this@vgroup.table(defaultSpacing = true) {
                            instance.javaClass.declaredFields.filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEachIndexed { index, field ->
                                field.isAccessible = true

                                label(field.name ?: "Nom introuvable")

                                addWidgetForField(this, field, instance, field.getAnnotation(ExposeEditor::class.java))

                                row()
                            }

                            row()

                            this@vgroup.textButton("Modifier") {
                                onClick {
                                    setAction(instance)
                                    this@editActionWindow.remove();
                                }
                            }
                        }
                    }.changed(ChangeListener.ChangeEvent(), this)
                }
            }
        }
    }

    private fun showAddComponentWindow(onCreateComp: (Component) -> Unit) {
        stage + window("Ajouter un component") editActionWindow@ {
            setSize(300f, 250f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table(defaultSpacing = true) {
                selectBox<String> {
                    val componentsList = gdxArrayOf<KClass<out Component>>()
                    FastClasspathScanner(Component::class.java.`package`.name).matchSubclassesOf(Component::class.java, { componentsList.add(it.kotlin) }).scan()

                    componentsList.removeAll { it.isAbstract || !Utility.hasNoArgConstructor(it) }

                    this.items = componentsList.map { it.simpleName ?: "Nom inconnu" }.toGdxArray()

                    onChange {
                        val comp = componentsList[selectedIndex]

                        val constructorsParamsValue = mutableMapOf<Int, Any>()

                        this@table.clear();
                        this@table.add(this@selectBox)
                        this@table.row()

                        val parametersTable = VisTable()

                        val scroll = ScrollPane(parametersTable)
                        this@table.add(scroll).size(300f, 150f)

                        if (comp.companionObjectInstance is CustomEditorImpl<*>) {
                            (comp.companionObjectInstance as CustomEditorImpl<in Component>).createInstance(parametersTable, this@EditorScene) {
                                onCreateComp(it as Component)
                                this@editActionWindow.remove()
                            }
                        } else {
                            val instance = Utility.findNoArgConstructor(comp)!!.newInstance()

                            instance.javaClass.declaredFields.filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
                                field.isAccessible = true

                                parametersTable.add(VisLabel(field.name))

                                addWidgetForField(parametersTable, field, instance, field.getAnnotation(ExposeEditor::class.java))

                                parametersTable.row()
                            }

                            this@table.row()
                            this@table.add(VisTextButton("Ajouter").apply {
                                onClick {
                                    onCreateComp(instance)
                                    this@editActionWindow.remove();
                                }
                            })

                        }

                    }.changed(ChangeListener.ChangeEvent(), this)
                }
            }
        }
    }

    private fun showEditGameObjectWindow(gameObject: GameObject) {
        stage + window("Éditer un gameObject") {
            setSize(250f, 450f)
            isModal = true
            addCloseButton()

            verticalGroup {
                space(10f)

                val onAddComponent = Signal<Component>()

                table(defaultSpacing = true) {

                    val posXIntModel = IntSpinnerModel(gameObject.position().x, 0, level.matrixRect.width - gameObject.size().width)
                    spinner("Pos X", posXIntModel) {
                        onChange {
                            gameObject.rectangle.x = posXIntModel.value
                        }
                    }

                    row()

                    val posYIntModel = IntSpinnerModel(gameObject.position().y, 0, level.matrixRect.height - gameObject.size().height)
                    spinner("Pos Y", posYIntModel) {
                        onChange {
                            gameObject.rectangle.y = posYIntModel.value
                        }
                    }

                    row()

                    val widthIntModel = IntSpinnerModel(gameObject.size().width, 1, maxGameObjectSize)
                    spinner("Largeur", widthIntModel) {
                        onChange {
                            gameObject.rectangle.width = widthIntModel.value
                        }
                    }

                    row()

                    val heightIntModel = IntSpinnerModel(gameObject.size().height, 1, maxGameObjectSize)
                    spinner("Hauteur", heightIntModel) {
                        onChange {
                            gameObject.rectangle.height = heightIntModel.value
                        }
                    }

                    row()

                    textButton("Ajouter un component") {
                        onClick {
                            showAddComponentWindow {
                                gameObject.addComponent(it)
                                onAddComponent(it)
                            }
                        }
                    }
                }

                table(defaultSpacing = true) {
                    fun generateComponentsItems() = gameObject.getComponents().mapIndexed { index, component -> "${index + 1} : " + component.javaClass.simpleName }.toGdxArray()

                    selectBox<String> {
                        this.items = generateComponentsItems()

                        onAddComponent.register { this.items = generateComponentsItems() }

                        onChange {
                            this@table.clear()
                            this@table.add(this@selectBox)

                            this@table.row()

                            if (selectedIndex == -1)
                                return@onChange

                            val removeBtn = VisTextButton("Supprimer").apply {
                                onClick {
                                    if (selectedIndex > -1) {
                                        gameObject.removeComponent(gameObject.getComponents().elementAt(selectedIndex))
                                        this@selectBox.items = generateComponentsItems()
                                    }
                                }
                            }

                            this@table.add(removeBtn)

                            this@table.row()

                            val instance = gameObject.getComponents().elementAt(selectedIndex)

                            val compPropertiesTable = VisTable().apply { setSize(250f, 500f); }

                            val scroll = ScrollPane(compPropertiesTable)
                            this@table.add(scroll).size(250f, 150f)

                            if (instance.javaClass.kotlin.companionObjectInstance is CustomEditorImpl<*>) {
                                (instance.javaClass.kotlin.companionObjectInstance!! as CustomEditorImpl<in Component>).insertChangeProperties(compPropertiesTable, this@EditorScene, instance)
                            } else {

                                instance.javaClass.declaredFields.filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach { field ->
                                    field.isAccessible = true

                                    compPropertiesTable.add(VisLabel(field.name))

                                    addWidgetForField(compPropertiesTable, field, instance, field.getAnnotation(ExposeEditor::class.java))

                                    compPropertiesTable.row()
                                }

                            }
                        }.changed(ChangeListener.ChangeEvent(), this)
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
                            val userObject = selectedImage.userObject as Pair<FileHandle, String>
                            onAtlasSelected(userObject.first, userObject.second)
                            latestSelectTextureWindowAtlasIndex = selectedBox.selectedIndex
                            this@window.remove()
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     * @param onAtlasSelected Méthode appelée quand l'atlasPath et la region ont été correctement sélectionnés par le joueur
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
                            val userObject = selectedImage.userObject as Pair<FileHandle, String>
                            onAnimationSelected(userObject.first, userObject.second, floatModel.value.toFloat())
                            latestSelectTextureWindowAtlasIndex = selectedBox.selectedIndex
                            this@window.remove()
                        }
                    }
                }
            }
        }
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     * @param onAtlasSelected Méthode appelée quand l'atlasPath et la region ont été correctement sélectionnés par le joueur
     */
    fun showSelectTextureWindow(onTextureSelected: (textureFile: FileHandle) -> Unit) {
        /**
         * Classe de donnée représentant la sélection d'une texture atlasPath
         */
        data class TextureSelect(val texture: Texture, val textureName: String) {
            override fun toString(): String {
                return textureName
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
            setSize(250f, 275f)
            setPosition(Gdx.graphics.width - width, Gdx.graphics.height - height)

            onEditorModeChange.register {
                isVisible = it != EditorMode.TRY_LEVEL
            }

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
                        editorMode = EditorMode.NO_MODE
                        clearSelectGameObjects()
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