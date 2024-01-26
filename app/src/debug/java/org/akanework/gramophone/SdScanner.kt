package org.akanework.gramophone

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import androidx.core.util.Consumer


/**
 * Rewrite of jerickson314's great SD Scanner for Lollipop as utility class
 * TODO: well why is it not working?
 * https://github.com/jerickson314/sdscanner/blob/master/src/com/gmail/jerickson314/sdscanner/ScanFragment.java
 */
class SdScanner(private val context: Context) {
	var progressFrequencyMs = 250
	val progress = SimpleProgress()
	private val filesToProcess = hashSetOf<File>()
	private var lastUpdate = 0L
	private var root: File? = null
	private var ignoreDb: Boolean? = null

	fun scan(inRoot: File, inIgnoreDb: Boolean) {
		root = inRoot
		this.ignoreDb = inIgnoreDb
		lastUpdate = System.currentTimeMillis()
		progress.set(
			SimpleProgress.Step.DIR_SCAN, root!!.path, null)
		recursiveAddFiles(root!!)
		context.contentResolver.query(
			MediaStore.Files.getContentUri("external"),
			arrayOf(
				MediaStore.MediaColumns.DATA,
				MediaStore.MediaColumns.DATE_MODIFIED
			),
			null,
			null,
			null
		)?.use { cursor ->
			val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
			val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
			val totalSize = cursor.count
			while (cursor.moveToNext()) {
				val mediaFile = File(cursor.getString(dataColumn)).getCanonicalFile()
				System.currentTimeMillis().apply {
					if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
						lastUpdate = this
						progress.set(
							SimpleProgress.Step.DATABASE, mediaFile.path,
							(100 * cursor.position) / totalSize)
					}
				}
				if ((!mediaFile.exists() ||
							mediaFile.lastModified() / 1000L >
							cursor.getLong(modifiedColumn))
					&& shouldScan(mediaFile, true)) {
					filesToProcess.add(mediaFile)
				} else {
					filesToProcess.remove(mediaFile)
				}
			}
		}
		if (filesToProcess.size == 0) {
			scannerEnded()
		} else {
			val pathsToProcess = filesToProcess.map { it.absolutePath }.toMutableList()
			MediaScannerConnection.scanFile(
				context,
				pathsToProcess.toTypedArray(),
				null) { path: String, _: Uri ->
				if (!pathsToProcess.remove(path)) {
					Log.w("SdScanner", "Android scanned $path but we never asked it to do so")
				}
				System.currentTimeMillis().apply {
					if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
						lastUpdate = this
						progress.set(
							SimpleProgress.Step.SCAN, path,
							(100 * (filesToProcess.size - pathsToProcess.size))
									/ filesToProcess.size
						)
					}
				}
				if (pathsToProcess.isEmpty()) {
					scannerEnded()
				}
			}
		}
	}

	private fun scannerEnded() {
		progress.set(SimpleProgress.Step.DONE, null, 100)
		progress.reset()
		filesToProcess.clear()
		root = null
		ignoreDb = null
	}

	@Throws(IOException::class)
	private fun recursiveAddFiles(file: File) {
		System.currentTimeMillis().apply {
			if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
				lastUpdate = this
				progress.set(
					SimpleProgress.Step.DIR_SCAN, file.path, null)
			}
		}
		if (!shouldScan(file, false)) {
			// If we got here, there file was either outside the scan
			// directory, or was an empty directory.
			return
		}
		if (!filesToProcess.add(file)) {
			// Avoid infinite recursion caused by symlinks.
			// If mFilesToProcess already contains this file, add() will
			// return false.
			return
		}
		if (!file.canRead()) {
			Log.w("SdScanner", "cannot read $file")
		}
		Log.w("SdScanner", " read $file")
		if (file.isDirectory()) {
			val nomedia = File(file, ".nomedia").exists()
			// Only recurse downward if not blocked by nomedia.
			if (!nomedia) {
				val files = file.listFiles()
				if (files != null) {
					for (nextFile in files) {
						recursiveAddFiles(
							nextFile.getCanonicalFile()
						)
					}
				}
			}
		}
	}

	@Throws(IOException::class)
	fun shouldScan(inFile: File?, fromDb: Boolean): Boolean {
		// Empty directory check.
		var file = inFile
		if (file!!.isDirectory()) {
			val files = file.listFiles()
			if (files.isNullOrEmpty()) {
				return false
			}
		}
		if (ignoreDb != false && fromDb) {
			return true
		}
		while (file != null) {
			if (file == root) {
				return true
			}
			file = file.getParentFile()
		}
		return false
	}

	fun cleanup() {
		progress.cleanup()
	}

	class SimpleProgress {
		var step = Step.NOT_STARTED
			private set
		var path: String? = null
			private set
		var percentage: Int? = null
			private set
		private val listeners = arrayListOf<Consumer<SimpleProgress>>()

		fun set(step: Step, path: String?, percentage: Int?) {
			this.step = step
			this.path = path
			this.percentage = percentage
			listeners.forEach { it.accept(this) }
		}

		fun addListener(listener: Consumer<SimpleProgress>) {
			listeners.add(listener)
		}

		fun removeListener(listener: Consumer<SimpleProgress>) {
			listeners.remove(listener)
		}

		fun reset() {
			step = Step.NOT_STARTED
			path = null
			percentage = null
		}

		fun cleanup() {
			listeners.clear()
		}

		enum class Step {
			NOT_STARTED, DONE, DATABASE, DIR_SCAN, SCAN
		}
	}

	companion object {
		fun scan(context: Context, root: File, ignoreDb: Boolean,
		         listener: Consumer<SimpleProgress>? = null) {
			val scanner = SdScanner(context)
			if (listener != null) {
				scanner.progress.addListener { t ->
					if (t?.step == SimpleProgress.Step.DONE) {
						// remove listener again to avoid leaking memory
						scanner.cleanup()
					}
					listener.accept(t)
				}
			}
			scanner.scan(root, ignoreDb)
		}
	}
}