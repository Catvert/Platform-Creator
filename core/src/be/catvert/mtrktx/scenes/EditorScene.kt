package be.catvert.mtrktx.scenes

import be.catvert.mtrktx.*
import be.catvert.mtrktx.ecs.EntityEvent
import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.components.EnemyType
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.systems.RenderingSystem
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.signals.Signal
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.*
import ktx.actors.contains
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.app.clearScreen
import ktx.app.use
import ktx.collections.toGdxArray
import ktx.vis.KVisTextButton
import ktx.vis.window

/**
 * Created by arno on 10/06/17.
 */

class EditorScene(game: MtrGame, entityEvent: EntityEvent, private val level: Level) : BaseScene(game, entityEvent, systems = RenderingSystem(game)) {
    private enum class EditorMode {
        NoMode, SelectEntity, CopyEntity
    }

    private data class RectangleMode(var startPosition: Vector2, var endPosition: Vector2, var rectangleStarted: Boolean = false) {
        fun getRectangle(): Rectangle {
            val minX = Math.min(startPosition.x, endPosition.x)
            val minY = Math.min(startPosition.y, endPosition.y)
            val maxX = Math.max(startPosition.x, endPosition.x)
            val maxY = Math.max(startPosition.y, endPosition.y)

            return Rectangle(minX, minY, maxX - minX, maxY - minY)
        }
    }

    override val entities: MutableList<Entity> = mutableListOf()

    private val shapeRenderer = ShapeRenderer()

    private val transformMapper = ComponentMapper.getFor(TransformComponent::class.java)
    private val physicsMapper = ComponentMapper.getFor(PhysicsComponent::class.java)

    private val cameraMoveSpeed = 10f

    private val maxEntitySize = 500f

    private val selectEntities: MutableSet<Entity> = mutableSetOf()

    private fun addSelectEntity(entity: Entity) {
        selectEntities += entity
        if (selectEntities.size == 1) {
            onSelectEntityChanged.dispatch(entity)
        } else
            onSelectEntityChanged.dispatch(null)

        editorMode = EditorMode.SelectEntity
    }

    private fun clearSelectEntities() {
        selectEntities.clear()
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

    init {
        showInfoEntityWindow()

        level.activeRect.setSize(Gdx.graphics.width.toFloat() * 4, Gdx.graphics.height.toFloat() * 4)
        level.followPlayerCamera = false
        level.drawDebugCells = true
        level.killEntityUnderY = false

        _stage.addListener(object : ClickListener() {
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

    override fun render(delta: Float) {
        clearScreen(186f / 255f, 212f / 255f, 1f)

        _game.batch.projectionMatrix = _game.defaultProjection
        _game.batch.use {
            _game.batch.draw(level.background.second.texture.second, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }
        _game.batch.projectionMatrix = level.camera.combined

        level.activeRect.setPosition(Math.max(0f, level.camera.position.x - level.activeRect.width / 2), Math.max(0f, level.camera.position.y - level.activeRect.height / 2))

        level.update(delta)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        shapeRenderer.projectionMatrix = level.camera.combined
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
                shapeRenderer.color = Color.RED
                selectEntities.forEach {
                    val transform = transformMapper[it]
                    shapeRenderer.rect(transform.rectangle.x, transform.rectangle.y, transform.rectangle.width, transform.rectangle.height)
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

        if (Gdx.input.isKeyPressed(Input.Keys.P)) {
            level.camera.zoom -= 0.02f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.M)) {
            level.camera.zoom += 0.02f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.L)) {
            level.camera.zoom = 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            level.camera.position.x -= cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            level.camera.position.x += cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            level.camera.position.y -= cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
            level.camera.position.y += cameraMoveSpeed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            level.drawDebugCells = !level.drawDebugCells
        }

        level.camera.update()

        val mousePos = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val mousePosInWorld = level.camera.unproject(Vector3(mousePos, 0f))

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
                                rectangleMode.startPosition = Vector2(mousePosInWorld.x, mousePosInWorld.y)
                                rectangleMode.endPosition = rectangleMode.startPosition
                            }
                        }
                    } else if (latestLeftButtonClick && rectangleMode.rectangleStarted) { // left button released on this frame
                        rectangleMode.rectangleStarted = false

                        level.getAllEntitiesInRect(rectangleMode.getRectangle(), false).forEach {
                            addSelectEntity(it)
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null && entity.flags != EntityFactory.EntityType.Player.flag) {
                            copyEntity = entity
                        }
                    }

                    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                        val entity = findEntityUnderMouse()
                        if (entity != null) {
                            removeEntityFromLevel(entity)
                        }
                    }
                }
                EditorScene.EditorMode.SelectEntity -> {
                    fun moveEntities(moveX: Float, moveY: Float) {
                        if (selectEntities.let {
                            var result = true
                            it.forEach {
                                val transform = transformMapper[it]
                                if (!level.matrixRect.contains(Rectangle(transform.rectangle.x + moveX, transform.rectangle.y + moveY, transform.rectangle.width, transform.rectangle.height))) {
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
                            if (selectEntities.size == 1)
                                onSelectEntityMoved.dispatch(transformMapper[selectEntities.elementAt(0)])
                        }
                    }

                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        if (!latestLeftButtonClick) {
                            val entity = findEntityUnderMouse()
                            if (entity == null) {
                                clearSelectEntities()
                            } else if (selectEntities.isEmpty() || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                                addSelectEntity(entity)
                            } else if (!selectEntities.contains(entity)) {
                                clearSelectEntities()
                                addSelectEntity(entity)
                            }
                        } else if (mousePos != latestMousePos) {
                            val lastPosMouseInWorld = level.camera.unproject(Vector3(latestMousePos.x, latestMousePos.y, 0f))

                            val moveX = lastPosMouseInWorld.x - mousePosInWorld.x
                            val moveY = lastPosMouseInWorld.y - mousePosInWorld.y

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

                    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
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
                        val newEntity = copyEntity!!.copy()
                        val transform = transformMapper[newEntity]

                        var posX = transform.rectangle.x
                        var posY = transform.rectangle.y

                        var moveToNextEntity = true

                        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                            posX -= transform.rectangle.width
                        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                            posX += transform.rectangle.width
                        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                            posY -= transform.rectangle.height
                        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
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
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                        copyEntity = null
                    }
                }
            }
        }

        latestLeftButtonClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
        latestRightButtonClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
        latestMousePos = mousePos
    }

    fun addEntityToLevel(entity: Entity) {
        level.loadedEntities += entity
        level.addEntity(entity)
    }

    fun removeEntityFromLevel(entity: Entity) {
        if (entity.flags != EntityFactory.EntityType.Player.flag) {
            level.loadedEntities -= entity
            level.removeEntity(entity)
        }
    }

    fun findEntityUnderMouse(): Entity? {
        val mousePosInWorld = level.camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

        entities.forEach {
            val transform = transformMapper[it]
            if (transform.rectangle.contains(mousePosInWorld.x, mousePosInWorld.y)) {
                return it
            }
        }
        return null
    }

    fun showSelectTextureWindow(onTextureSelected: (Pair<FileHandle, Texture>) -> Unit) {
        val folders = Gdx.files.internal("levelObjects").list()

        _stage.addActor(window("Sélectionner une texture") {
            setSize(400f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true

            table {
                val table = VisTable()
                table.setSize(350f, 200f)

                val selectedImage = VisImage()

                selectBox<FileHandle> {
                    items = folders.toGdxArray()

                    addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisSelectBox<FileHandle> ->
                        table.clearChildren()

                        var count = 0

                        Utility.getFilesRecursivly(selected, "png").forEach {
                            val texture = _game.getTexture(it)
                            val image = VisImage(texture.second)

                            image.userObject = texture

                            image.addListener(image.onClick { event, actor ->
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
                    addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                        if (selectedImage.userObject != null) {
                            onTextureSelected(selectedImage.userObject as Pair<FileHandle, Texture>)
                            this@window.remove()
                        }
                    })
                }

                row()

                textButton("Fermer") {
                    addListener(onClick { event, actor ->
                        this@window.remove()
                    })
                }
            }
        })

        onTextureSelected(_game.getTexture(Gdx.files.internal("game/logo.png")))
    }

    fun showAddEntityWindow() {
        _stage.addActor(window("Ajouter une entité") {
            setSize(250f, 400f)
            setPosition(Gdx.graphics.width / 2f - width / 2f, Gdx.graphics.height / 2f - height / 2f)
            isModal = true
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
                            var widthField: VisTextField = VisTextField("50")
                            var heightField: VisTextField = VisTextField("50")

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

                            return Pair(widthField, heightField)
                        }

                        fun addSelectTexture(onTextureSelected: (Pair<FileHandle, Texture>) -> Unit) {
                            this@verticalSettingsGroup.table {
                                val image = com.kotcrab.vis.ui.widget.VisImage()
                                textButton("Sélectionner texture") {
                                    addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                                        showSelectTextureWindow({ texture ->
                                            onTextureSelected(texture)
                                            image.setDrawable(texture.second)
                                            if (!this@table.contains(image)) {
                                                this@table.add(image).size(this.height, this.height).spaceLeft(10f)
                                            }
                                        })
                                    })
                                }
                            }
                        }

                        fun checkValidSize(width: VisTextField, height: VisTextField): Boolean {
                            if (width.text.toIntOrNull() == null || height.text.toIntOrNull() == null)
                                return false
                            val width = width.text.toInt()
                            val height = height.text.toInt()

                            return (width > 0 && width < maxEntitySize) && (height > 0 && height < maxEntitySize)
                        }

                        fun finishEntityBuild(entity: Entity) {
                            addEntityToLevel(entity)
                            copyEntity = entity

                            deleteEntityAfterCopying = true

                            this@window.remove()
                            UIHover = false
                        }

                        addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisSelectBox<EntityFactory.EntityType> ->
                            this@verticalSettingsGroup.clearChildren()
                            this@verticalSettingsGroup.addActor(this)

                            val addButton = VisTextButton("Ajouter !")

                            when (selected) {
                                EntityFactory.EntityType.Sprite -> {
                                    val (width, height) = addSize()

                                    var selectedTexture: Pair<FileHandle, Texture>? = null
                                    addSelectTexture({ texture ->
                                        selectedTexture = texture
                                    })

                                    addButton.addListener(addButton.onClick { event: InputEvent, actor: VisTextButton ->
                                        if (checkValidSize(width, height) && selectedTexture != null) {
                                            finishEntityBuild(EntityFactory.createSprite(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat()), selectedTexture!!))
                                        }
                                    })
                                }
                                EntityFactory.EntityType.PhysicsSprite -> {
                                    val (width, height) = addSize()

                                    var selectedTexture: Pair<FileHandle, Texture>? = null
                                    addSelectTexture({ texture ->
                                        selectedTexture = texture
                                    })

                                    addButton.addListener(addButton.onClick { event: InputEvent, actor: VisTextButton ->
                                        if (checkValidSize(width, height) && selectedTexture != null) {
                                            finishEntityBuild(EntityFactory.createPhysicsSprite(Rectangle(0f, 0f, width.text.toInt().toFloat(), height.text.toInt().toFloat()), selectedTexture!!, be.catvert.mtrktx.ecs.components.PhysicsComponent(true)))
                                        }
                                    })

                                }
                                EntityFactory.EntityType.Enemy -> {
                                    val enemyType = this@verticalSettingsGroup.selectBox<EnemyType> {
                                        items = EnemyType.values().toGdxArray()
                                    }

                                    addButton.addListener(addButton.onClick { event: InputEvent, actor: VisTextButton ->
                                        finishEntityBuild(EntityFactory.createEnemy(_game, _entityEvent, enemyType.selected, Vector2(0f, 0f)))
                                    })
                                }
                                EntityFactory.EntityType.Player -> {}
                            }

                            this@verticalSettingsGroup.addActor(addButton)
                        })

                        if (this.items.size > 1) { // Permet de mettre a jour les acteurs pour créer l'entités
                            this.selectedIndex = 1
                            this.selectedIndex = 0
                        }

                    }
                }
                textButton("Fermer") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                        this@window.remove()
                        UIHover = false
                    })
                }
            }
        })
    }

    fun showInfoEntityWindow() {
        _stage.addActor(window("Réglages des entités") {
            setSize(250f, 400f)
            setPosition(0f, 0f)
            verticalGroup {
                space(10f)

                textButton("Ajouter une entité") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton -> showAddEntityWindow() })
                }
                textButton("Supprimer l'entité sélectionnée") {
                    onSelectEntityChanged.add { _, selectEntity ->
                        this.touchable =
                                if (selectEntity == null || selectEntity.flags == EntityFactory.EntityType.Player.flag)
                                    Touchable.disabled
                                else
                                    Touchable.enabled
                    }

                    addListener(onClick { _: InputEvent, _: KVisTextButton ->
                        removeEntityFromLevel(selectEntities.elementAt(0))
                        clearSelectEntities()
                    })
                }

                label("Aucune entité sélectionnée") {
                    onSelectEntityChanged.add { _, selectEntity ->
                        if(selectEntity == null)
                            setText("Aucune entité sélectionnée")
                        else
                            setText(EntityFactory.EntityType.values().first { entityType -> selectEntity.flags == entityType.flag }.name)
                    }
                }
                horizontalGroup {
                    label("Position X : ")
                    textField("") {
                        onSelectEntityChanged.add { _, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.x.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }
                        onSelectEntityMoved.add { signal, transform ->
                            text = transform.rectangle.x.toInt().toString()
                        }

                        addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisTextField ->
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
                        onSelectEntityChanged.add { signal, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.y.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }
                        onSelectEntityMoved.add { signal, transform ->
                            text = transform.rectangle.y.toInt().toString()
                        }

                        addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisTextField ->
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
                        onSelectEntityChanged.add { signal, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.width.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }

                        addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisTextField ->
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
                        onSelectEntityChanged.add { signal, selectEntity ->
                            if (selectEntity == null) {
                                this.text = ""
                            } else {
                                this.text = transformMapper[selectEntity].rectangle.height.toInt().toString()
                            }

                            this.isReadOnly = selectEntity == null
                        }

                        addListener(onChange { event: ChangeListener.ChangeEvent, actor: VisTextField ->
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
            }
        })
    }

    fun showExitWindow() {
        _stage.addActor(window("Quitter") {
            setPosition(Gdx.graphics.width / 2f - width / 2, Gdx.graphics.height / 2f - height / 2)
            verticalGroup {
                space(10f)

                textButton("Sauvegarder") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton ->
                        try {
                            LevelFactory.saveLevel(level)
                            this@window.remove()
                        } catch(e: Exception) {
                            println("Erreur lors de l'enregistrement du niveau ! Erreur : $e")
                        }
                    })
                }
                textButton("Quitter") {
                    addListener(onClick { event: InputEvent, actor: KVisTextButton -> _game.setScene(MainMenuScene(_game)) })
                }
            }
        })
    }
}