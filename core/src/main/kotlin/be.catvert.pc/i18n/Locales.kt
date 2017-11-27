package be.catvert.pc.i18n

import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.loadOnDemand
import com.badlogic.gdx.utils.I18NBundle
import java.util.*

object Locales {
    private lateinit var menusBundle: I18NBundle
    private lateinit var gameBundle: I18NBundle
    private lateinit var editorBundle: I18NBundle

    fun load() {
        menusBundle = PCGame.assetManager.loadOnDemand<I18NBundle>(Constants.bundlesDirPath.child("menus/bundle")).asset
        gameBundle = PCGame.assetManager.loadOnDemand<I18NBundle>(Constants.bundlesDirPath.child("menus/bundle")).asset
        editorBundle = PCGame.assetManager.loadOnDemand<I18NBundle>(Constants.bundlesDirPath.child("menus/bundle")).asset
    }

    fun get(key: MenusText, vararg args: Any): String {
        try {
            return menusBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }

    fun get(key: GameText, vararg args: Any): String {
        try {
            return gameBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }

    fun get(key: EditorText, vararg args: Any): String {
        try {
            return editorBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }
}