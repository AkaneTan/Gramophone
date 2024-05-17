package org.akanework.gramophone.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import org.akanework.gramophone.logic.GramophoneApplication
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.utils.LifecycleCallbackList
import org.akanework.gramophone.logic.utils.LifecycleCallbackListImpl

class MediaControllerViewModel(application: Application) : AndroidViewModel(application),
	DefaultLifecycleObserver, MediaController.Listener {

	private val context: GramophoneApplication
		get() = getApplication()
	private var controllerFuture: ListenableFuture<MediaController>? = null
	private val customCommandListenersImpl = LifecycleCallbackListImpl<
				(MediaController, SessionCommand, Bundle) -> ListenableFuture<SessionResult>>()
	private val disconnectionListenersImpl = LifecycleCallbackListImpl<() -> Unit>()
	private val connectionListenersImpl = LifecycleCallbackListImpl<(MediaController) -> Unit>()
	val customCommandListeners: LifecycleCallbackList<
				(MediaController, SessionCommand, Bundle) -> ListenableFuture<SessionResult>>
		get() = customCommandListenersImpl
	val disconnectionListeners: LifecycleCallbackList<() -> Unit>
		get() = disconnectionListenersImpl
	val connectionListeners: LifecycleCallbackList<(MediaController) -> Unit>
		get() = connectionListenersImpl

	override fun onStart(owner: LifecycleOwner) {
		val sessionToken =
			SessionToken(context, ComponentName(context, GramophonePlaybackService::class.java))
		controllerFuture =
			MediaController
				.Builder(context, sessionToken)
				.setListener(this)
				.buildAsync()
				.apply {
					addListener(
						{
							if (controllerFuture?.isDone == true &&
								controllerFuture?.isCancelled == false) {
								val instance = get()
								connectionListenersImpl.dispatch { it(instance) }
							}
						}, ContextCompat.getMainExecutor(context)
					)
				}
	}

	fun addOneOffControllerCallback(lifecycle: Lifecycle?, callback: (MediaController) -> Unit) {
		val instance = get()
		if (instance != null) {
			callback(instance)
		} else {
			connectionListeners.addCallback(lifecycle, true, callback)
		}
	}

	fun get(): MediaController? {
		if (controllerFuture?.isDone == true && controllerFuture?.isCancelled == false) {
			return controllerFuture!!.get()
		}
		return null
	}

	override fun onDisconnected(controller: MediaController) {
		controllerFuture = null
		disconnectionListenersImpl.dispatch { it() }
	}

	override fun onStop(owner: LifecycleOwner) {
		if (controllerFuture?.isDone == true) {
			if (controllerFuture?.isCancelled == false) {
				controllerFuture?.get()?.release()
			} else {
				throw IllegalStateException("controllerFuture?.isCancelled != false")
			}
		} else {
			controllerFuture?.cancel(true)
			controllerFuture = null
			disconnectionListenersImpl.dispatch { it() }
		}
	}

	override fun onDestroy(owner: LifecycleOwner) {
		ContextCompat.getMainExecutor(context).execute {
			customCommandListenersImpl.throwIfRelease()
			connectionListenersImpl.throwIfRelease()
			disconnectionListenersImpl.throwIfRelease()
		}
	}

	override fun onCustomCommand(
		controller: MediaController,
		command: SessionCommand,
		args: Bundle
	): ListenableFuture<SessionResult> {
		var future: ListenableFuture<SessionResult>? = null
		val listenerIterator = customCommandListenersImpl.iterator()
		while (future == null || (future.isDone &&
					future.get().resultCode == SessionResult.RESULT_ERROR_NOT_SUPPORTED)) {
			future = listenerIterator.next()(controller, command, args)
		}
		return future
	}
}