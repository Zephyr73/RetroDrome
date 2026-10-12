@file:Suppress("UNUSED_PARAMETER", "unused")

package code.name.monkey.retromusic.extensions

import android.content.Context
import android.view.Menu
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R

fun Context.setUpMediaRouteButton(menu: Menu) {
    menu.findItem(R.id.action_cast)?.isVisible = false
}

fun FragmentActivity.installLanguageAndRecreate(code: String, onInstallComplete: () -> Unit) {
    onInstallComplete()
}

fun Context.installSplitCompat() {}