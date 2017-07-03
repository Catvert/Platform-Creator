package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.GameKeys
import be.catvert.plateformcreator.Level
import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.components.TransformComponent
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import be.catvert.plateformcreator.ecs.systems.UpdateSystem
import be.catvert.plateformcreator.ecs.systems.physics.PhysicsSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Timer
import ktx.app.use

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Scène du jeu
 * @param game L'objet du jeu
 * @param level Le niveau
 * @param onEndOfLevel
 */
class GameScene(game: MtrGame, private val level: Level) : BaseScene(game, RenderingSystem(game), UpdateSystem(level), PhysicsSystem(level)) {
    private val cameraMoveSpeed = 10f

    private val transformPlayer = level.player.getComponent(TransformComponent::class.java)

    private var scorePlayer = 0
    private var _timeInGame = 0

    var timeTimer: Timer.Task = initTimer()

    init {
        background = game.getBackground(level.backgroundPath).second

        EntityEvent.onEntityAdded = { entity -> level.addEntity(entity) }
        EntityEvent.onEntityRemoved = { entity -> level.removeEntity(entity) }
        EntityEvent.onAddScore = { scorePlayer += it }
        EntityEvent.onEndLevel = { levelSuccess -> game.setScene(EndLevelScene(game, level.levelFile, levelSuccess)) }

        entities.addAll(level.loadedEntities)
    }

    /**
     * Permet de (ré)initialiser le timer quand celui-ci a été coupé le hide()
     */
    fun initTimer() = Timer.schedule(object : Timer.Task() {
        override fun run() {
            ++_timeInGame
        }
    }, 1f, 1f)!!

    override fun show() {
        super.show()
        if (!timeTimer.isScheduled)
            timeTimer = initTimer()
    }

    override fun hide() {
        super.hide()
        timeTimer.cancel()
    }

    override fun render(delta: Float) {
        super.render(delta)

        level.drawDebug()

        game.batch.projectionMatrix = game.defaultProjection
        game.batch.use { gameBatch ->
            game.mainFont.draw(gameBatch, "Score : $scorePlayer", 10f, Gdx.graphics.height - game.mainFont.lineHeight)
            val layout = GlyphLayout(game.mainFont, "Temps : $_timeInGame")
            game.mainFont.draw(gameBatch, layout, Gdx.graphics.width / 2 - layout.width / 2, Gdx.graphics.height - layout.height)
        }

        level.activeRect.setPosition(Math.max(0f, transformPlayer.rectangle.x - level.activeRect.width / 2 + transformPlayer.rectangle.width / 2), Math.max(0f, transformPlayer.rectangle.y - level.activeRect.height / 2 + transformPlayer.rectangle.height / 2))

        if (level.followPlayerCamera) {
            val x = MathUtils.lerp(camera.position.x, Math.max(0f + camera.viewportWidth / 2, transformPlayer.rectangle.x + transformPlayer.rectangle.width / 2), 0.1f)
            val y = MathUtils.lerp(camera.position.y, Math.max(0f + camera.viewportHeight / 2, transformPlayer.rectangle.y + transformPlayer.rectangle.height / 2), 0.1f)
            camera.position.set(x, y, 0f)
        }
    }

    override fun update(deltaTime: Float) {
        level.update(deltaTime)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))

        game.refreshEntitiesInEngine()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hide()
            game.setScene(PauseScene(game, level.levelFile, this), false)
        }

        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_UP.key)) {
            camera.zoom -= 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_DOWN.key)) {
            camera.zoom += 0.02f
        }
        if (Gdx.input.isKeyPressed(GameKeys.CAMERA_ZOOM_RESET.key)) {
            camera.zoom = 1f
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_FOLLOW_CAMERA_PLAYER.key)) {
            level.followPlayerCamera = !level.followPlayerCamera
        }
        if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_LEFT.key)) {
            camera.position.x -= cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_RIGHT.key)) {
            camera.position.x += cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_DOWN.key)) {
            camera.position.y -= cameraMoveSpeed
        }
        if (Gdx.input.isKeyPressed(GameKeys.GAME_CAMERA_UP.key)) {
            camera.position.y += cameraMoveSpeed
        }

        if (Gdx.input.isKeyJustPressed(GameKeys.DEBUG_MODE.key)) {
            level.drawDebugCells = !level.drawDebugCells
        }
        if (Gdx.input.isKeyJustPressed(GameKeys.GAME_SWITCH_GRAVITY.key)) {
            level.applyGravity = !level.applyGravity
        }

        camera.update()
    }
}