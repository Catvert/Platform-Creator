package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.assets.loadOnDemand
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by arno on 07/06/17.
 */

class LevelFactory() {
    companion object {
        fun loadLevel(game:MtrGame, engine: Engine, path: String): Level {
            if (Files.notExists(Paths.get(path)))
                throw Exception("Niveau introuvable ! Chemin : $path")

            val loadedEntities = mutableListOf<Entity>()

            fun addEntity(e: Entity) {
                loadedEntities.add(e)
                engine += e
            }

            val player = EntityFactory.createPlayer(game, Vector2(0f, 400f))
            addEntity(player)

            val entity4 = EntityFactory.createPhysicsSprite(Rectangle(0f, 0f, 50f, 50f), game.getTexture(Gdx.files.internal("levelObjects/blocks/brick/Brick Blue.png")), PhysicsComponent(true))
            addEntity(entity4)

            for(x in 0..500) {
                for(y in 0..500) {
                    addEntity(EntityFactory.createPhysicsSprite(Rectangle(300f + x * 50, 300f + y * 50, 50f, 50f), game.getTexture(Gdx.files.internal("levelObjects/blocks/brick/Brick Red.png")), PhysicsComponent(true)))
                }
            }


            return Level("test", player, loadedEntities)
        }
    }
}