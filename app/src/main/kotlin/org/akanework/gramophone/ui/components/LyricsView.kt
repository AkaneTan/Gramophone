package org.akanework.gramophone.ui.components

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.CustomSmoothScroller
import org.akanework.gramophone.logic.ui.CustomSmoothScroller.SNAP_TO_START
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity

class LyricsView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
	SharedPreferences.OnSharedPreferenceChangeListener {

	companion object {
		const val LYRIC_REMOVE_HIGHLIGHT = 0
		const val LYRIC_SET_HIGHLIGHT = 1
		const val LYRIC_SCROLL_DURATION: Long = 650
	}
	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val activity
		get() = context as MainActivity
	private val instance
		get() = activity.getPlayer()
	private val recyclerView: MyRecyclerView
	private val adapter = LyricAdapter()
	private var animationLock = false

	init {
		adapter.updateLyricStatus()
		inflate(context, R.layout.lyric_view, this)
		recyclerView = findViewById(R.id.recycler_view)
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(LyricPaddingDecoration(context))
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		prefs.registerOnSharedPreferenceChangeListener(this)
		adapter.updateLyricStatus()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		prefs.unregisterOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == "lyric_center" || key == "lyric_bold" || key == "lyric_contrast") {
			adapter.updateLyricStatus()
			@Suppress("NotifyDataSetChanged")
			adapter.notifyDataSetChanged()
		}
	}

	inner class LyricAdapter() : MyRecyclerView.Adapter<LyricAdapter.ViewHolder>() {

		private val interpolator = PathInterpolator(0.4f, 0.2f, 0f, 1f)
		val lyricList = mutableListOf<MediaStoreUtils.Lyric>()
		internal var defaultTextColor = 0
		internal var contrastTextColor = 0
		internal var highlightTextColor = 0
		internal var currentFocusPos = -1
			private set
		private var currentTranslationPos = -1
		private var isBoldEnabled = false
		private var isLyricCentered = false
		private var isLyricContrastEnhanced = false
		private val sizeFactor = 1f
		private val defaultSizeFactor = .97f

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int
		): ViewHolder =
			ViewHolder(
				LayoutInflater
					.from(parent.context)
					.inflate(R.layout.lyrics, parent, false),
			)

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			throw IllegalStateException()
		}

		override fun onBindViewHolder(
			holder: ViewHolder,
			position: Int,
			payloads: MutableList<Any>
		) {
			val lyric = lyricList[position]
			val isHighlightPayload =
				payloads.isNotEmpty() && (payloads[0] == LYRIC_SET_HIGHLIGHT || payloads[0] == LYRIC_REMOVE_HIGHLIGHT)

			with(holder.lyricCard) {
				setOnClickListener {
					lyric.timeStamp?.let { it1 ->
						ViewCompat.performHapticFeedback(
							it,
							HapticFeedbackConstantsCompat.CONTEXT_CLICK
						)
						val instance = activity.getPlayer()
						if (instance?.isPlaying == false) {
							instance.play()
						}
						instance?.seekTo(it1)
					}
				}
			}

			with(holder.lyricTextView) {
				visibility = if (lyric.content.isNotEmpty()) VISIBLE else GONE
				text = lyric.content
				gravity = if (isLyricCentered) Gravity.CENTER else Gravity.START
				translationY = 0f

				val textSize = if (lyric.isTranslation) 20f else 34.25f
				val paddingTop = if (lyric.isTranslation) 2 else 18
				val paddingBottom =
					if (position + 1 < lyricList.size && lyricList[position + 1].isTranslation) 2 else 18

				if (isBoldEnabled) {
					this.typeface = TypefaceCompat.create(context,null, 700, false)
				} else {
					this.typeface = TypefaceCompat.create(context, null, 500, false)
				}

				if (isLyricCentered) {
					this.gravity = Gravity.CENTER
				} else {
					this.gravity = Gravity.START
				}

				setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
				setPadding(
					(12.5f).dpToPx(context).toInt(),
					paddingTop.dpToPx(context),
					(12.5f).dpToPx(context).toInt(),
					paddingBottom.dpToPx(context)
				)

				doOnLayout {
					pivotX = 0f
					pivotY = height / 2f
				}

				when {
					isHighlightPayload -> {
						val targetScale =
							if (payloads[0] == LYRIC_SET_HIGHLIGHT) sizeFactor else defaultSizeFactor
						val targetColor =
							if (payloads[0] == LYRIC_SET_HIGHLIGHT)
								highlightTextColor
							else
								defaultTextColor
						animateText(targetScale, targetColor)
					}

					position == currentFocusPos || position == currentTranslationPos -> {
						scaleText(sizeFactor)
						setTextColor(highlightTextColor)
					}

					else -> {
						scaleText(defaultSizeFactor)
						setTextColor(defaultTextColor)
					}
				}
			}
		}

		private fun TextView.animateText(targetScale: Float, targetColor: Int) {
			val animator = ValueAnimator.ofFloat(scaleX, targetScale)
			animator.addUpdateListener { animation ->
				val animatedValue = animation.animatedValue as Float
				scaleX = animatedValue
				scaleY = animatedValue
			}
			animator.duration = LYRIC_SCROLL_DURATION
			animator.interpolator = interpolator
			animator.start()

			val colorAnimator = ValueAnimator.ofArgb(textColors.defaultColor, targetColor)
			colorAnimator.addUpdateListener { animation ->
				val animatedValue = animation.animatedValue as Int
				setTextColor(animatedValue)
			}
			colorAnimator.duration = LYRIC_SCROLL_DURATION
			colorAnimator.interpolator = interpolator
			colorAnimator.start()
		}

		private fun TextView.scaleText(scale: Float) {
			scaleX = scale
			scaleY = scale
		}

		override fun onAttachedToRecyclerView(recyclerView: MyRecyclerView) {
			super.onAttachedToRecyclerView(recyclerView)
			updateLyricStatus()
		}

		fun updateLyricStatus() {
			isBoldEnabled = prefs.getBooleanStrict("lyric_bold", false)
			isLyricCentered = prefs.getBooleanStrict("lyric_center", false)
			isLyricContrastEnhanced = prefs.getBooleanStrict("lyric_contrast", false)
		}

		override fun getItemCount(): Int = lyricList.size

		inner class ViewHolder(
			view: View
		) : RecyclerView.ViewHolder(view) {
			val lyricTextView: TextView = view.findViewById(R.id.lyric)
			val lyricCard: MaterialCardView = view.findViewById(R.id.cardview)
		}

		internal fun updateHighlight(position: Int) {
			if (currentFocusPos == position) return
			if (position >= 0) {
				currentFocusPos.let {
					notifyItemChanged(it, LYRIC_REMOVE_HIGHLIGHT)
					currentFocusPos = position
					notifyItemChanged(currentFocusPos, LYRIC_SET_HIGHLIGHT)
				}

				if (position + 1 < lyricList.size &&
					lyricList[position + 1].isTranslation
				) {
					currentTranslationPos.let {
						notifyItemChanged(it, LYRIC_REMOVE_HIGHLIGHT)
						currentTranslationPos = position + 1
						notifyItemChanged(currentTranslationPos, LYRIC_SET_HIGHLIGHT)
					}
				} else if (currentTranslationPos != -1) {
					notifyItemChanged(currentTranslationPos, LYRIC_REMOVE_HIGHLIGHT)
					currentTranslationPos = -1
				}
			} else {
				currentFocusPos = -1
				currentTranslationPos = -1
			}
		}
	}

	private fun createSmoothScroller(noAnimation: Boolean = false): RecyclerView.SmoothScroller {
		return object : CustomSmoothScroller(context) {

			override fun calculateDtToFit(
				viewStart: Int,
				viewEnd: Int,
				boxStart: Int,
				boxEnd: Int,
				snapPreference: Int
			): Int {
				return super.calculateDtToFit(
					viewStart,
					viewEnd,
					boxStart,
					boxEnd,
					snapPreference
				) + 72.dpToPx(context)
			}

			override fun getVerticalSnapPreference(): Int {
				return SNAP_TO_START
			}

			override fun calculateTimeForDeceleration(dx: Int): Int {
				return LYRIC_SCROLL_DURATION.toInt()
			}

			override fun afterTargetFound() {
				if (targetPosition > 1) {
					val firstVisibleItemPosition = targetPosition + 1
					val lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() + 2
					for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
						val view: View? = layoutManager!!.findViewByPosition(i)
						if (view != null) {
							if (i == targetPosition + 1 && adapter.lyricList[i].isTranslation) {
								continue
							}
							if (!noAnimation) {
								val ii = i - firstVisibleItemPosition -
										if (adapter.lyricList[i].isTranslation) 1 else 0
								applyAnimation(view, ii)
							}
						}
					}
				}
			}
		}
	}

	private fun applyAnimation(view: View, ii: Int) {
		val depth = 15.dpToPx(context).toFloat()
		val duration = (LYRIC_SCROLL_DURATION * 0.278).toLong()
		val durationReturn = (LYRIC_SCROLL_DURATION * 0.722).toLong()
		val durationStep = (LYRIC_SCROLL_DURATION * 0.1).toLong()
		val animator = ObjectAnimator.ofFloat(
			view,
			"translationY",
			0f,
			depth,
		)
		animator.setDuration(duration)
		animator.interpolator = PathInterpolator(0.96f, 0.43f, 0.72f, 1f)
		animator.doOnEnd {
			val animator1 = ObjectAnimator.ofFloat(
				view,
				"translationY",
				depth,
				0f
			)
			animator1.setDuration(durationReturn + ii * durationStep)
			animator1.interpolator = PathInterpolator(0.17f, 0f, -0.15f, 1f)
			animator1.start()
		}
		animator.start()
	}

	fun resetToDefaultLyricPosition() {
		val smoothScroller = createSmoothScroller()
		smoothScroller.targetPosition = 0
		recyclerView.layoutManager!!.startSmoothScroll(
			smoothScroller
		)
		adapter.updateHighlight(0)
	}


	fun updateLyric(duration: Long?) {
		if (adapter.lyricList.isNotEmpty()) {
			val newIndex = updateNewIndex()

			if (newIndex != -1 &&
				duration != null &&
				newIndex != adapter.currentFocusPos
			) {
				if (adapter.lyricList[newIndex].content.isNotEmpty()) {
					val smoothScroller = createSmoothScroller(animationLock || newIndex == 0)
					smoothScroller.targetPosition = newIndex
					recyclerView.layoutManager!!.startSmoothScroll(
						smoothScroller
					)
					if (animationLock) animationLock = false
				}

				adapter.updateHighlight(newIndex)
			}
		}
	}

	private fun updateNewIndex(): Int {
		val filteredList = adapter.lyricList.filterIndexed { _, lyric ->
			(lyric.timeStamp ?: 0) <= (instance?.currentPosition ?: 0)
		}

		return if (filteredList.isNotEmpty()) {
			filteredList.indices.maxBy {
				filteredList[it].timeStamp ?: 0
			}
		} else {
			-1
		}
	}

	fun updateLyrics(parsedLyrics: MutableList<MediaStoreUtils.Lyric>?) {
		if (adapter.lyricList != parsedLyrics) {
			adapter.lyricList.clear()
			if (parsedLyrics?.isEmpty() != false) {
				adapter.lyricList.add(
					MediaStoreUtils.Lyric(
						null,
						context.getString(R.string.no_lyric_found)
					)
				)
			} else {
				adapter.lyricList.addAll(parsedLyrics)
			}
			@SuppressLint("NotifyDataSetChanged")
			adapter.notifyDataSetChanged()
			resetToDefaultLyricPosition()
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	fun updateTextColor(newColorContrast: Int, newColor: Int, newHighlightColor: Int) {
		adapter.defaultTextColor = newColor
		adapter.contrastTextColor = newColorContrast
		adapter.highlightTextColor = newHighlightColor
		adapter.notifyDataSetChanged()
	}
}