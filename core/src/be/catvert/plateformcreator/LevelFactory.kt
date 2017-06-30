package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.ashley.mapperFor
import ktx.log.error
import ktx.log.info
import java.io.FileReader
import java.io.FileWriter

/**
 * Created by Catvert on 07/06/17.
 */

/**
 * Factory permettant de créer un niveau, de charger un niveau ou de le sauvegarder
 */
class LevelFactory private constructor() {
    companion object {
        /**
         * Crée un niveau vide contenant juste le joueur et un fond d'écran par défaut.
         * @param game L'objet du jeu
         * @param levelFile Le fichier à utiliser pour sauvegarder le nouveau niveau
         * @param levelName Le nom du niveau
         */
        fun createEmptyLevel(game: MtrGame, levelFile: FileHandle, levelName: String): Level {
            val loadedEntities = mutableListOf<Entity>()

            val player = EntityFactory(game, levelFile).createPlayer(Point(100, 100))

            loadedEntities += player

            return Level(game, levelName, levelFile, player, Gdx.files.internal("game/background/background-4.png"), loadedEntities)
        }

        /**
         * Charge le niveau depuis levelFile
         * @param game L'objet du jeu
         * @param levelFile Le fichier utilisé pour charger le niveau
         * @return Boolean : Réussite ou non du chargement du niveau | Level : Le niveau chargé ou non
         */
        fun loadLevel(game: MtrGame, levelFile: FileHandle): Pair<Boolean, Level> {
            val entityFactory = EntityFactory(game, levelFile)

            val entities = mutableListOf<Entity>()

            var loadSuccess = true
            var levelName = ""
            var backgroundPath = FileHandle("")
            var player: Entity = Entity()

            info { "Loading level : $levelFile" }

            if (!levelFile.exists())
                throw Exception("Niveau introuvable ! Chemin : $levelFile")
            fun addEntity(e: Entity) {
                entities += e
            }

            try {
                val root = JsonReader().parse(FileReader(levelFile.path()))
                levelName = root["levelName"].asString()
                backgroundPath = Gdx.files.internal(root["background"].asString())

                fun getPosition(it: JsonValue): Point {
                    return Point(it["position"]["x"].asInt(), it["position"]["y"].asInt())
                }

                fun getSize(it: JsonValue): Pair<Int, Int> {
                    return it["size"]["x"].asInt() to it["size"]["y"].asInt()
                }

                fun getTransformComponent(it: JsonValue): TransformComponent {
                    val (width, height) = getSize(it)
                    val (x, y) = getPosition(it)
                    return TransformComponent(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()))
                }

                fun getRenderComponent(it: JsonValue) = renderComponent { textures, _ ->
                    textures += game.getSpriteSheetTexture(it["spritesheet"].asString(), it["texturePath"].asString())

                    flipX = it["flipX"].asBoolean()
                    flipY = it["flipY"].asBoolean()

                    renderLayer = Layer.values().firstOrNull { layer -> layer.layer == it["layer"].asInt() } ?: Layer.LAYER_0
                }

                root["entities"].forEach {
                    val type = it["type"].asString()
                    when (type) {
                        EntityFactory.EntityType.Sprite.name -> {
                            addEntity(EntityFactory.createSprite(
                                    getTransformComponent(it),
                                    getRenderComponent(it)))
                        }
                        EntityFactory.EntityType.PhysicsSprite.name -> {
                            addEntity(EntityFactory.createPhysicsSprite(
                                    getTransformComponent(it),
                                    getRenderComponent(it), PhysicsComponent(true)))
                        }
                        EntityFactory.EntityType.Player.name -> {
                            player = entityFactory.createPlayer(getPosition(it))
                            addEntity(player)
                        }
                        EntityFactory.EntityType.Enemy.name -> {
                            val enemyType = EnemyType.valueOf(it["enemyType"].asString())
                            addEntity(entityFactory.createEnemyWithType(enemyType, getPosition(it)))
                        }
                        EntityFactory.EntityType.Special.name -> {
                            val specialType = SpecialType.valueOf(it["specialType"].asString())
                            addEntity(entityFactory.createSpecialWithType(specialType, getPosition(it)))
                        }
                    }
                }
            } catch(e: Exception) {
                loadSuccess = false
                error(e, message = { "Erreur lors du chargement du niveau ! Erreur : $e" })
            }

            return Pair(loadSuccess, Level(game, levelName, levelFile, player, backgroundPath, entities))
        }

        /**
         * Sauvegarde un niveau
         * @param level Le niveau à sauvegarder
         */
        fun saveLevel(level: Level) {
            val transformMapper = mapperFor<TransformComponent>()
            val renderMapper = mapperFor<RenderComponent>()

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

            fun saveRender(renderComponent: RenderComponent) {
                writer.name("spritesheet").value(renderComponent.textureInfoList[0].spriteSheet)
                writer.name("texturePath").value(renderComponent.textureInfoList[0].texturePath)
                writer.name("layer").value(renderComponent.renderLayer.layer)
                writer.name("flipX").value(renderComponent.flipX)
                writer.name("flipY").value(renderComponent.flipY)
            }

            writer.`object`()
            writer.name("levelName").value(level.levelName)
            writer.name("background").value(level.backgroundPath)

            writer.array("entities")
            level.loadedEntities.forEach {
                writer.`object`()
                writer.name("type").value(it.getType().name)

                val transformComp = transformMapper[it]

                when (it.getType()) {
                    EntityFactory.EntityType.Sprite -> {
                        savePosition(transformComp)
                        saveSize(transformComp)
                        saveRender(renderMapper[it])
                    }
                    EntityFactory.EntityType.PhysicsSprite -> {
                        savePosition(transformComp)
                        saveSize(transformComp)
                        saveRender(renderMapper[it])
                    }
                    EntityFactory.EntityType.Player -> {
                        savePosition(transformComp)
                    }
                    EntityFactory.EntityType.Enemy -> {
                        savePosition(transformComp)
                        writer.name("enemyType").value(it[EnemyComponent::class.java].enemyType)
                    }
                    EntityFactory.EntityType.Special -> {
                        savePosition(transformComp)
                        writer.name("specialType").value(it[SpecialComponent::class.java].specialType)
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