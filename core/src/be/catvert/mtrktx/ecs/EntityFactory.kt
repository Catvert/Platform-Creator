package be.catvert.mtrktx.ecs

import be.catvert.mtrktx.MtrGame
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import be.catvert.mtrktx.ecs.components.UpdateComponent
import be.catvert.mtrktx.plusAssign
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.assets.loadOnDemand

/**
 * Created by arno on 03/06/17.
 */

class EntityFactory {
    companion object {
        fun createSprite(rectangle: Rectangle, texture: Texture): Entity {
            val entity = Entity()

            entity += TransformComponent(rectangle)
            entity += RenderComponent(texture)

            return entity
        }

        fun createPhysicsSprite(rectangle: Rectangle, texture: Texture, physComp: PhysicsComponent): Entity {
            val entity = createSprite(rectangle, texture)

            entity += physComp

            return entity
        }

        fun createPlayer(game: MtrGame, pos: Vector2): Entity {
            val entity = createPhysicsSprite(Rectangle(pos.x, pos.y, 50f, 100f), game.assetManager.loadOnDemand<Texture>("game/maryo/small/stand_right.png").asset, PhysicsComponent(false, 10, true))

            val renderComp = entity.getComponent(RenderComponent::class.java)
            val physicsComp = entity.getComponent(PhysicsComponent::class.java)

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
                    if(Gdx.input.isKeyPressed(Input.Keys.Z))
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_UP
                    if(Gdx.input.isKeyPressed(Input.Keys.S))
                        physicsComp.nextActions += PhysicsComponent.NextActions.GO_DOWN
                }
            })

            return entity
        }
    }
}