package be.catvert.pc.utility

/**
 * Objet permettant d'accéder aux constantes du jeu
 */
object Constants {
    val gameTitle = "Platform Creator"
    val gameVersion = 2.0f

    val assetsDirPath = "assets/"

    val UISkinPath = assetsDirPath + "ui/neutralizer-fix/neutralizer.json"

    val mainFontPath = assetsDirPath + "fonts/mainFont.fnt"
    val editorFontPath = assetsDirPath + "fonts/editorFont.fnt"

    val configPath = assetsDirPath + "config.json"
    val keysConfigPath = assetsDirPath + "keysConfig.json"

    val atlasDirPath = assetsDirPath + "atlas/"
    val texturesDirPath = assetsDirPath + "textures/"
    val soundsDirPath = assetsDirPath + "sounds/"
    val backgroundsDirPath = assetsDirPath + "game/background/"

    val gameBackgroundMenuPath = assetsDirPath + "game/mainmenu.png"
    val gameLogoPath = assetsDirPath + "game/logo.png"

    val levelDirPath = assetsDirPath + "levels/"
    val levelExtension = ".pclvl"

    val noTextureAtlasFoundPath = assetsDirPath + "game/notexture_atlas.atlas"
    val noTextureFoundTexturePath = assetsDirPath + "game/notexture.png"
    val noSoundPath = assetsDirPath + "game/nosound.wav"
}