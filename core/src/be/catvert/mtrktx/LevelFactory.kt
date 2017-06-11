package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.EntityFactory
import be.catvert.mtrktx.ecs.components.PhysicsComponent
import be.catvert.mtrktx.ecs.components.RenderComponent
import be.catvert.mtrktx.ecs.components.TransformComponent
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import java.io.FileReader
import java.io.FileWriter

/**
 * Created by arno on 07/06/17.
 */

class LevelFactory() {
    companion object {
        fun loadLevel(game:MtrGame, levelPath: FileHandle): Pair<Boolean, Level> { // NOTE A SOIS MEME, SI UN ENTITE DOIT CREER UNE AUTRE ENTITE, IL FAUT PASSER LEVEL.ADDENTITY ET PAS LA LISTE ICI
            val entities = mutableListOf<Entity>()

            if (!levelPath.exists())
                throw Exception("Niveau introuvable ! Chemin : $levelPath")
            fun addEntity(e: Entity) {
                entities += e
            }

            var errorInLevel = false
            var levelName = ""
            var backgroundPath = FileHandle("")
            var player: Entity = Entity()

            try {
                val root = JsonReader().parse(FileReader(levelPath.path()))
                levelName = root["levelName"].asString()
                backgroundPath = Gdx.files.internal(root["background"].asString())

                fun getPosition(it: JsonValue): Pair<Int, Int> {
                   return Pair(it["position"]["x"].asInt(), it["position"]["y"].asInt())
                }

                fun getSize(it: JsonValue): Pair<Int, Int> {
                    return Pair(it["size"]["x"].asInt(), it["size"]["y"].asInt())
                }

                fun getTexture(it: JsonValue): String {
                    return it["texture"].asString()
                }

                root["entities"].forEach {
                    val type = it["type"].asString()
                    val (x, y) = getPosition(it)
                    when(type) {
                        EntityFactory.EntityType.Sprite.name -> {
                            val (width, height) = getSize(it)
                            val texture = getTexture(it)
                            addEntity(EntityFactory.createSprite(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()), game.getTexture(Gdx.files.internal(texture))))
                        }
                        EntityFactory.EntityType.PhysicsSprite.name -> {
                            val (width, height) = getSize(it)
                            val texture = getTexture(it)
                            addEntity(EntityFactory.createPhysicsSprite(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()), game.getTexture(Gdx.files.internal(texture)), PhysicsComponent(true)))
                        }
                        EntityFactory.EntityType.Player.name -> {
                            player = EntityFactory.createPlayer(game, Vector2(x.toFloat(), y.toFloat()))
                            addEntity(player)
                        }
                    }
                }
            } catch(e: Exception) {
                errorInLevel = true
                println("Erreur lors du chargement du niveau ! Erreur : $e")
            }

            return Pair(!errorInLevel, Level(levelName, player, Pair(backgroundPath, RenderComponent(game.getTexture(backgroundPath))), levelPath, entities))
        }

        fun saveLevel(level: Level) {
            val writer = JsonWriter(FileWriter(level.levelFile.path()))
            writer.setOutputType(JsonWriter.OutputType.json)

            fun savePosition(transformComponent: TransformComponent) {
                writer.`object`("position")
                writer.name("x").value(transformComponent.rectangle.x.toInt())
                writer.name("y").value(transformComponent.rectangle.y.toInt())
                writer.pop()
            }

            fun saveSize(transformComponent: TransformComponent) {
                writer.`object`("size")
                writer.name("x").value(transformComponent.rectangle.width.toInt())
                writer.name("y").value(transformComponent.rectangle.height.toInt())
                writer.pop()
            }

            fun saveTexture(renderComponent: RenderComponent) {
                writer.name("texture").value(renderComponent.texture.first.path())
            }

            writer.`object`()
            writer.name("levelName").value(level.levelName)
            writer.name("background").value(level.background.first)

            writer.array("entities")
            level.loadedEntities.forEach {
                writer.`object`()
                writer.name("type").value(EntityFactory.EntityType.values()[it.flags].name)
                when(it.flags) {
                    EntityFactory.EntityType.Sprite.flag -> {
                        savePosition(it[TransformComponent::class.java])
                        saveSize(it[TransformComponent::class.java])
                        saveTexture(it[RenderComponent::class.java])
                    }
                    EntityFactory.EntityType.PhysicsSprite.flag -> {
                        savePosition(it[TransformComponent::class.java])
                        saveSize(it[TransformComponent::class.java])
                        saveTexture(it[RenderComponent::class.java])
                    }
                    EntityFactory.EntityType.Player.flag -> {
                        savePosition(it[TransformComponent::class.java])
                    }
                }
                writer.pop()
            }
            writer.pop()

            writer.pop()

            writer.flush()
            writer.close()
        }
    }
}