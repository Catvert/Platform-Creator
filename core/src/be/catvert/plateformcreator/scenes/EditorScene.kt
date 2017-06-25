package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.*
import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.ComponentMapper
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
class EditorScene(game: MtrGame, entityEvent: EntityEvent, private val level: Level) : BaseScene(game, entityEvent, systems = RenderingSystem(game)) {
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

    private val shapeRenderer = ShapeRenderer()

    private val transformMapper = mapperFor<TransformComponent>()
    private val renderMapper = mapperFor<RenderComponent>()

    private val cameraMoveSpeed = 10f
    private val maxEntitySize = 500f
    private val selectEntities = mutableSetOf<Entity>()
    private var selectMoveEntity: Entity? = null
    private val entityFactory = _game.entityFactory
    private fun addSelectEntity(entity: Entity) {
        selectEntities += entity
        if (selectEntities.size == 1) {
            selectMoveEntity = entity
            onSelectEntityChanged.dispatch(entity)
        } else
            onSelectEntityChanged.dispatch(null)

        editorMode = EditorMode.SelectEntity
    }

    private fun clearSelectEntities() {
        selectEntities.clear()
        selectMoveEntity = null
        onSelectEntityChanged.dispatch(null)
        editorMode = EditorMode.NoMode
    }

    private var onSelectEntityChanged: Signal<Entity?> = Signal()
    private var onSelectEntityMoved: Signal<TransformComponent> = Signal()
    private var copyEntity: Entity? = null
        set(value) {
            field = value

            if (value == null)
                editorMode = EditorMode.NoMode
            else
                editorMode = EditorMode.CopyEntity
        }
    private var deleteEntityAfterCopying = false
    private val rectangleMode = RectangleMode(Vector2(), Vector2())
    private var editorMode: EditorMode = EditorMode.NoMode
    private var latestLeftButtonClick = false
    private var latestRightButtonClick = false
    private var latestMousePos = Vector2()
    private var UIHover = false
    private val editorFont = BitmapFont(Gdx.files.internal("fonts/editorFont.fnt"))

    override val entities: MutableSet<Entity> = mutableSetOf()

    init {
        background = level.background

        showInfoEntityWindow()

        level.activeRect.setSize(Gdx.graphics.width.toFloat() * 3, Gdx.graphics.height.toFloat() * 3)
        level.followPlayerCamera = false
        level.drawDebugCells = true
        level.killEntityUnderY = false

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
        level.activeRect.setPosition(Math.max(0f, camera.position.x - level.activeRect.width / 2), Math.max(0f, camera.position.y - level.activeRect.height / 2))

        level.update(delta)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        when (editorMode) {
            EditorScene.EditorMode.NoMode -> {
                if (rectangleMode.rectangleStarted) {
                    shapeRenderer.color = Color.BLUE
                    val rect = rectangleMode.getRectangle()
                    shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
                }
            }
            EditorScene.EditorMode.SelectEntity -> {
                selectEntities.forEach {
                    shapeRenderer.color = Color.RED
                    if (it == selectMoveEntity) {
                        shapeRenderer.color = Color.CORAL
                    }
                    val transform = transformMapper[it]
                    shapeRenderer.rect(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width, transform.rectangle.height)
                }

                _game.batch.use { gameBatch ->
                    val transform = transformMapper[selectEntities.elementAt(0)]
                    editorFont.draw(gameBatch, "(${transform.rectangle.x.toInt()}, ${transform.rectangle.y.toInt()})", transform.rectangle.x, transform.rectangle.y + transform.rectangle.height + 20)
                }
            }
            EditorScene.EditorMode.CopyEntity -> {
                if (copyEntity != null) {
                    val transform = transformMapper[copyEntity]
                    shapeRenderer.color = Color.GREEN
                    shapeRenderer.rect(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width, transform.rectangle.height)
                }
            }
        }

        shapeRenderer.end()

        super.render(delta)
    }

    override fun updateInputs() {
        super.updateInputs()

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

        val x = MathUtils.lerp(camera.position.x, camera.position.x + moveCameraX, 0.5f)
        val y = MathUtils.lerp(camera.position.y, camera.position.y + moveCameraY, 0.5f)
        camera.position.set(x, y, 0f)

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key)) {
            level.drawDebugCells = !level.drawDebugCells
        }

        camera.update()

        val mousePos = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val mousePosInWorld = camera.unproject(Vector3(mousePos, 0f))
        val latestMousePosInWorld = camera.unproject(Vector3(latestMousePos, 0f))

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
                        if (entity != null && entity.flags != EntityFactory.EntityType.Player.flag) {
                            copyEntity = entity
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null) {
                            removeEntityFromLevel(entity)
                        }
                    }

                    if(Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX_0.key) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPX_1.key)) {
                        val entity = findEntityUnderMouse()
                        if(entity != null && renderMapper.has(entity)) {
                            renderMapper[entity].flipX = !renderMapper[entity].flipX
                        }
                    }
                    if(Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY_0.key) || Gdx.input.isKeyJustPressed(GameKeys.EDITOR_FLIPY_1.key)) {
                        val entity = findEntityUnderMouse()
                        if(entity != null && renderMapper.has(entity)) {
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

                    if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_REMOVE_ENTITY.key)) {
                        selectEntities.forEach {
                            removeEntityFromLevel(it)
                        }
                        clearSelectEntities()
                    }
                }
                EditorScene.EditorMode.CopyEntity -> {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        val entity = findEntityUnderMouse()
                        if (entity?.flags != EntityFactory.EntityType.Player.flag) {
                            copyEntity = entity
                        }
                    }
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !latestRightButtonClick) {
                        val newEntity = copyEntity!!.copy(entityFactory, _entityEvent)
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

                        if (deleteEntityAfterCopying) {
                            removeEntityFromLevel(copyEntity!!)
                            copyEntity = null
                            deleteEntityAfterCopying = false
                        }
                    } else if (Gdx.input.isKeyJustPressed(GameKeys.EDITOR_COPY_MODE.key)) {
                        copyEntity = null
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
     */
    private fun addEntityToLevel(entity: Entity) {
        level.loadedEntities += entity
        level.addEntity(entity)
    }

    /**
     * Permet de supprimer une entité du niveau
     */
    private fun removeEntityFromLevel(entity: Entity) {
        if (entity.flags != EntityFactory.EntityType.Player.flag) {
            level.loadedEntities -= entity
            level.removeEntity(entity)
        }
    }

    /**
     * Permet de retourner l'entité sous le pointeur
     */
    private fun findEntityUnderMouse(): Entity? {
        stage.keyboardFocus = null // Enlève le focus sur la fenêtre active permettant d'utiliser par exemple les touches de déplacement même si le joueur était dans un textField l'étape avant

        val mousePosInWorld = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

        entities.forEach {
            val transform = transformMapper[it]
            if (transform.rectangle.contains(mousePosInWorld.x, mousePosInWorld.y)) {
                return it
            }
        }
        return null
    }

    /**
     * Permet d'afficher la fenêtre pour sélectionner une texture
     */
    private fun showSelectTextureWindow(onTextureSelected: (TextureInfo) -> Unit) {
        data class TextureAtlasSelect(val textureAtlas: TextureAtlas, val atlasName: String) {
            override fun toString(): String {
                return atlasName
            }
        }

        stage.addActor(window("Sélectionner une texture") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
            addCloseButton()

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                selectBox<TextureAtlasSelect> {
                    items = _game.getTextureAtlasList().let {
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
        })
    }

    /**
     * Permet d'afficher la fenêtre pour créer une entité
     */
    private fun showAddEntityWindow() {
        stage.addActor(window("Ajouter une entité") {
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
                            addEntityToLevel(entity)
                            copyEntity = entity

                            deleteEntityAfterCopying = true

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
                                                finishEntityBuild(entityFactory.createSprite(TransformComponent(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat())), renderComponent { textures, _ -> textures += selectedTexture!! }))
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
                                                finishEntityBuild(entityFactory.createPhysicsSprite(TransformComponent(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat())), renderComponent { textures, _ -> textures += selectedTexture!! }, PhysicsComponent(true)))
                                            }
                                        })

                                    }
                                    EntityFactory.EntityType.Enemy -> {
                                        val enemyType = this@verticalSettingsGroup.selectBox<EnemyType> {
                                            items = EnemyType.values().toGdxArray()
                                        }

                                        addButton.addListener(addButton.onClick {
                                            finishEntityBuild(entityFactory.createEnemyWithType(enemyType.selected, _entityEvent, Point(0, 0)))
                                        })
                                    }
                                    EntityFactory.EntityType.Player -> {
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
        })
    }

    /**
     * Permet d'afficher la fenêtre comportant les informations de l'entité sélectionnée
     */
    private fun showInfoEntityWindow() {
        stage.addActor(window("Réglages des entités") {
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
                                if (selectEntity == null || selectEntity.flags == EntityFactory.EntityType.Player.flag)
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
                            setText(EntityFactory.EntityType.values().first { entityType -> selectEntity.flags == entityType.flag }.name)
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
                            if (selectEntity == null) {
                                touchable = Touchable.disabled
                                selectedIndex = 0
                            } else {
                                touchable = Touchable.enabled
                                selected = selectEntity[RenderComponent::class.java].renderLayer
                            }
                        }

                        addListener(onChange {
                            if (this.isTouchable) {
                                val selectEntity = selectEntities.elementAt(0)
                                selectEntity[RenderComponent::class.java].renderLayer = selected
                            }
                        })
                    }
                }
            }
        })
    }

    /**
     * Permet d'afficher la fenêtre permettant de sauvegarder et quitter l'éditeur
     */
    private fun showExitWindow() {
        stage.addActor(window("Quitter") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            addCloseButton()

            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    addListener(onClick {
                        try {
                            LevelFactory(_game).saveLevel(level)
                            this@window.remove()
                        } catch(e: Exception) {
                            ktx.log.error(e, message = { "Erreur lors de l'enregistrement du niveau !" })
                        }
                    })
                }
                textButton("Quitter") {
                    addListener(onClick { _game.setScene(MainMenuScene(_game)) })
                }
            }
        })
    }
}