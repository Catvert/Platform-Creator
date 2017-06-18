package be.catvert.plateformcreator.scenes

import be.catvert.plateformcreator.Level
import be.catvert.plateformcreator.MtrGame
import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.components.TransformComponent
import be.catvert.plateformcreator.ecs.systems.RenderingSystem
import be.catvert.plateformcreator.ecs.systems.UpdateSystem
import be.catvert.plateformcreator.ecs.systems.physics.PhysicsSystem
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils


/**
 * Created by arno on 03/06/17.
 */

/**
 * Scène du jeu
 */
class GameScene(game: MtrGame, entityEvent: EntityEvent, private val level: Level) : BaseScene(game, entityEvent, RenderingSystem(game), UpdateSystem(level), PhysicsSystem(level)) {
    override val entities: MutableList<Entity> = mutableListOf()

    private val cameraMoveSpeed = 10f

    private val transformPlayer = level.player.getComponent(TransformComponent::class.java)

    init {
        _game.background = level.background
        _entityEvent.onEntityAdded = { entity -> level.addEntity(entity) }
        _entityEvent.onEntityRemoved = { entity -> level.removeEntity(entity) }

        entities.addAll(level.loadedEntities)
    }

    override fun render(delta: Float) {
        super.render(delta)

        level.activeRect.setPosition(Math.max(0f, transformPlayer.rectangle.x - level.activeRect.width / 2 + transformPlayer.rectangle.width / 2), Math.max(0f, transformPlayer.rectangle.y - level.activeRect.height / 2 + transformPlayer.rectangle.height / 2))

        if(level.followPlayerCamera) {
            val x = MathUtils.lerp(camera.position.x, Math.max(0f + camera.viewportWidth / 2, transformPlayer.rectangle.x + transformPlayer.rectangle.width / 2), 0.1f)
            val y = MathUtils.lerp(camera.position.y, Math.max(0f + camera.viewportHeight / 2, transformPlayer.rectangle.y + transformPlayer.rectangle.height / 2), 0.1f)
            camera.position.set(x, y, 0f)
        }

        level.update(delta)

        entities.clear()
        entities.addAll(level.getAllEntitiesInCells(level.getActiveGridCells()))
    }

    override fun updateInputs() {
        super.updateInputs()

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            _game.setScene(PauseScene(_game))
        }

        if(Gdx.input.isKeyPressed(Input.Keys.P)) {
            camera.zoom -= 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.M)) {
            camera.zoom += 0.02f
        }
        if(Gdx.input.isKeyPressed(Input.Keys.L)) {
            camera.zoom = 1f
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            level.followPlayerCamera = !level.followPlayerCamera
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.position.x -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.position.x += cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.position.y -= cameraMoveSpeed
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.position.y += cameraMoveSpeed
        }

        if( Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            level.drawDebugCells = !level.drawDebugCells
        }
        if( Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            level.applyGravity = !level.applyGravity
        }

       camera.update()
    }
}