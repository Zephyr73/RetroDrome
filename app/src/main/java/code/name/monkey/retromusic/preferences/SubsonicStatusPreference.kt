package code.name.monkey.retromusic.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.accentColor

class SubsonicStatusPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    enum class State {
        CONNECTED,
        CHECKING,
        DISCONNECTED,
        DISABLED,
        NOT_CONFIGURED
    }

    private var currentState: State = State.DISABLED
    private var customSummary: String? = null
    var onRefreshClickListener: (() -> Unit)? = null

    init {
        layoutResource = R.layout.preference_subsonic_status
        isSelectable = true
    }

    fun setState(state: State, summary: String? = null) {
        currentState = state
        customSummary = summary
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val iconView = holder.findViewById(R.id.status_icon) as? ImageView
        val progressView = holder.findViewById(R.id.status_progress) as? ProgressBar
        val titleView = holder.findViewById(R.id.status_title) as? TextView
        val summaryView = holder.findViewById(R.id.status_summary) as? TextView
        val refreshBtn = holder.findViewById(R.id.btn_refresh) as? View

        progressView?.indeterminateTintList = ColorStateList.valueOf(context.accentColor())

        refreshBtn?.setOnClickListener {
            onRefreshClickListener?.invoke()
        }
        holder.itemView.setOnClickListener {
            onRefreshClickListener?.invoke()
        }

        val density = context.resources.displayMetrics.density
        val pad = (4 * density).toInt()

        when (currentState) {
            State.CONNECTED -> {
                progressView?.visibility = View.GONE
                iconView?.visibility = View.VISIBLE
                iconView?.setImageResource(R.drawable.ic_check_circle)

                val greenColor = COLOR_CONNECTED
                iconView?.let { iv ->
                    ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(greenColor))
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ColorUtils.setAlphaComponent(greenColor, 38))
                    }
                    iv.background = bg
                    iv.setPadding(pad, pad, pad, pad)
                }

                titleView?.text = context.getString(R.string.subsonic_status_connected)
                summaryView?.text = customSummary ?: context.getString(R.string.subsonic_status_online)
            }
            State.CHECKING -> {
                progressView?.visibility = View.VISIBLE
                iconView?.visibility = View.GONE

                titleView?.text = context.getString(R.string.subsonic_status_checking)
                summaryView?.text = customSummary ?: context.getString(R.string.subsonic_status_connecting)
            }
            State.DISCONNECTED -> {
                progressView?.visibility = View.GONE
                iconView?.visibility = View.VISIBLE
                iconView?.setImageResource(R.drawable.ic_close)

                val redColor = COLOR_DISCONNECTED
                iconView?.let { iv ->
                    ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(redColor))
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ColorUtils.setAlphaComponent(redColor, 38))
                    }
                    iv.background = bg
                    iv.setPadding(pad, pad, pad, pad)
                }

                titleView?.text = context.getString(R.string.subsonic_status_disconnected)
                summaryView?.text = customSummary ?: context.getString(R.string.subsonic_status_tap_to_retry)
            }
            State.DISABLED -> {
                progressView?.visibility = View.GONE
                iconView?.visibility = View.VISIBLE
                iconView?.setImageResource(R.drawable.ic_cloud)

                val grayColor = ATHUtil.resolveColor(context, android.R.attr.textColorSecondary)
                iconView?.let { iv ->
                    ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(grayColor))
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ColorUtils.setAlphaComponent(grayColor, 25))
                    }
                    iv.background = bg
                    iv.setPadding(pad, pad, pad, pad)
                }

                titleView?.text = context.getString(R.string.subsonic_status_disabled)
                summaryView?.text = context.getString(R.string.subsonic_status_enable_hint)
            }
            State.NOT_CONFIGURED -> {
                progressView?.visibility = View.GONE
                iconView?.visibility = View.VISIBLE
                iconView?.setImageResource(R.drawable.ic_info_outline)

                val grayColor = ATHUtil.resolveColor(context, android.R.attr.textColorSecondary)
                iconView?.let { iv ->
                    ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(grayColor))
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ColorUtils.setAlphaComponent(grayColor, 25))
                    }
                    iv.background = bg
                    iv.setPadding(pad, pad, pad, pad)
                }

                titleView?.text = context.getString(R.string.subsonic_status_not_configured)
                summaryView?.text = context.getString(R.string.subsonic_status_configure_hint)
            }
        }
    }

    companion object {
        const val COLOR_CONNECTED = 0xFF4CAF50.toInt()
        const val COLOR_DISCONNECTED = 0xFFF44336.toInt()
    }
}
