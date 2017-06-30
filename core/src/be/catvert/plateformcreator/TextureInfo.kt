package be.catvert.plateformcreator

import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
 * Created by Catvert on 14/06/17.
 */

/**
 * Classe représentant une texture
 * @param texture la texture(région)
 * @param spriteSheet la feuille de sprites utilisée
 * @param texturePath le chemin vers la texture
 */
data class TextureInfo(val texture: TextureAtlas.AtlasRegion, val spriteSheet: String = "", val texturePath: String = "")