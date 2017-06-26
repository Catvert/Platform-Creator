package be.catvert.plateformcreator.ecs.components

/**
 * Created by Catvert on 26/06/17.
 */

enum class SpecialType {
    EXIT_LEVEL
}

class SpecialComponent(val specialType: SpecialType) : BaseComponent<SpecialComponent>() {
    override fun copy(): SpecialComponent {
        return SpecialComponent(specialType)
    }
}