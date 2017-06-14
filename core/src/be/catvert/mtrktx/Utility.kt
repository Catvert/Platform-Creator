package be.catvert.mtrktx

import be.catvert.mtrktx.ecs.components.BaseComponent
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle

/**
* Created by Catvert on 03/06/17.
*/

class Utility {
    companion object {
        fun getFilesRecursivly(dir: FileHandle, fileExt: String = ""): List<FileHandle> {
            val files = mutableListOf<FileHandle>()

            dir.list().forEach {
                if(it.isDirectory)
                    files += getFilesRecursivly(it)
                else {
                    if(fileExt.isBlank() || it.extension() == fileExt)
                        files += it
                }
            }
            return files
        }
    }
}

fun Entity.copy(): Entity {
    val entityCopy = Entity()
    entityCopy.flags = this.flags

    this.components.forEach {
        if(it is BaseComponent) {
            entityCopy += it.copy(entityCopy)
        }
    }
    return entityCopy
}

fun <B : Batch> B.draw(texture: TextureAtlas.AtlasRegion, rect: Rectangle, flipX: Boolean = false, flipY: Boolean = true) {
    if(flipX && !texture.isFlipX || !flipX && texture.isFlipX) {
        texture.flip(true, false)
    }
    if(flipY && !texture.isFlipY || !flipY && texture.isFlipY){
        texture.flip(false, true)
    }

    this.draw(texture, rect.x, rect.y, rect.width, rect.height)
}

operator fun Entity.plusAssign(component: BaseComponent) {
    this.add(component)
}

operator fun Entity.minusAssign(component: Class<out BaseComponent>) {
    this.remove(component)
}

operator fun <T: BaseComponent> Entity.get(component: Class<T>): T {
    return this.getComponent(component)
}

operator fun Engine.plusAssign(entity: Entity) {
    this.addEntity(entity)
}

operator fun Engine.minusAssign(entity: Entity) {
    this.removeEntity(entity)
}

operator fun Engine.plusAssign(system: EntitySystem) {
    this.addSystem(system)
}

operator fun Engine.minusAssign(system: EntitySystem) {
    this.removeSystem(system)
}
