package be.catvert.pc.components.graphics

import be.catvert.pc.GameObject
import be.catvert.pc.utility.InheritanceAdapter
import be.catvert.pc.PCGame
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import ktx.assets.getValue
import ktx.assets.loadOnDemand

@JsonAdapter(TextureComponentAdapter::class)
class TextureComponent(val texturePath: String, rectangle: Rectangle, val linkRectToGO: Boolean) : RenderableComponent() {
    private val texture by PCGame.assetManager.loadOnDemand<Texture>(texturePath)

    var rectangle: Rectangle = rectangle
        private set

    override fun onGameObjectSet(gameObject: GameObject) {
        super.onGameObjectSet(gameObject)

        if(linkRectToGO)
            rectangle = gameObject.rectangle
    }

    override fun render(batch: Batch) {
        batch.draw(texture, rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }
}

class TextureComponentAdapter : InheritanceAdapter<TextureComponent>() {
    private val texturePathStr = "texturePath"
    private val rectangleStr = "rectangle"
    private val linkRectToGOStr = "linkRectToGO"

    init {
        customSerializer = { src, _, context ->
            val jsonObj = JsonObject()

            jsonObj.addProperty(texturePathStr, src.texturePath)
            jsonObj.add(rectangleStr, context.serialize(src.rectangle))
            jsonObj.addProperty(linkRectToGOStr, src.linkRectToGO)

            jsonObj
        }

        customDeserializer = { json, _, context ->
            val jsonObj = json.asJsonObject

            val texturePath = jsonObj.getAsJsonPrimitive(texturePathStr).asString
            val rectangle = context.deserialize<Rectangle>(jsonObj.get(rectangleStr), Rectangle::class.java)
            val linkRectToGOStr = jsonObj.getAsJsonPrimitive(linkRectToGOStr).asBoolean

            TextureComponent(texturePath, rectangle, linkRectToGOStr)
        }
    }
}