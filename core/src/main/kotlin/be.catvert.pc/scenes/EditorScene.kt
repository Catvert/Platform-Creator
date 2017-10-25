package be.catvert.pc.scenes

import be.catvert.pc.*
import be.catvert.pc.components.RenderableComponent
import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Json
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.contains
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
import ktx.assets.loadOnDemand
import ktx.assets.toLocalFile
import ktx.collections.toGdxArray
import ktx.vis.window
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Scène de l'éditeur de niveau
 */
class EditorScene(private val level: Level) : Scene() {
    private enum class EditorMode {
        NO_MODE, SELECT_GO, COPY_GO
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

    override val gameObjectContainer: GameObjectContainer = level

    private val shapeRenderer = ShapeRenderer()

    private val editorFont = BitmapFont(Constants.editorFont.toLocalFile())

    private val cameraMoveSpeed = 10f

    private var editorMode = EditorMode.NO_MODE
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

    init {
        gameObjectContainer.allowUpdatingGO = false

        if (level.backgroundPath != null)
            backgroundTexture = PCGame.assetManager.loadOnDemand<Texture>(level.backgroundPath.toLocalFile().path()).asset

        level.removeEntityBelowY0 = false
        level.drawDebugCells = true

        showInfoEntityWindow()

        onSelectGameObjectChange(null)
    }

    override fun postBatchRender() {
        super.postBatchRender()

        level.drawDebug()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        if(!level.drawDebugCells) {
            shapeRenderer.line(0f, 0f, 10000f, 1f, Color.GOLD, Color.WHITE)
            shapeRenderer.line(0f, 0f, 1f, 10000f, Color.GOLD, Color.WHITE)
        }

        /**
         * Dessine les gameObjects qui n'ont pas de renderableComponent avec un rectangle noir
         */
        gameObjectContainer.getGameObjectsData().filter { it.active }.forEach { gameObject ->
            if (!gameObject.hasComponent<RenderableComponent>()) {
                shapeRenderer.color = Color.BLACK
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
                            shapeRenderer.color = Color.BLUE; shapeRenderer.circle(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat(), 10f)
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
                draw(it, "Layer sélectionné : $selectLayer", 10f, Gdx.graphics.height - editorFont.lineHeight)
                draw(it, "Nombre d'entités : ${level.getGameObjectsData().size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
                draw(it, "Resize mode : ${resizeGameObjectMode.name}", 10f, Gdx.graphics.height - editorFont.lineHeight * 3)
            }
            it.projectionMatrix = camera.combined
        }
    }

    override fun update() {
        super.update()

        level.activeRect.position = Point(Math.max(0, camera.position.x.toInt() - level.activeRect.width / 2), Math.max(0, camera.position.y.toInt() - level.activeRect.height / 2))

        if (!isUIHover()) {
            updateCamera()

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

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                        val gameObject = findGameObjectUnderMouse()
                        if (gameObject != null && gameObject.id != level.playerUUID) {
                            gameObject.removeFromParent()
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
                            if (selectGameObjects.contains(findGameObjectUnderMouse()))
                                TODO() //showContextMenuSelectEntities()
                            else
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

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)
                            && (selectGameObjects.size > 1 || findGameObjectUnderMouse() === selectGameObjects.elementAt(0))) {
                        selectGameObjects.forEach {
                            it.removeFromParent()
                        }
                        clearSelectGameObjects()
                    }
                }
                EditorMode.COPY_GO -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        selectGameObject = findGameObjectUnderMouse()
                        if(selectGameObject == null)
                            editorMode = EditorMode.NO_MODE
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick && selectGameObject != null) {
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
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
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

    //region UI

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
                    onClick {
                        try {
                            SerializationFactory.serializeToFile(level, level.levelPath.toLocalFile())
                            this@window.remove()
                        } catch (e: Exception) {
                            Log.error(e, message = { "Erreur lors de l'enregistrement du niveau !" })
                        }
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
    private fun showAddEntityWindow() {
        /*stage + window("Ajouter une entité") {
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

                        if (latestAddEntityWindowEntityTypeIndex < this.items.size)
                            this.selectedIndex = latestAddEntityWindowEntityTypeIndex

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
                                        showSelectTextureWindow(null, { texture ->
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
                            latestAddEntityWindowEntityTypeIndex = this.selectedIndex
                            this@window.remove()
                        }

                        val change = onChange {
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

                                this@verticalSettingsGroup.addActor(addButton)
                            }
                        }

                        addListener(change)

                        change.changed(ChangeListener.ChangeEvent(), this)
                    }
                }
            }
        }
        */
    }

    /**
     * Permet d'afficher la fenêtre comportant les informations de l'entité sélectionnée
     */
    private fun showInfoEntityWindow() {
        stage + window("Réglages des gameObjects") {
            setSize(250f, 275f)
            setPosition(Gdx.graphics.width - width, Gdx.graphics.height - height)
            verticalGroup {
                space(10f)

                textButton("Ajouter un gameObject") {
                    onClick { showAddEntityWindow() }
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
                            this.touchable = if(gameObject == null) Touchable.disabled else Touchable.enabled
                        }

                        onClick {
                            if(selectGameObject != null) {
                                selectGameObject!!.layer++
                                onSelectGameObjectChange(selectGameObject!!)
                            }
                        }
                    }
                    textButton("-") {
                        onSelectGameObjectChange.register { gameObject ->
                            this.touchable = if(gameObject == null) Touchable.disabled else Touchable.enabled
                        }

                        onClick {
                            if(selectGameObject != null) {
                                selectGameObject!!.layer--
                                onSelectGameObjectChange(selectGameObject!!)
                            }
                        }
                    }
                }

                textButton("Éditer") {
                    onSelectGameObjectChange.register { gameObject ->
                        this.touchable = if(gameObject == null) Touchable.disabled else Touchable.enabled
                    }

                    onClick {
                        val components = selectGameObject!!.getComponents()

                        components.forEach { comp ->
                            comp.javaClass.declaredFields.filter { it.isAnnotationPresent(ExposeEditor::class.java) }.forEach {

                            }
                        }
                    }
                }

                /*table {
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
                                showSelectTextureWindow(firstSpriteSheet) {
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
                */
            }
        }
    }

    //endregion
}