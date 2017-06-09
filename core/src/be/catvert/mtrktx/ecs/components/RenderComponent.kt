package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Texture

/**
 * Created by arno on 05/06/17.
 */

class RenderComponent(var texture: Texture, var flipX: Boolean = false, var flipY: Boolean = false) : BaseComponent()