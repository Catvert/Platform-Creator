package be.catvert.plateformcreator

import be.catvert.plateformcreator.ecs.EntityEvent
import be.catvert.plateformcreator.ecs.EntityFactory
import be.catvert.plateformcreator.ecs.components.BaseComponent
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle

/**
 * Created by Catvert on 03/06/17.
 */

/**
 * Classe static utilitaire
 */
class Utility {
    companion object {

        /**
         * Permet de retourner tout les fichiers présent dans un répertoire récursivement
         */
        fun getFilesRecursivly(dir: FileHandle, fileExt: String = ""): List<FileHandle> {
            val files = mutableListOf<FileHandle>()

            dir.list().forEach {
                if (it.isDirectory)
                    files += getFilesRecursivly(it, fileExt)
                else {
                    if (fileExt.isBlank() || it.extension() == fileExt)
                        files += it
                }
            }
            return files
        }
    }
}

/**
 * Méthode d'extension permettant de copier l'entité
 */
fun Entity.copy(entityFactory: EntityFactory, entityEvent: EntityEvent): Entity {
    return entityFactory.copyEntity(this, entityEvent)
}

/**
 * Méthode d'extension permettant de dessiner une entité à partir d'une région et d'un rectangle
 */
fun <B : Batch> B.draw(texture: TextureAtlas.AtlasRegion, rect: Rectangle, flipX: Boolean = false, flipY: Boolean = true) {
    if (flipX && !texture.isFlipX || !flipX && texture.isFlipX) {
        texture.flip(true, false)
    }
    if (flipY && !texture.isFlipY || !flipY && texture.isFlipY) {
        texture.flip(false, true)
    }

    this.draw(texture, rect.x, rect.y, rect.width, rect.height)
}

operator fun Entity.plusAssign(component: BaseComponent<*>) {
    this.add(component)
}

operator fun Entity.minusAssign(component: Class<out BaseComponent<*>>) {
    this.remove(component)
}

operator fun <T : BaseComponent<*>> Entity.get(component: Class<T>): T {
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
