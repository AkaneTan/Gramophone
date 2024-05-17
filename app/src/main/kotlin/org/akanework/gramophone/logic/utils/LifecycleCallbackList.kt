package org.akanework.gramophone.logic.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

interface LifecycleCallbackList<T> {
	fun addCallbackForever(clear: Boolean = false, callback: T) {
		addCallback(null, clear, callback)
	}
	fun addCallback(lifecycle: Lifecycle?, clear: Boolean = false, callback: T)
	fun removeCallback(callback: T)
}

class LifecycleCallbackListImpl<T>(lifecycle: Lifecycle? = null)
	: LifecycleCallbackList<T>, DefaultLifecycleObserver {
	private var list = hashMapOf<T, Pair<Boolean, CallbackLifecycleObserver?>>()

	init {
		lifecycle?.addObserver(this)
	}

	override fun addCallback(lifecycle: Lifecycle?, clear: Boolean, callback: T) {
		if (list.containsKey(callback)) throw IllegalArgumentException("cannot add same callback twice")
		list[callback] = Pair(clear, lifecycle?.let { CallbackLifecycleObserver(it, callback) })
	}

	override fun removeCallback(callback: T) {
		list.remove(callback)?.second?.release()
	}

	fun dispatch(callback: (T) -> Unit) {
		list.forEach { callback(it.key) }
		list = HashMap(list.filterValues { !it.first })
	}

	fun release() {
		dispatch { removeCallback(it) }
	}

	fun throwIfRelease() {
		if (list.size == 0) return
		release()
		throw IllegalStateException("Callbacks leaked in LifecycleCallbackList")
	}

	fun iterator(): Iterator<T> {
		return list.keys.iterator()
	}

	override fun onDestroy(owner: LifecycleOwner) {
		release()
	}

	private inner class CallbackLifecycleObserver(private val lifecycle: Lifecycle,
	                                              private val callback: T)
		: DefaultLifecycleObserver {

		init {
			lifecycle.addObserver(this)
		}

		override fun onDestroy(owner: LifecycleOwner) {
			removeCallback(callback)
		}

		fun release() {
			lifecycle.removeObserver(this)
		}
	}
}