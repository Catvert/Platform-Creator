package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by arno on 07/06/17.
 */

class LevelFactory() {
    companion object {
        fun loadLevel(game:MtrGame, entities: MutableList<Entity>, path: String): Level { // NOTE A SOIS MEME, SI UN ENTITE DOIT CREER UNE AUTRE ENTITE, IL FAUT PASSER LEVEL.ADDENTITY ET PAS LA LISTE ICI
            if (Files.notExists(Paths.get(path)))
                throw Exception("Niveau introuvable ! Chemin : $path")

            fun addEntity(e: Entity) {
                entities += e
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


            return Level("test", player, entities)
        }
    }
}