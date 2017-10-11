package be.catvert.pc.components.graphics

import be.catvert.pc.components.Component
import be.catvert.pc.utility.Renderable

enum class Layout {

}

/**
 * Classe abstraite permettant à un component d'être rendu à l'écran
 */
abstract class RenderableComponent : Renderable, Component()