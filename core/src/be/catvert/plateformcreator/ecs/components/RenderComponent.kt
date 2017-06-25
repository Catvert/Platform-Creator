package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.TextureInfo
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2

/**
 * Created by Catvert on 05/06/17.
 */

/**
 * Enum permettant de spécifier comment la taille de l'entité doit changer
 */
enum class ResizeMode {
    NO_RESIZE, ACTUAL_REGION, FIXED_SIZE
}

/**
 * Les layer représente la "couche" de l'entité, les entités ayant la plus haute couche seront dessiner en dernier et seront donc "au-dessus" des autres
 */
enum class Layer(val layer: Int) {
    LAYER_0(0), LAYER_1(1), LAYER_2(2), LAYER_MOVABLE_ENT(3), LAYER_4(4), LAYER_5(5), LAYER_HUD(6)
}

/**
 * Ce component permet d'ajouter une/des textures et/ou une/des animations à l'entité
 * textureInfoList : Liste des textures que dispose l'entité
 * animationList : Liste des animations que dispose l'entité
 * useAnimation : Permet de spécifier si l'entité doit utiliser une animation ou une texture
 * flipX : Permet de retourner horizontalement la texture
 * flipY : Permet de retourner verticalement la texture
 * renderLayer : Permet de spécifier à quel priorité il faut dessiner l'entité, plus il est élevé, plus l'entité sera en avant-plan
 * resizeMode : Permet de spécifier comment la taille de l'entité doit changer
 */
class RenderComponent(val textureInfoList: List<TextureInfo> = listOf(),
                      val animationList: List<Animation<TextureAtlas.AtlasRegion>> = listOf(),
                      var useAnimation: Boolean = false,
                      var flipX: Boolean = false,
                      var flipY: Boolean = false,
                      var renderLayer: Layer = Layer.LAYER_0,
                      var resizeMode: ResizeMode = ResizeMode.NO_RESIZE) : BaseComponent<RenderComponent>() {

    override fun copy(): RenderComponent {
        return RenderComponent(textureInfoList, animationList, useAnimation, flipX, flipY, renderLayer, resizeMode)
    }

    /**
     * Permet de faire fonctionner les animations
     */
    private var stateTime = 0f

    /**
     * Permet au RenderSystem de savoir qu'elle texture il doit utiliser
     */
    var actualTextureInfoIndex: Int = 0
        set(value) {
            field = value

            useAnimation = false
        }

    /**
     * Permet au RenderSystem de savoir qu'elle animation il doit utiliser
     */
    var actualAnimationIndex: Int = 0

    /**
     * Permet de spécifier une taille fixe à l'entité
     * Le resizeMode doit être en FIXED_RESIZE
     */
    var fixedResize: Vector2? = null

    /**
     * Permet de retourner la region à dessiner
     * En appelant cette fonction, l'animation actuelle se met à jour (si useAnimation est vrai).
     */
    fun getActualAtlasRegion(): TextureAtlas.AtlasRegion {
        if (useAnimation) {
            stateTime += Gdx.graphics.deltaTime
            return animationList[actualAnimationIndex].getKeyFrame(stateTime, true)
        } else {
            return textureInfoList[actualTextureInfoIndex].texture
        }
    }
}

/**
 * Permet de construire un renderComponent
 */
fun renderComponent(init: RenderComponent.(textures: MutableList<TextureInfo>, animations: MutableList<Animation<TextureAtlas.AtlasRegion>>) -> Unit): RenderComponent {
    val textureList = mutableListOf<TextureInfo>()
    val animationsList = mutableListOf<Animation<TextureAtlas.AtlasRegion>>()

    val renderComponent = RenderComponent(textureList, animationsList)

    renderComponent.init(textureList, animationsList)

    return renderComponent
}