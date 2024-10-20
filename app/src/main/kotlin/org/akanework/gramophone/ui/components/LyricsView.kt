package org.akanework.gramophone.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SemanticLyrics

class LyricsView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
	SharedPreferences.OnSharedPreferenceChangeListener {


	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private var recyclerView: MyRecyclerView? = null
	private var newView: NewLyricsView? = null
	private val adapter
		get() = recyclerView?.adapter as LegacyLyricsAdapter?
	private var defaultTextColor = 0
	private var contrastTextColor = 0
	private var highlightTextColor = 0
	private var lyrics: SemanticLyrics? = null
	private var lyricsLegacy: MutableList<MediaStoreUtils.Lyric>? = null

	init {
		createView()
	}

	private fun createView() {
		removeAllViews()
		recyclerView = null
		newView = null
		if (prefs.getBooleanStrict("lyric_ui", false)) {
			inflate(context, R.layout.lyric_view_v2, this)
			newView = findViewById(R.id.lyric_view)
			newView?.updateTextColor(contrastTextColor, defaultTextColor, highlightTextColor)
			newView?.updateLyrics(lyrics)
		} else {
			inflate(context, R.layout.lyric_view, this)
			recyclerView = findViewById(R.id.recycler_view)
			recyclerView!!.adapter = LegacyLyricsAdapter(context).also {
				it.updateTextColor(contrastTextColor, defaultTextColor, highlightTextColor)
			}
			recyclerView!!.addItemDecoration(LyricPaddingDecoration(context))
			if (lyrics != null)
				adapter?.updateLyrics(SemanticLyrics.convertForLegacy(lyrics))
			else
				adapter?.updateLyrics(lyricsLegacy)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		prefs.registerOnSharedPreferenceChangeListener(this)
		adapter?.updateLyricStatus()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		prefs.unregisterOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == "lyric_center" || key == "lyric_bold" || key == "lyric_contrast") {
			adapter?.onPrefsChanged()
			newView?.onPrefsChanged()
		} else if (key == "lyric_ui") {
			createView()
		}
	}

	fun updateLyricPositionFromPlaybackPos() {
		adapter?.updateLyricPositionFromPlaybackPos()
		newView?.updateLyricPositionFromPlaybackPos()
	}

	fun updateLyrics(parsedLyrics: SemanticLyrics?) {
		lyrics = parsedLyrics
		lyricsLegacy = null
		adapter?.updateLyrics(SemanticLyrics.convertForLegacy(lyrics))
		newView?.updateLyrics(lyrics)
	}

	fun updateLyricsLegacy(parsedLyrics: MutableList<MediaStoreUtils.Lyric>?) {
		lyrics = null
		lyricsLegacy = parsedLyrics
		adapter?.updateLyrics(lyricsLegacy)
		newView?.updateLyrics(null)
	}

	fun updateTextColor(newColorContrast: Int, newColor: Int, newHighlightColor: Int) {
		defaultTextColor = newColor
		contrastTextColor = newColorContrast
		highlightTextColor = newHighlightColor
		adapter?.updateTextColor(contrastTextColor, defaultTextColor, highlightTextColor)
		newView?.updateTextColor(contrastTextColor, defaultTextColor, highlightTextColor)
	}
}