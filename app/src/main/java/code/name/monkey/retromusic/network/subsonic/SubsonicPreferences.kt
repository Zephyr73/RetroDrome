package code.name.monkey.retromusic.network.subsonic

import android.content.Context
import android.content.SharedPreferences
import code.name.monkey.retromusic.App

object SubsonicPreferences {
    private const val PREFS_NAME = "retro_subsonic_prefs"
    private const val KEY_SERVER_URL = "subsonic_server_url"
    private const val KEY_USERNAME = "subsonic_username"
    private const val KEY_PASSWORD = "subsonic_password"
    private const val KEY_ENABLED = "subsonic_enabled"
    private const val KEY_ACTIVE_SOURCE = "subsonic_active_source"

    const val SOURCE_LOCAL = "local"
    const val SOURCE_SUBSONIC = "subsonic"

    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences {
        if (sharedPreferences == null) {
            try {
                init(App.getContext())
            } catch (e: Exception) {
                // Handled if App.getContext() throws
            }
        }
        return sharedPreferences ?: throw IllegalStateException("SubsonicPreferences not initialized")
    }

    var serverUrl: String
        get() = getPrefs().getString(KEY_SERVER_URL, "")?.trimEnd('/') ?: ""
        set(value) = getPrefs().edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var username: String
        get() = getPrefs().getString(KEY_USERNAME, "") ?: ""
        set(value) = getPrefs().edit().putString(KEY_USERNAME, value.trim()).apply()

    var password: String
        get() = getPrefs().getString(KEY_PASSWORD, "") ?: ""
        set(value) = getPrefs().edit().putString(KEY_PASSWORD, value).apply()

    var isEnabled: Boolean
        get() = getPrefs().getBoolean(KEY_ENABLED, false)
        set(value) = getPrefs().edit().putBoolean(KEY_ENABLED, value).apply()

    var activeSource: String
        get() = getPrefs().getString(KEY_ACTIVE_SOURCE, SOURCE_LOCAL) ?: SOURCE_LOCAL
        set(value) = getPrefs().edit().putString(KEY_ACTIVE_SOURCE, value).apply()

    const val FILTER_ALL = 0
    const val FILTER_LOCAL = 1
    const val FILTER_SERVER = 2
    private const val KEY_LIBRARY_FILTER = "subsonic_library_filter"

    var libraryFilter: Int
        get() = getPrefs().getInt(KEY_LIBRARY_FILTER, FILTER_ALL)
        set(value) = getPrefs().edit().putInt(KEY_LIBRARY_FILTER, value).apply()

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    private const val KEY_LAST_SERVER_TYPE = "subsonic_last_server_type"
    private const val KEY_LAST_SERVER_VERSION = "subsonic_last_server_version"

    var lastServerType: String
        get() = getPrefs().getString(KEY_LAST_SERVER_TYPE, "") ?: ""
        set(value) = getPrefs().edit().putString(KEY_LAST_SERVER_TYPE, value).apply()

    var lastServerVersion: String
        get() = getPrefs().getString(KEY_LAST_SERVER_VERSION, "") ?: ""
        set(value) = getPrefs().edit().putString(KEY_LAST_SERVER_VERSION, value).apply()
}
