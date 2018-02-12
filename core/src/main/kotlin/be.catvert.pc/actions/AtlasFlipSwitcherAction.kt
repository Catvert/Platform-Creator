package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.ExposeEditor

class AtlasFlipSwitcherAction(@ExposeEditor var unFlipXAction: Action = EmptyAction(), @ExposeEditor var flipXAction: Action = EmptyAction(), @ExposeEditor var unFlipYAction: Action = EmptyAction(), @ExposeEditor var flipYAction: Action = EmptyAction()) : Action() {
    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.apply {
            if (flipX)
                flipXAction(gameObject)
            else
                unFlipXAction(gameObject)

            if (flipY)
                flipYAction(gameObject)
            else
                unFlipYAction(gameObject)
        }
    }
}