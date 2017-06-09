package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.IUpdateable
import com.badlogic.ashley.core.Component

/**
 * Created by arno on 04/06/17.
 */

class UpdateComponent(val update: IUpdateable) : BaseComponent()