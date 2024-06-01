package org.akanework.gramophone.logic.utils.exoplayer

import android.net.Uri
import androidx.annotation.GuardedBy
import androidx.annotation.OptIn
import androidx.media3.common.FileTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.Ac4Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(UnstableApi::class)
class GramophoneExtractorsFactory : ExtractorsFactory {
	companion object {
		private val DEFAULT_EXTRACTOR_ORDER =
			intArrayOf(5, 4, 12, 8, 3, 10, 9, 11, 6, 2, 0, 1, 7, 16, 15)
		private val FLAC_EXTENSION_LOADER =
			ExtensionLoader {
				val isFlacNativeLibraryAvailable =
					java.lang.Boolean.TRUE == Class.forName("androidx.media3.decoder.flac.FlacLibrary")
						.getMethod("isAvailable").invoke(null as Any?)
				if (isFlacNativeLibraryAvailable) Class.forName("androidx.media3.decoder.flac.FlacExtractor")
					.asSubclass(
						Extractor::class.java
					).getConstructor(Integer.TYPE) else null
			}
		private val MIDI_EXTENSION_LOADER =
			ExtensionLoader {
				Class.forName("androidx.media3.decoder.midi.MidiExtractor").asSubclass(
					Extractor::class.java
				).getConstructor()
			}
	}
	private var constantBitrateSeekingEnabled = false
	private var constantBitrateSeekingAlwaysEnabled = false
	private var adtsFlags = 0
	private var flacFlags = 0
	private var matroskaFlags = 0
	private var mp4Flags = 0
	private var fragmentedMp4Flags = 0
	private var mp3Flags = 0

	@Synchronized
	fun setConstantBitrateSeekingEnabled(constantBitrateSeekingEnabled: Boolean): GramophoneExtractorsFactory {
		this.constantBitrateSeekingEnabled = constantBitrateSeekingEnabled
		return this
	}

	@Synchronized
	fun setConstantBitrateSeekingAlwaysEnabled(constantBitrateSeekingAlwaysEnabled: Boolean): GramophoneExtractorsFactory {
		this.constantBitrateSeekingAlwaysEnabled = constantBitrateSeekingAlwaysEnabled
		return this
	}

	@Synchronized
	fun setAdtsExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.adtsFlags = flags
		return this
	}

	@Synchronized
	fun setFlacExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.flacFlags = flags
		return this
	}

	@Synchronized
	fun setMatroskaExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.matroskaFlags = flags
		return this
	}

	@Synchronized
	fun setMp4ExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.mp4Flags = flags
		return this
	}

	@Synchronized
	fun setFragmentedMp4ExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.fragmentedMp4Flags = flags
		return this
	}

	@Synchronized
	fun setMp3ExtractorFlags(flags: Int): GramophoneExtractorsFactory {
		this.mp3Flags = flags
		return this
	}

	@Synchronized
	override fun createExtractors(): Array<out Extractor> {
		return this.createExtractors(Uri.EMPTY, hashMapOf())
	}

	@Synchronized
	override fun createExtractors(
		uri: Uri,
		responseHeaders: MutableMap<String, List<String>>
	): Array<out Extractor> {
		val extractors: MutableList<Extractor> = ArrayList(DEFAULT_EXTRACTOR_ORDER.size)
		val responseHeadersInferredFileType =
			FileTypes.inferFileTypeFromResponseHeaders(responseHeaders)
		if (responseHeadersInferredFileType != -1) {
			this.addExtractorsForFileType(responseHeadersInferredFileType, extractors)
		}

		val uriInferredFileType = FileTypes.inferFileTypeFromUri(uri)
		if (uriInferredFileType != -1 && uriInferredFileType != responseHeadersInferredFileType) {
			this.addExtractorsForFileType(uriInferredFileType, extractors)
		}


		for (fileType in DEFAULT_EXTRACTOR_ORDER) {
			if (fileType != responseHeadersInferredFileType && fileType != uriInferredFileType) {
				this.addExtractorsForFileType(fileType, extractors)
			}
		}

		return extractors.toTypedArray()
	}

	private fun addExtractorsForFileType(fileType: Int, extractors: MutableList<Extractor>) {
		when (fileType) {
			0 -> extractors.add(Ac3Extractor())
			1 -> extractors.add(Ac4Extractor())
			2 -> extractors.add(AdtsExtractor(this.adtsFlags or (if (this.constantBitrateSeekingEnabled) AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING else 0) or (if (this.constantBitrateSeekingAlwaysEnabled) AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING_ALWAYS else 0)))
			4 -> {
				val flacExtractor = FLAC_EXTENSION_LOADER.getExtractor(this.flacFlags)
				if (flacExtractor != null) {
					extractors.add(flacExtractor)
				} else {
					extractors.add(FlacExtractor(this.flacFlags))
				}
			}
			6 -> extractors.add(
				MatroskaExtractor(
					SubtitleParser.Factory.UNSUPPORTED,
					this.matroskaFlags or MatroskaExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA
				)
			)
			7 -> extractors.add(Mp3Extractor(this.mp3Flags or (if (this.constantBitrateSeekingEnabled) Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING else 0) or (if (this.constantBitrateSeekingAlwaysEnabled) Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING_ALWAYS else 0)))
			8 -> {
				extractors.add(
					FragmentedMp4Extractor(
						SubtitleParser.Factory.UNSUPPORTED,
						this.fragmentedMp4Flags or FragmentedMp4Extractor.FLAG_EMIT_RAW_SUBTITLE_DATA
					)
				)
				extractors.add(
					Mp4Extractor(
						SubtitleParser.Factory.UNSUPPORTED,
						this.mp4Flags or Mp4Extractor.FLAG_EMIT_RAW_SUBTITLE_DATA
					)
				)
			}
			9 -> extractors.add(OggExtractor())
			12 -> extractors.add(WavExtractor())
			15 -> {
				val midiExtractor = MIDI_EXTENSION_LOADER.getExtractor()
				if (midiExtractor != null) {
					extractors.add(midiExtractor)
				}
			}

			else -> {}
		}
	}

	private class ExtensionLoader(private val constructorSupplier: ConstructorSupplier) {
		private val extensionLoaded = AtomicBoolean(false)

		@GuardedBy("extensionLoaded")
		private val extractorConstructor: Constructor<out Extractor>? = null

		fun getExtractor(vararg constructorParams: Any?): Extractor? {
			val extractorConstructor = this.maybeLoadExtractorConstructor()
			return if (extractorConstructor == null) {
				null
			} else {
				try {
					extractorConstructor.newInstance(*constructorParams) as Extractor
				} catch (var4: Exception) {
					throw IllegalStateException("Unexpected error creating extractor", var4)
				}
			}
		}

		private fun maybeLoadExtractorConstructor(): Constructor<out Extractor>? {
			synchronized(this.extensionLoaded) {
				if (extensionLoaded.get()) {
					return this.extractorConstructor
				} else {
					val var10000: Constructor<*>?
					try {
						var10000 = constructorSupplier.getConstructor()
					} catch (var4: ClassNotFoundException) {
						extensionLoaded.set(true)
						return this.extractorConstructor
					} catch (var5: Exception) {
						throw RuntimeException("Error instantiating extension", var5)
					}

					return var10000
				}
			}
		}
	}

	fun interface ConstructorSupplier {
		@Throws(
			InvocationTargetException::class,
			IllegalAccessException::class,
			NoSuchMethodException::class,
			ClassNotFoundException::class
		)
		fun getConstructor(): Constructor<out Extractor?>?
	}
}