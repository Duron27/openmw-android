package org.openmw.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_files_prefs")

object GameFilesPreferences {
    val GAME_FILES_URI_KEY = stringPreferencesKey("game_files_uri")
    val NEXUS_API_KEY = stringPreferencesKey("nexus_api_key")
    val UI_HIDDEN_STATE_KEY = stringPreferencesKey("ui_hidden_state")
    val VIBRATION_STATE_KEY = stringPreferencesKey("vibration_state")
    val MATCH_ICON_COLOR_KEY = stringPreferencesKey("match_icon_color")
    val RESOLUTION_X_KEY = intPreferencesKey("resolution_x")
    val RESOLUTION_Y_KEY = intPreferencesKey("resolution_y")
    val ICON_GLOW_KEY = booleanPreferencesKey("icon_glow")
    val COMMAND_LINE_KEY = stringPreferencesKey("commandLine")
    val USER_OPTIONS_KEY = stringPreferencesKey("userOptions")
    val AUTO_MOUSE_MODE_KEY = stringPreferencesKey("autoMouseMode")
    val AVOID_16_BITS_KEY = booleanPreferencesKey("avoid16bits")
    val TEXTURE_SHRINKING_KEY = stringPreferencesKey("textureShrinking")
    val CODE_GROUP_KEY = stringPreferencesKey("whichGame")
    val CUSTOM_GL_DRIVER_KEY = stringPreferencesKey("custom_gl_driver")
    val BUTTON_GROUP_SWITCH_KEY = booleanPreferencesKey("button_group_switch")
    val SELECTED_ANIMATION_KEY = stringPreferencesKey("selected_animation")
    val SELECTED_MOUSE_KEYS = stringPreferencesKey("selected_mouse")
    val ALLOWED_TO_TEXT_EDITOR = stringPreferencesKey("allowed_to_edit")
    val NEW_FEATURE_ENABLED_KEY = booleanPreferencesKey("new_feature_enabled")
    private val _gameFilesUri = MutableStateFlow<String?>(null)
    val gameFilesUri: StateFlow<String?> = _gameFilesUri
    val BACKGROUND_ANIMATION_KEY = stringPreferencesKey("background_animation")
    val WHATS_NEW_KEY = booleanPreferencesKey("whats_new")
    val TUTORIAL_KEY = booleanPreferencesKey("tutorial_enable")

    // Android 15 bug where game files not detected when launching
    val BYPASS_GAME_FILES_KEY = booleanPreferencesKey("bypass_game_files_check")

    // Developer Options
    val AVOID_RESOLUTION_INSERTION = booleanPreferencesKey("avoidInsertion")

    // Mouse Settings
    val OFFSET_X_MOUSE = floatPreferencesKey("offset_x_mouse")
    val OFFSET_Y_MOUSE = floatPreferencesKey("offset_y_mouse")
    val SENSITIVITY_MOUSE = floatPreferencesKey("sensitivity_mouse")

    val SENSITIVITY_RT = floatPreferencesKey("sensitivity_right_thumb")

    suspend fun setOffsetXMouse(context: Context, offsetX: Float) {
        context.dataStore.edit { preferences ->
            preferences[OFFSET_X_MOUSE] = offsetX
        }
    }

    suspend fun setOffsetYMouse(context: Context, offsetY: Float) {
        context.dataStore.edit { preferences ->
            preferences[OFFSET_Y_MOUSE] = offsetY
        }
    }

    fun getOffsetXMouse(context: Context): Flow<Float?> {
        return context.dataStore.data.map { preferences ->
            preferences[OFFSET_X_MOUSE]
        }
    }

    fun getOffsetYMouse(context: Context): Flow<Float?> {
        return context.dataStore.data.map { preferences ->
            preferences[OFFSET_Y_MOUSE]
        }
    }

    suspend fun setSensitivityMouse(context: Context, sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[SENSITIVITY_MOUSE] = sensitivity
        }
    }

    fun getSensitivityMouse(context: Context): Flow<Float?> {
        return context.dataStore.data.map { preferences ->
            preferences[SENSITIVITY_MOUSE]
        }
    }

    suspend fun setSensitivityRT(context: Context, sensitivityRT: Float) {
        context.dataStore.edit { preferences ->
            preferences[SENSITIVITY_RT] = sensitivityRT
        }
    }

    fun getSensitivityRT(context: Context): Flow<Float?> {
        return context.dataStore.data.map { preferences ->
            preferences[SENSITIVITY_RT]
        }
    }

    fun getGameFilesUri(context: Context): String? {
        val dataStoreKey = GAME_FILES_URI_KEY
        val dataStore = context.dataStore
        return runBlocking {
            val preferences = dataStore.data.first()
            preferences[dataStoreKey]?.also {
                _gameFilesUri.value = it
            }
        }
    }

    fun getGameFilesUriState(context: Context): Flow<String?> {
        val dataStoreKey = GAME_FILES_URI_KEY
        val dataStore = context.dataStore
        return dataStore.data.map { preferences ->
            preferences[dataStoreKey]
        }
    }

    suspend fun storeGameFilesPath(context: Context, path: String) {
        context.dataStore.edit { preferences ->
            preferences[GAME_FILES_URI_KEY] = path
        }
        _gameFilesUri.value = path
    }

    suspend fun setCustomGLDriverPath(context: Context, customGLDriverPath: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_GL_DRIVER_KEY] = customGLDriverPath
        }
    }

    fun getCustomGLDriverPath(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_GL_DRIVER_KEY]
        }
    }

    suspend fun clearCustomGLDriverPath(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(CUSTOM_GL_DRIVER_KEY)
        }
    }

    suspend fun saveResolutionX(context: Context, resolutionX: Int) {
        context.dataStore.edit { preferences ->
            preferences[RESOLUTION_X_KEY] = resolutionX
        }
    }

    suspend fun saveResolutionY(context: Context, resolutionY: Int) {
        context.dataStore.edit { preferences ->
            preferences[RESOLUTION_Y_KEY] = resolutionY
        }
    }

    suspend fun saveUIState(context: Context, isHidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[UI_HIDDEN_STATE_KEY] = isHidden.toString()
        }
    }

    fun loadUIState(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[UI_HIDDEN_STATE_KEY]?.toBoolean() ?: false
        }
    }

    suspend fun saveVibrationState(context: Context, isHidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_STATE_KEY] = isHidden.toString()
        }
    }

    fun loadVibrationState(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[VIBRATION_STATE_KEY]?.toBoolean() ?: true
        }
    }

    suspend fun saveMatchIconColorState(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MATCH_ICON_COLOR_KEY] = isEnabled.toString()
        }
    }

    fun loadMatchIconColorState(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[MATCH_ICON_COLOR_KEY]?.toBoolean() ?: false
        }
    }
    suspend fun getAllPreferences(context: Context): Map<String, String> {
        val preferences = context.dataStore.data.first()
        return preferences.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
    }

    suspend fun saveIconGlow(context: Context, iconGlow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ICON_GLOW_KEY] = iconGlow
        }
    }

    fun loadIconGlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ICON_GLOW_KEY] ?: true
        }
    }

    suspend fun saveBypassGameCheck(context: Context, iconGlow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BYPASS_GAME_FILES_KEY] = iconGlow
        }
    }

    fun loadBypassGameCheck(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BYPASS_GAME_FILES_KEY] ?: false
        }
    }

    suspend fun saveCommandLine(context: Context, commandLine: String) {
        context.dataStore.edit { preferences ->
            preferences[COMMAND_LINE_KEY] = commandLine
        }
    }

    fun getCommandLine(context: Context): Flow<String?> {
        return context.dataStore.data.map { settings ->
            settings[COMMAND_LINE_KEY]
        }
    }

    suspend fun saveUserOptions(context: Context, options: Set<String>) {
        val existingOptions = getUserOptions(context).first().filterNot { it.isEmpty() }
        val allOptions = (existingOptions + options).filterNot { it.isEmpty() }
        context.dataStore.edit { preferences ->
            preferences[USER_OPTIONS_KEY] = allOptions.joinToString(",")
        }
    }

    fun getUserOptions(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_OPTIONS_KEY]?.split(",")?.filterNot { it.isEmpty() }?.toSet() ?: emptySet()
        }
    }

    suspend fun deleteUserOption(context: Context, option: String) {
        val existingOptions = getUserOptions(context).first()
        val updatedOptions = existingOptions - option
        context.dataStore.edit { preferences ->
            preferences[USER_OPTIONS_KEY] = updatedOptions.joinToString(",")
        }
    }

    suspend fun saveAutoMouseMode(context: Context, autoMouseMode: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_MOUSE_MODE_KEY] = autoMouseMode
        }
    }

    fun loadAutoMouseMode(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_MOUSE_MODE_KEY] ?: "Hybrid"
        }
    }

    fun readAvoid16Bits(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences ->
                preferences[AVOID_16_BITS_KEY] ?: false
            }
    }

    suspend fun writeAvoid16Bits(context: Context, avoid16bits: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AVOID_16_BITS_KEY] = avoid16bits
        }
    }

    fun readResolutionInsertion(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences ->
                preferences[AVOID_RESOLUTION_INSERTION] ?: false
            }
    }

    suspend fun writeResolutionInsertion(context: Context, avoidInsertion: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AVOID_RESOLUTION_INSERTION] = avoidInsertion
        }
    }

    fun readTextureShrinkingOption(context: Context): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[TEXTURE_SHRINKING_KEY] ?: "None"
            }
    }

    suspend fun writeTextureShrinkingOption(context: Context, option: String) {
        context.dataStore.edit { preferences ->
            preferences[TEXTURE_SHRINKING_KEY] = option
        }
    }

    fun readCodeGroup(context: Context): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[CODE_GROUP_KEY] ?: "OpenMW"
            }
    }

    suspend fun setCodeGroup(context: Context, option: String) {
        context.dataStore.edit { preferences ->
            preferences[CODE_GROUP_KEY] = option
        }
    }

    fun getButtonGroupSwitch(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BUTTON_GROUP_SWITCH_KEY] ?: false
        }
    }

    suspend fun setButtonGroupSwitch(context: Context, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BUTTON_GROUP_SWITCH_KEY] = value
        }
    }

    fun getSelectedAnimation(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_ANIMATION_KEY] ?: "None"
        }
    }

    suspend fun setSelectedAnimation(context: Context, animation: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_ANIMATION_KEY] = animation
        }
    }

    fun getSelectedKeycodes(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_MOUSE_KEYS] ?: "54,111"
        }
    }

    suspend fun setSelectedKeycodes(context: Context, animation: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MOUSE_KEYS] = animation
        }
    }

    fun getExtensionAllowedToEdit(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[ALLOWED_TO_TEXT_EDITOR] ?: "sh,txt,db,xml,cfg,json,py,yaml,lua,omwscripts,log,frag,vert,layout,glsl,omwfx"
        }
    }

    suspend fun setExtensionAllowedToEdit(context: Context, extensions: String) {
        context.dataStore.edit { preferences ->
            preferences[ALLOWED_TO_TEXT_EDITOR] = extensions
        }
    }

    suspend fun saveNewFeatureEnabledState(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NEW_FEATURE_ENABLED_KEY] = isEnabled
        }
    }

    fun loadNewFeatureEnabledState(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NEW_FEATURE_ENABLED_KEY] ?: false
        }
    }

    fun getBackgroundAnimation(context: Context): String? {
        val dataStore = context.dataStore
        return runBlocking {
            val preferences = dataStore.data.first()
            preferences[BACKGROUND_ANIMATION_KEY]
        }
    }
    fun getBackgroundAnimationFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[BACKGROUND_ANIMATION_KEY] ?: "None"
        }
    }

    suspend fun setBackgroundAnimation(context: Context, animation: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_ANIMATION_KEY] = animation
        }
    }

    suspend fun setWhatsNew(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WHATS_NEW_KEY] = isEnabled
        }
    }

    fun getWhatsNew(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[WHATS_NEW_KEY] != false
        }
    }

    suspend fun setTutorial(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TUTORIAL_KEY] = isEnabled
        }
    }

    fun getTutorial(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[TUTORIAL_KEY] != false
        }
    }
}

suspend fun initializePreferences(context: Context) {
    context.dataStore.edit { preferences ->
        if (!preferences.contains(GameFilesPreferences.UI_HIDDEN_STATE_KEY)) {
            preferences[GameFilesPreferences.UI_HIDDEN_STATE_KEY] = "false"
        }
        if (!preferences.contains(GameFilesPreferences.VIBRATION_STATE_KEY)) {
            preferences[GameFilesPreferences.VIBRATION_STATE_KEY] = "true"
        }
        if (!preferences.contains(GameFilesPreferences.MATCH_ICON_COLOR_KEY)) {
            preferences[GameFilesPreferences.MATCH_ICON_COLOR_KEY] = "false"
        }
        if (!preferences.contains(GameFilesPreferences.ICON_GLOW_KEY)) {
            preferences[GameFilesPreferences.ICON_GLOW_KEY] = true
        }
        if (!preferences.contains(GameFilesPreferences.BYPASS_GAME_FILES_KEY)) {
            preferences[GameFilesPreferences.BYPASS_GAME_FILES_KEY] = false
        }
        if (!preferences.contains(GameFilesPreferences.AUTO_MOUSE_MODE_KEY)) {
            preferences[GameFilesPreferences.AUTO_MOUSE_MODE_KEY] = "Hybrid"
        }
        if (!preferences.contains(GameFilesPreferences.AVOID_16_BITS_KEY)) {
            preferences[GameFilesPreferences.AVOID_16_BITS_KEY] = false
        }
        if (!preferences.contains(GameFilesPreferences.AVOID_RESOLUTION_INSERTION)) {
            preferences[GameFilesPreferences.AVOID_RESOLUTION_INSERTION] = false
        }
        if (!preferences.contains(GameFilesPreferences.TEXTURE_SHRINKING_KEY)) {
            preferences[GameFilesPreferences.TEXTURE_SHRINKING_KEY] = "None"
        }
        if (!preferences.contains(GameFilesPreferences.CODE_GROUP_KEY)) {
            preferences[GameFilesPreferences.CODE_GROUP_KEY] = "OpenMW"
        }
        if (!preferences.contains(GameFilesPreferences.BUTTON_GROUP_SWITCH_KEY)) {
            preferences[GameFilesPreferences.BUTTON_GROUP_SWITCH_KEY] = true
        }
        if (!preferences.contains(GameFilesPreferences.SELECTED_MOUSE_KEYS)) {
            preferences[GameFilesPreferences.SELECTED_MOUSE_KEYS] = "54,111"
        }
        if (!preferences.contains(GameFilesPreferences.SELECTED_ANIMATION_KEY)) {
            preferences[GameFilesPreferences.SELECTED_ANIMATION_KEY] = "None"
        }
        if (!preferences.contains(GameFilesPreferences.OFFSET_X_MOUSE)) {
            preferences[GameFilesPreferences.OFFSET_X_MOUSE] = 0f
        }
        if (!preferences.contains(GameFilesPreferences.OFFSET_Y_MOUSE)) {
            preferences[GameFilesPreferences.OFFSET_Y_MOUSE] = 0f
        }
        if (!preferences.contains(GameFilesPreferences.SENSITIVITY_MOUSE)) {
            preferences[GameFilesPreferences.SENSITIVITY_MOUSE] = 5000f
        }
        if (!preferences.contains(GameFilesPreferences.SENSITIVITY_RT)) {
            preferences[GameFilesPreferences.SENSITIVITY_RT] = 2500f
        }
        if (!preferences.contains(GameFilesPreferences.SELECTED_ANIMATION_KEY)) {
            preferences[GameFilesPreferences.SELECTED_ANIMATION_KEY] = "None"
        }
        if (!preferences.contains(GameFilesPreferences.NEW_FEATURE_ENABLED_KEY)) {
            preferences[GameFilesPreferences.NEW_FEATURE_ENABLED_KEY] = false
        }
        if (!preferences.contains(GameFilesPreferences.WHATS_NEW_KEY)) {
            preferences[GameFilesPreferences.WHATS_NEW_KEY] = true
        }
        if (!preferences.contains(GameFilesPreferences.TUTORIAL_KEY)) {
            preferences[GameFilesPreferences.TUTORIAL_KEY] = true
        }
        if (!preferences.contains(GameFilesPreferences.BACKGROUND_ANIMATION_KEY)) {
            preferences[GameFilesPreferences.BACKGROUND_ANIMATION_KEY] = "BouncingBackground"
        }
        if (!preferences.contains(GameFilesPreferences.ALLOWED_TO_TEXT_EDITOR)) {
            preferences[GameFilesPreferences.ALLOWED_TO_TEXT_EDITOR] = "sh,txt,db,xml,cfg,json,py,yaml,lua,omwscripts,log,frag,vert,layout,glsl,omwfx"
        }
    }
}
