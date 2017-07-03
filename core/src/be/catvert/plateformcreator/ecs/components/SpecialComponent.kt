package be.catvert.plateformcreator.ecs.components

/**
 * Created by Catvert on 26/06/17.
 */

/**
 * Réprésente les différents type de spécial
 */
enum class SpecialType {
    ExitLevel, BlockEnemy, GoldCoin, Teleporter
}

/**
 * Ce component permet d'identifier l'entité spécial
 * @property specialType Le type spécial de l'entité
 */
class SpecialComponent(val specialType: SpecialType) : BaseComponent<SpecialComponent>() {
    override fun copy(): SpecialComponent {
        return SpecialComponent(specialType)
    }
}