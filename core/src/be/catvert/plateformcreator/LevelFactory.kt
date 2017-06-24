package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.*
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

        val player = entityFactory.createPlayer(Vector2(100f, 100f))
        val defaultBackground = game.getGameTexture(Gdx.files.internal("game/background/background-4.png"))

        loadedEntities += player

        /* for(x in 0..300) { // large amount of entity test
             for(y in 0..50) {
                 loadedEntities += entityFactory.createPhysicsSprite(Rectangle(500 + 50f*x, 500 + 50f *y, 50f, 50f), RenderComponent(listOf(game.getSpriteSheetTexture("sheet", "slice03_03"))), PhysicsComponent(true))
             }
         }*/

        return Level(game, levelName, levelPath, player, RenderComponent(listOf(defaultBackground)), loadedEntities) to EntityEvent()
    }

    /**
     * Charge un niveau
     */
    fun loadLevel(levelPath: FileHandle): Triple<Boolean, Level, EntityEvent> { // NOTE A SOIS MEME, SI UN ENTITE DOIT CREER UNE AUTRE ENTITE, IL FAUT PASSER LEVEL.ADDENTITY ET PAS LA LISTE ICI
        val entities = mutableListOf<Entity>()
        val entityEvent = EntityEvent()

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
                return it["position"]["x"].asInt() to it["position"]["y"].asInt()
            }

            fun getSize(it: JsonValue): Pair<Int, Int> {
                return  it["size"]["x"].asInt() to it["size"]["y"].asInt()
            }

            fun getSpriteSheetTexture(it: JsonValue): Triple<String, String, Layer> { // First is the sprite sheet, second is the texture name
                var layer = Layer.LAYER_0
                if(it.has("layer")) {
                    val layerInt = it["layer"].asInt()
                    val loadedLayer = Layer.values().firstOrNull { it.layer == layerInt }
                    if(loadedLayer != null)
                        layer = loadedLayer
                }

                return Triple(it["spritesheet"].asString(), it["texture_name"].asString(), layer)
            }

            root["entities"].forEach {
                val type = it["type"].asString()
                val (x, y) = getPosition(it)
                when (type) {
                    EntityFactory.EntityType.Sprite.name -> {
                        val (width, height) = getSize(it)
                        val texture = getSpriteSheetTexture(it)
                        addEntity(entityFactory.createSprite(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()), RenderComponent(listOf(game.getSpriteSheetTexture(texture.first, texture.second)), renderLayer = texture.third)))
                    }
                    EntityFactory.EntityType.PhysicsSprite.name -> {
                        val (width, height) = getSize(it)
                        val texture = getSpriteSheetTexture(it)
                        addEntity(entityFactory.createPhysicsSprite(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()), RenderComponent(listOf(game.getSpriteSheetTexture(texture.first, texture.second)), renderLayer = texture.third), PhysicsComponent(true)))
                    }
                    EntityFactory.EntityType.Player.name -> {
                        player = entityFactory.createPlayer(Vector2(x.toFloat(), y.toFloat()))
                        addEntity(player)
                    }
                    EntityFactory.EntityType.Enemy.name -> {
                        val enemyType = EnemyType.valueOf(it["enemyType"].asString())
                        addEntity(entityFactory.createEnemyWithType(enemyType, entityEvent, Vector2(x.toFloat(), y.toFloat())))
                    }
                }
            }
        } catch(e: Exception) {
            errorInLevel = true
            println("Erreur lors du chargement du niveau ! Erreur : $e")
        }

        return Triple(!errorInLevel, Level(game, levelName, levelPath, player, RenderComponent(listOf(game.getGameTexture(backgroundPath))), entities), entityEvent)
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
            writer.name("texture_name").value(renderComponent.textureInfoList[0].textureName)
            writer.name("layer").value(renderComponent.renderLayer.layer)
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
            }
            writer.pop()
        }
        writer.pop()

        writer.pop()

        writer.flush()
        writer.close()
    }

}