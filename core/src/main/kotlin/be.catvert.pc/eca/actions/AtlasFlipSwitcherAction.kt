package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.graphics.AtlasComponent
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor

@Description("Permet d'appeler une action selon l'état de flip de l'AtlasComponent")
class AtlasFlipSwitcherAction(@ExposeEditor var unFlipXAction: Action = EmptyAction(), @ExposeEditor var flipXAction: Action = EmptyAction(), @ExposeEditor var unFlipYAction: Action = EmptyAction(), @ExposeEditor var flipYAction: Action = EmptyAction()) : Action() {
    override fun invoke(entity: Entity) {
        entity.getCurrentState().getComponent<AtlasComponent>()?.apply {
            if (flipX)
                flipXAction(entity)
            else
                unFlipXAction(entity)

            if (flipY)
                flipYAction(entity)
            else
                unFlipYAction(entity)
        }
    }
}