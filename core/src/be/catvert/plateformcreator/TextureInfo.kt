package be.catvert.plateformcreator

import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
 * Created by Catvert on 14/06/17.
 */

data class TextureInfo(val texture: TextureAtlas.AtlasRegion, val spriteSheet: String = "", val texturePath: String = "")