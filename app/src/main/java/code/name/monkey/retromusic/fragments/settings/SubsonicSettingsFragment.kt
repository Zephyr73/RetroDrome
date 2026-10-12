package code.name.monkey.retromusic.fragments.settings

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.network.subsonic.SubsonicPreferences
import code.name.monkey.retromusic.preferences.SubsonicStatusPreference
import code.name.monkey.retromusic.repository.SubsonicRepository
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SubsonicSettingsFragment : AbsSettingsFragment() {

    private val subsonicRepository: SubsonicRepository by inject()
    private val syncEngine: code.name.monkey.retromusic.db.catalog.MusicCatalogSyncEngine by inject()
    private val libraryViewModel by activityViewModel<LibraryViewModel>()
    private var connectionCheckJob: Job? = null

    override fun invalidateSettings() {
        val statusPref: SubsonicStatusPreference? = findPreference("subsonic_connection_status")
        statusPref?.onRefreshClickListener = {
            checkConnection(showToast = true)
        }

        val enabledPref: TwoStatePreference? = findPreference("subsonic_enabled")
        enabledPref?.apply {
            isChecked = SubsonicPreferences.isEnabled
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                SubsonicPreferences.isEnabled = enabled
                libraryViewModel.forceReload(ReloadType.All)
                if (enabled) {
                    checkConnection(showToast = false)
                } else {
                    updateStatusDisplay()
                }
                true
            }
        }

        updateStatusDisplay()

        val urlPref: Preference? = findPreference("subsonic_server_url")
        urlPref?.apply {
            summary = SubsonicPreferences.serverUrl.ifEmpty { getString(R.string.subsonic_server_url_summary) }
            setOnPreferenceClickListener {
                MaterialDialog(requireContext()).show {
                    title(res = R.string.subsonic_server_url_title)
                    input(
                        prefill = SubsonicPreferences.serverUrl,
                        hint = "http://100.x.y.z:4533",
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    ) { _, text ->
                        SubsonicPreferences.serverUrl = text.toString().trim()
                        invalidateSettings()
                        if (SubsonicPreferences.isConfigured) {
                            checkConnection(showToast = false)
                        }
                    }
                    positiveButton(res = R.string.save)
                    negativeButton(res = R.string.action_cancel)
                }
                true
            }
        }

        val userPref: Preference? = findPreference("subsonic_username")
        userPref?.apply {
            summary = SubsonicPreferences.username.ifEmpty { getString(R.string.subsonic_username_title) }
            setOnPreferenceClickListener {
                MaterialDialog(requireContext()).show {
                    title(res = R.string.subsonic_username_title)
                    input(
                        prefill = SubsonicPreferences.username,
                        hint = "Username"
                    ) { _, text ->
                        SubsonicPreferences.username = text.toString().trim()
                        invalidateSettings()
                        if (SubsonicPreferences.isConfigured) {
                            checkConnection(showToast = false)
                        }
                    }
                    positiveButton(res = R.string.save)
                    negativeButton(res = R.string.action_cancel)
                }
                true
            }
        }

        val passPref: Preference? = findPreference("subsonic_password")
        passPref?.apply {
            summary = if (SubsonicPreferences.password.isNotEmpty()) "••••••••" else "Not set"
            setOnPreferenceClickListener {
                MaterialDialog(requireContext()).show {
                    title(res = R.string.subsonic_password_title)
                    input(
                        prefill = SubsonicPreferences.password,
                        hint = "Password",
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    ) { _, text ->
                        SubsonicPreferences.password = text.toString()
                        invalidateSettings()
                        if (SubsonicPreferences.isConfigured) {
                            checkConnection(showToast = false)
                        }
                    }
                    positiveButton(res = R.string.save)
                    negativeButton(res = R.string.action_cancel)
                }
                true
            }
        }

        val testPref: Preference? = findPreference("subsonic_test_connection")
        testPref?.setOnPreferenceClickListener {
            checkConnection(showToast = true)
            true
        }
    }

    private fun updateStatusDisplay() {
        val statusPref: SubsonicStatusPreference = findPreference("subsonic_connection_status") ?: return
        when {
            !SubsonicPreferences.isConfigured -> {
                statusPref.setState(SubsonicStatusPreference.State.NOT_CONFIGURED)
            }
            !SubsonicPreferences.isEnabled -> {
                statusPref.setState(SubsonicStatusPreference.State.DISABLED)
            }
            else -> {
                if (SubsonicPreferences.lastServerType.isNotEmpty()) {
                    val summary = "${SubsonicPreferences.lastServerType} v${SubsonicPreferences.lastServerVersion} • ${SubsonicPreferences.serverUrl}"
                    statusPref.setState(SubsonicStatusPreference.State.CONNECTED, summary)
                } else {
                    statusPref.setState(SubsonicStatusPreference.State.CHECKING, "Connecting to ${SubsonicPreferences.serverUrl}…")
                }
            }
        }
    }

    private fun checkConnection(showToast: Boolean) {
        val statusPref: SubsonicStatusPreference? = findPreference("subsonic_connection_status")
        if (!SubsonicPreferences.isConfigured) {
            statusPref?.setState(SubsonicStatusPreference.State.NOT_CONFIGURED)
            if (showToast) {
                Toast.makeText(requireContext(), "Please configure URL, username, and password first", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val serverDisplay = SubsonicPreferences.serverUrl.ifEmpty { "server" }
        statusPref?.setState(
            SubsonicStatusPreference.State.CHECKING,
            "Connecting to $serverDisplay…"
        )

        connectionCheckJob?.cancel()
        connectionCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = subsonicRepository.checkConnection()
            if (!isAdded) return@launch

            if (result.success) {
                val serverName = result.serverType ?: "Subsonic"
                val versionStr = result.serverVersion?.let { " v$it" } ?: ""
                val summary = "$serverName$versionStr • $serverDisplay"
                SubsonicPreferences.lastServerType = serverName
                SubsonicPreferences.lastServerVersion = result.serverVersion ?: ""

                statusPref?.setState(SubsonicStatusPreference.State.CONNECTED, summary)
                if (showToast) {
                    Toast.makeText(
                        requireContext(),
                        "Connected to $serverName$versionStr",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    syncEngine.syncNavidromeCatalog()
                    libraryViewModel.forceReload(ReloadType.All)
                }
            } else {
                val errorMsg = result.errorMessage ?: "Unknown error"
                statusPref?.setState(
                    SubsonicStatusPreference.State.DISCONNECTED,
                    "$errorMsg • Tap to retry"
                )
                if (showToast) {
                    Toast.makeText(
                        requireContext(),
                        "Connection failed: $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_subsonic)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        invalidateSettings()
        if (SubsonicPreferences.isEnabled && SubsonicPreferences.isConfigured) {
            checkConnection(showToast = false)
        }
    }
}
