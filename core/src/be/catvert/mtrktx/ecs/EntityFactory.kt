package be.catvert.mtrktx.ecs

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.*
import be.catvert.mtrktx.get
import be.catvert.mtrktx.plusAssign
import be.catvert.mtrktx.scenes.MainMenuScene
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
 * Created by arno on 03/06/17.
 */

class EntityFactory {
    enum class EntityType(val flag: Int) {
        Sprite(0), PhysicsSprite(1), Player(2)
    }

    companion object {
        fun createSprite(rectangle: Rectangle, texture: Pair<FileHandle, Texture>): Entity {
            val entity = Entity()
            entity.flags = EntityType.Sprite.flag

            entity += TransformComponent(rectangle)
            entity += RenderComponent(texture)

            return entity
        }

        fun createPhysicsSprite(rectangle: Rectangle, texture: Pair<FileHandle, Texture>, physComp: PhysicsComponent): Entity {
            val entity = createSprite(rectangle, texture)
            entity.flags = EntityType.PhysicsSprite.flag
            entity += physComp

            return entity
        }

        fun createPlayer(game: MtrGame, pos: Vector2): Entity {
            val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 50f, 100f), game.getTexture(Gdx.files.internal("game/maryo/small/stand_right.png")), PhysicsComponent(false, 15, true))
            entity.flags = EntityType.Player.flag

            val renderComp = entity.getComponent(RenderComponent::class.java)
            val physicsComp = entity.getComponent(PhysicsComponent::class.java)
            physicsComp.jumpData = JumpData(250)

            entity += UpdateComponent(object : IUpdateable {
                override fun update(deltaTime: Float) {

                    if(Gdx.input.isKeyPressed(Input.Keys.D)) {
                        renderComp.flipX = false
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_RIGHT
                    }
                    if(Gdx.input.isKeyPressed(Input.Keys.Q)) {
                        renderComp.flipX = true
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_LEFT
                    }
                    if(Gdx.input.isKeyPressed(Input.Keys.Z)) {
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_UP
                    }
                    if(Gdx.input.isKeyPressed(Input.Keys.S)) {
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_DOWN
                    }
                    if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                        physicsComp.nextActions += PhysicsComponent.NextActions.JUMP
                }
            })

            entity += LifeComponent(1, { hp -> // remove life event
                if(hp == 0) {
                    game.setScene(MainMenuScene(game))
                }
            }, { hp -> // add life event

            })

            return entity
        }
    }
}