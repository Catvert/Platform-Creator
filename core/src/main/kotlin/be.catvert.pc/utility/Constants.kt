package be.catvert.pc.utility

/**
 * Objet permettant d'accéder aux constantes du jeu
 */
object Constants {
    const val gameTitle = "Platform Creator"
    const val gameVersion = 2.0f

    const val assetsDirPath = "assets/"

    const val UISkinPath = assetsDirPath + "ui/neutralizer-fix/neutralizer.json"

    const val mainFontPath = assetsDirPath + "fonts/mainFont.fnt"
    const val editorFontPath = assetsDirPath + "fonts/editorFont.fnt"

    const val configPath = assetsDirPath + "config.json"
    const val keysConfigPath = assetsDirPath + "keysConfig.json"

    const val atlasDirPath = assetsDirPath + "atlas/"
    const val texturesDirPath = assetsDirPath + "textures/"
    const val soundsDirPath = assetsDirPath + "sounds/"
    const val backgroundsDirPath = assetsDirPath + "game/background/"
    const val prefabsDirPath = assetsDirPath + "prefabs/"
    const val levelDirPath = assetsDirPath + "levels/"

    const val gameBackgroundMenuPath = assetsDirPath + "game/mainmenu.png"
    const val gameLogoPath = assetsDirPath + "game/logo.png"

    const val prefabExtension = ".prefab"
    const val levelExtension = ".pclvl"

    const val noTextureAtlasFoundPath = assetsDirPath + "game/notexture_atlas.atlas"
    const val noTextureFoundTexturePath = assetsDirPath + "game/notexture.png"
    const val noSoundPath = assetsDirPath + "game/nosound.wav"

    const val maxGameObjectSize = 500

    const val minLayerIndex = -100
    const val maxLayerIndex = 100
}