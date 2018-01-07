package be.catvert.pc.i18n

enum class MenusText(val key: String) {
    MM_WINDOW_TITLE("mm_windowTitle"),
    MM_PLAY_BUTTON("mm_playButton"),
    MM_SETTINGS_BUTTON("mm_settingsButton"),
    MM_EXIT_BUTTON("mm_exitButton"),

    MM_CONFIRM("mm_confirm"),
    MM_NAME("mm_name"),

    MM_SELECT_LEVEL_WINDOW_TITLE("mm_selectLevel_windowTitle"),
    MM_SELECT_LEVEL_LEVEL_COMBO("mm_selectLevel_level"),
    MM_SELECT_LEVEL_PLAY_BUTTON("mm_selectLevel_playButton"),
    MM_SELECT_LEVEL_EDIT_BUTTON("mm_selectLevel_editButton"),
    MM_SELECT_LEVEL_NEW_BUTTON("mm_selectLevel_newButton"),
    MM_SELECT_LEVEL_COPY_BUTTON("mm_selectLevel_copyButton"),
    MM_SELECT_LEVEL_DELETE_BUTTON("mm_selectLevel_deleteButton"),

    MM_SELECT_LEVEL_NEW_LEVEL_CREATE("mm_selectLevel_newLevelCreate"),

    MM_SETTINGS_WINDOW_TITLE("mm_settingsWindowTitle"),
    MM_SETTINGS_SCREEN_SIZE("mm_settingsScreenSize"),
    MM_SETTINGS_FULLSCREEN("mm_settingsFullscreen"),
    MM_SETTINGS_SOUND("mm_settingsSound"),
    MM_SETTINGS_DARK_INTERFACE("mm_settingsDarkInterface"),
    MM_SETTINGS_LOCALE("mm_settingsLocale"),
    MM_SETTINGS_PRESSKEY("mm_settingsPressKey"),
    MM_SETTINGS_APPLY("mm_settingsApply"),

    MM_ERROR_LEVEL_POPUP("mm_errorLevel"),
    MM_ERROR_LEVEL_CLOSE("mm_errorLevelClose");

    operator fun invoke(vararg args: Any) = Locales.get(this, *args)
}