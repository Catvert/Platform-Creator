package be.catvert.plateformcreator

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
         * @param dir Le chemin vers le dossier à itérer
         * @param fileExt Optionelle, l'extension que doit avoir chaque fichier présent dans la liste
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
 * Réprésente un point dans l'espace
 * @param x La position x
 * @param y La position y
 */
data class Point(val x: Int, val y: Int)

/**
 * Méthode d'extension permettant de simplifier l'accès au nom de la classe utilisée
 */
fun Any.className(): String = this::class.java.simpleName

/**
 * Builder pour créer une entité
 */
fun entity(init: Entity.() -> Unit): Entity {
    val entity = Entity()

    entity.init()

    return entity
}

/**
 * Méthode d'extension permettant d'assigner un type à l'entité (flag)
 * @param entityType Le type à assigner à l'entité
 */
fun Entity.setType(entityType: EntityFactory.EntityType) {
    this.flags = entityType.flag
}

/**
 * Méthode d'extension permettant d'avoir le type de l'entité depuis son flag
 */
fun Entity.getType(): EntityFactory.EntityType = EntityFactory.EntityType.values().first { it.flag == this.flags }

/**
 * Méthode d'extension permettant de vérifier si l'entité est un type précis (flag)
 * @param entityType Le type à comparer avec l'entité
 */
infix fun Entity.isType(entityType: EntityFactory.EntityType): Boolean {
    return this.flags == entityType.flag
}

/**
 * Méthode d'extension permettant de vérifier si l'entité n'est pas un type précis (flag)
 * @param entityType Le type à comparer avec l'entité
 */
infix fun Entity.isNotType(entityType: EntityFactory.EntityType): Boolean {
    return this.flags != entityType.flag
}

/**
 * Méthode d'extension permettant de retourner un component précis
 * @param component La classe du component à retourner
 */
operator fun <T : BaseComponent<*>> Entity.get(component: Class<T>): T {
    return this.getComponent(component)
}

/**
 * Méthode d'extension permettant de dessiner une entité à partir d'une région et d'un rectangle
 * @param texture La texture à dessiner
 * @param rect Le rectangle utilisé pour dessiner la texture
 * @param flipX Retourner la texture horizontalement
 * @param flipY Retourner la texture verticalement
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
