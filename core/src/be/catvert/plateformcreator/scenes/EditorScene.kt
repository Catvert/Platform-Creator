package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.*
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Signal
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.contains
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.plus
import ktx.app.use
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
        NoMode, SelectEntity, CopyEntity
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
     * Permet d'ajouter une nouvelle entité sélectionnée
     */
    private fun addSelectEntity(entity: Entity) {
        selectEntities += entity
        if (selectEntities.size == 1) {
            selectMoveEntity = entity
            onSelectEntityChanged.dispatch(entity)
        } else
            onSelectEntityChanged.dispatch(null)

        editorMode = EditorMode.SelectEntity
    }

    /**
     * Permet de supprimer les entités sélectionnées de la liste -> ils ne sont pas supprimés du niveau, juste déséléctionnés
     */
    private fun clearSelectEntities() {
        selectEntities.clear()
        selectMoveEntity = null
        onSelectEntityChanged.dispatch(null)

        editorMode = EditorMode.NoMode
    }

    /**
     * Signal appelé quand l'entité sélectionné change(UI)
     */
    private var onSelectEntityChanged: Signal<Entity?> = Signal()

    /**
     * Signal appelé quand l'entité sélectionné se déplace(UI)
     */
    private var onSelectEntityMoved: Signal<TransformComponent> = Signal()

    /**
     * L'entité à copier
     */
    private var copyEntity: Entity? = null
        set(value) {
            field = value

            if (value == null)
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
    private var latestLeftButtonClick = false

    /**
     * Est-ce que à la dernière frame, la bouton droit était pressé
     */
    private var latestRightButtonClick = false

    /**
     * Position de la souris au dernier frame
     */
    private var latestMousePos = Vector2()

    /**
     * Est-ce que l'utilisateur est sur l'interface
     */
    private var UIHover = false

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

    init {
        background = game.getBackground(level.backgroundPath).second

        showInfoEntityWindow()

        level.activeRect.setSize(Gdx.graphics.width.toFloat() * 3, Gdx.graphics.height.toFloat() * 3)
        level.followPlayerCamera = false
        level.drawDebugCells = true
        level.killEntityNegativeY = false

        stage.addListener(object : ClickListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                UIHover = true
                super.enter(event, x, y, pointer, fromActor)
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                UIHover = false
                super.exit(event, x, y, pointer, toActor)
            }
        }) // UI Hover
    }

    override fun dispose() {
        super.dispose()

        editorFont.dispose()
    }

    override fun render(delta: Float) {
        game.batch.projectionMatrix = game.defaultProjection
        game.batch.use { gameBatch ->
            editorFont.draw(gameBatch, "Layer sélectionné : $selectedLayer", 10f, Gdx.graphics.height - editorFont.lineHeight)
            editorFont.draw(gameBatch, "Nombre d'entités : ${level.loadedEntities.size}", 10f, Gdx.graphics.height - editorFont.lineHeight * 2)
        }

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        /**
         * Dessine les entités qui n'ont pas de renderComponent avec un rectangle noir
         */
        entities.filter { !renderMapper.has(it) }.forEach {
            shapeRenderer.color = Color.BLACK
            val rect = transformMapper[it].rectangle
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
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
        }
        shapeRenderer.end()

        super.render(delta)
    }

    override fun update(deltaTime: Float) {
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

        val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
        val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)
        camera.position.set(x, y, 0f)

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key)) {
            level.drawDebugCells = !level.drawDebugCells
        }

        camera.update()

        val mousePos = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val mousePosInWorld = camera.unproject(Vector3(mousePos, 0f))

        if (!UIHover) {
            when (editorMode) {
                EditorScene.EditorMode.NoMode -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (latestLeftButtonClick) { // Rectangle
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
                    } else if (latestLeftButtonClick && rectangleMode.rectangleStarted) { // left button released on this frame
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

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX_0.key) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX_1.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && renderMapper.has(entity)) {
                            renderMapper[entity].flipX = !renderMapper[entity].flipX
                        }
                    }
                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY_0.key) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY_1.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && renderMapper.has(entity)) {
                            renderMapper[entity].flipY = !renderMapper[entity].flipY
                        }
                    }
                }
                EditorScene.EditorMode.SelectEntity -> {
                    fun moveEntities(moveX: Float, moveY: Float) {
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
                                transform.rectangle.x += moveX
                                transform.rectangle.y += moveY
                                level.setEntityGrid(it)
                            }
                            if (selectEntities.size == 1 && selectMoveEntity != null)
                                onSelectEntityMoved.dispatch(transformMapper[selectMoveEntity])
                        }
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (!latestLeftButtonClick) {
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
                        } else if (selectMoveEntity != null && latestMousePos != mousePos) {
                            val transformMoveEntity = transformMapper[selectMoveEntity]

                            val moveX = transformMoveEntity.rectangle.x + transformMoveEntity.rectangle.width / 2 - mousePosInWorld.x
                            val moveY = transformMoveEntity.rectangle.y + transformMoveEntity.rectangle.height / 2 - mousePosInWorld.y

                            moveEntities(-moveX, -moveY)
                        }
                    } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                        clearSelectEntities()
                    } else {
                        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                            moveEntities(-1f, 0f)
                        }
                        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                            moveEntities(1f, 0f)
                        }
                        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                            moveEntities(0f, 1f)
                        }
                        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                            moveEntities(0f, -1f)
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
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && entity isNotType EntityFactory.EntityType.Player)
                            copyEntity = entity
                        else
                            copyEntity = null
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        val newEntity = entityFactory.copyEntity(copyEntity!!)
                        val transform = transformMapper[newEntity]

                        var posX = transform.rectangle.x
                        var posY = transform.rectangle.y

                        var moveToNextEntity = true

                        if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_LEFT.key)) {
                            posX -= transform.rectangle.width
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_RIGHT.key)) {
                            posX += transform.rectangle.width
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_DOWN.key)) {
                            posY -= transform.rectangle.height
                        } else if (Gdx.input.isKeyPressed(GameKeys.EDITOR_SMARTCOPY_UP.key)) {
                            posY += transform.rectangle.height
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
            }
        }

        latestLeftButtonClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
        latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
        latestMousePos = mousePos
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
     * Permet de retourner l'entité sous le pointeur et de mettre à jour les entités sous le pointeur(entitiesUnderMouse)
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
            else
                return entities.first()
        }

        return null
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     * @param onTextureSelected Méthode appelée quand la texture a été correctement sélectionnée par le joueur
     */
    private fun showSelectTextureWindow(onTextureSelected: (TextureInfo) -> Unit) {
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
                                            image.drawable = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(texture.texture)
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

                            return (width > 0 && width < maxEntitySize) && (height > 0 && height < maxEntitySize)
                        }

                        fun finishEntityBuild(entity: Entity) {
                            copyEntity = entity

                            this@window.remove()
                            UIHover = false
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
            setSize(250f, 400f)
            setPosition(Gdx.graphics.width - width, Gdx.graphics.height - height)
            verticalGroup {
                space(10f)

                textButton("Ajouter une entité") {
                    addListener(onClick { showAddEntityWindow() })
                }
                textButton("Supprimer l'entité sélectionnée") {
                    touchable = Touchable.disabled

                    onSelectEntityChanged.add { _, selectEntity ->
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
                    onSelectEntityChanged.add { _, selectEntity ->
                        if (selectEntity == null)
                            setText("Aucune entité sélectionnée")
                        else
                            setText(EntityFactory.EntityType.values().first { entityType -> selectEntity isType entityType }.name)
                    }
                }
                horizontalGroup {
                    label("Position X : ")
                    textField("") {
                        isReadOnly = true

                        onSelectEntityChanged.add { _, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.x.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }
                        onSelectEntityMoved.add { _, transform ->
                            text = transform.rectangle.x.toInt().toString()
                        }

                        addListener(onChange {
                            if (!isReadOnly) {
                                if (text.toIntOrNull() != null) {
                                    if (level.matrixRect.contains(text.toInt().toFloat(), 0f))
                                        transformMapper[selectEntities.elementAt(0)].rectangle.x = text.toInt().toFloat()
                                }
                            }
                        })
                    }
                }
                horizontalGroup {
                    label("Position Y : ")
                    textField("") {
                        isReadOnly = true

                        onSelectEntityChanged.add { _, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.y.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }
                        onSelectEntityMoved.add { _, transform ->
                            text = transform.rectangle.y.toInt().toString()
                        }

                        addListener(onChange {
                            if (!isReadOnly) {
                                if (text.toIntOrNull() != null) {
                                    if (level.matrixRect.contains(0f, text.toInt().toFloat()))
                                        transformMapper[selectEntities.elementAt(0)].rectangle.y = text.toInt().toFloat()
                                }
                            }
                        })
                    }
                }
                horizontalGroup {
                    label("Largeur : ")
                    textField("") {
                        isReadOnly = true

                        onSelectEntityChanged.add { _, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.width.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }

                        addListener(onChange {
                            if (!this.isReadOnly) {
                                val transform = transformMapper[selectEntities.elementAt(0)]
                                val width = text.toIntOrNull()
                                if (width != null && width > 0 && width <= maxEntitySize && !transform.fixedSizeEditor) {
                                    transform.rectangle.width = width.toFloat()
                                }
                            }
                        })
                    }
                }
                horizontalGroup {
                    label("Hauteur : ")
                    textField("") {
                        isReadOnly = true

                        onSelectEntityChanged.add { _, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.height.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }

                        addListener(onChange {
                            if (!this.isReadOnly) {
                                val transform = transformMapper[selectEntities.elementAt(0)]
                                val height = text.toIntOrNull()
                                if (height != null && height > 0 && height <= maxEntitySize && !transform.fixedSizeEditor) {
                                    transform.rectangle.height = height.toFloat()
                                }
                            }
                        })
                    }
                }

                horizontalGroup {
                    label("Layer : ")
                    selectBox<Layer> {
                        items = Layer.values().filterNot { it == Layer.LAYER_HUD }.toGdxArray()

                        touchable = Touchable.disabled

                        onSelectEntityChanged.add { _, selectEntity ->
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
}