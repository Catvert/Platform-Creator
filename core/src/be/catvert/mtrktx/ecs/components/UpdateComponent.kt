package be.catvert.mtrktx.ecs.components

import be.catvert.mtrktx.ecs.IUpdateable

/**
 * Created by arno on 04/06/17.
 */

class UpdateComponent(val update: IUpdateable) : BaseComponent()