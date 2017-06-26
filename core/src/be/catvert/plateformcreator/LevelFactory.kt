package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import ktx.log.error
import ktx.log.info
import org.lwjgl.util.Point
import java.io.FileReader
import java.io.FileWriter

/**
 * Created by Catvert on 07/06/17.
 */

/**
 * Factory permettant de créer un niveau, de charger un niveau ou de le sauvegarder
 */
class LevelFactory(private val game: MtrGame) {
    private val entityFactory = game.entityFactory

    /**
     * Crée un niveau vide contenant juste le joueur et un fond d'écran par défaut.
     */
    fun createEmptyLevel(levelName: String, levelPath: FileHandle): Pair<Level, EntityEvent> {
        val loadedEntities = mutableListOf<Entity>()

        val entityEvent = EntityEvent(levelPath)

        val player = entityFactory.createPlayer(entityEvent, Point(100, 100))
        val defaultBackground = game.getGameTexture(Gdx.files.internal("game/background/background-4.png"))

        loadedEntities += player

        /* for(x in 0..300) { // large amount of entity test
             for(y in 0..50) {
                 loadedEntities += entityFactory.createPhysicsSprite(Rectangle(500 + 50f*x, 500 + 50f *y, 50f, 50f), RenderComponent(listOf(game.getSpriteSheetTexture("sheet", "slice03_03"))), PhysicsComponent(true))
             }
         }*/
        return Level(game, levelName, levelPath, player, renderComponent { textures, _ -> textures += defaultBackground }, loadedEntities) to entityEvent
    }

    /**
     * Charge un niveau
     */
    fun loadLevel(levelPath: FileHandle): Triple<Boolean, Level, EntityEvent> { // NOTE A SOIS MEME, SI UN ENTITE DOIT CREER UNE AUTRE ENTITE, IL FAUT PASSER PAR LEVEL.ADDENTITY ET PAS LA LISTE ICI
        info { "Loading level : $levelPath" }

        val entities = mutableListOf<Entity>()

        val entityEvent = EntityEvent(levelPath)

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
                        addEntity(entityFactory.createSprite(
                                getTransformComponent(it),
                                getRenderComponent(it)))
                    }
                    EntityFactory.EntityType.PhysicsSprite.name -> {
                        addEntity(entityFactory.createPhysicsSprite(
                                getTransformComponent(it),
                                getRenderComponent(it), PhysicsComponent(true)))
                    }
                    EntityFactory.EntityType.Player.name -> {
                        player = entityFactory.createPlayer(entityEvent, getPosition(it))
                        addEntity(player)
                    }
                    EntityFactory.EntityType.Enemy.name -> {
                        val enemyType = EnemyType.valueOf(it["enemyType"].asString())
                        addEntity(entityFactory.createEnemyWithType(enemyType, entityEvent, getPosition(it)))
                    }
                    EntityFactory.EntityType.Special.name -> {
                        val specialType = SpecialType.valueOf(it["specialType"].asString())
                        addEntity(entityFactory.createSpecialWithType(specialType, entityEvent, getPosition(it)))
                    }
                }
            }
        } catch(e: Exception) {
            errorInLevel = true
            error(e, message = { "Erreur lors du chargement du niveau ! Erreur : $e" })
        }

        return Triple(!errorInLevel, Level(game, levelName, levelPath, player, renderComponent { textures, _ -> textures += game.getGameTexture(backgroundPath) }, entities), entityEvent)
    }

    /**
     * Sauvegarde un niveau
     */
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

        fun saveRender(renderComponent: RenderComponent) {
            writer.name("spritesheet").value(renderComponent.textureInfoList[0].spriteSheet)
            writer.name("texturePath").value(renderComponent.textureInfoList[0].texturePath)
            writer.name("layer").value(renderComponent.renderLayer.layer)
            writer.name("flipX").value(renderComponent.flipX)
            writer.name("flipY").value(renderComponent.flipY)
        }

        writer.`object`()
        writer.name("levelName").value(level.levelName)
        writer.name("background").value(level.background.textureInfoList[0].texturePath)

        writer.array("entities")
        level.loadedEntities.forEach {
            writer.`object`()
            writer.name("type").value(EntityFactory.EntityType.values()[it.flags].name)

            val transformComp = it[TransformComponent::class.java]

            when (it.flags) {
                EntityFactory.EntityType.Sprite.flag -> {
                    savePosition(transformComp)
                    saveSize(transformComp)
                    saveRender(it[RenderComponent::class.java])
                }
                EntityFactory.EntityType.PhysicsSprite.flag -> {
                    savePosition(transformComp)
                    saveSize(transformComp)
                    saveRender(it[RenderComponent::class.java])
                }
                EntityFactory.EntityType.Player.flag -> {
                    savePosition(transformComp)
                }
                EntityFactory.EntityType.Enemy.flag -> {
                    savePosition(transformComp)
                    writer.name("enemyType").value(it[EnemyComponent::class.java].enemyType)
                }
                EntityFactory.EntityType.Special.flag -> {
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