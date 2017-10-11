package be.catvert.pc.components.logics

import be.catvert.pc.components.Component
import be.catvert.pc.utility.Updeatable

/**
 * Classe abstraite permettant à un component d'être mis à jour
 */
abstract class UpdeatableComponent : Updeatable, Component()