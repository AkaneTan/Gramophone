package org.akanework.gramophone.logic.lrcdecode

import androidx.annotation.OptIn
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import java.nio.charset.Charset

// Class heavily based on MIT-licensed https://github.com/yoheimuta/ExoPlayerMusic/blob/77cfb989b59f6906b1170c9b2d565f9b8447db41/app/src/main/java/com/github/yoheimuta/amplayer/playback/UsltFrameDecoder.kt
// See http://id3.org/id3v2.4.0-frames
@OptIn(UnstableApi::class)
class UsltFrameDecoder {
	companion object {
		private const val ID3_TEXT_ENCODING_ISO_8859_1 = 0
		private const val ID3_TEXT_ENCODING_UTF_16 = 1
		private const val ID3_TEXT_ENCODING_UTF_16BE = 2
		private const val ID3_TEXT_ENCODING_UTF_8 = 3

		fun decode(id3Data: ParsableByteArray, frameSize: Int): String? {
			if (frameSize < 4) {
				// Frame is malformed.
				return null
			}

			val encoding = id3Data.readUnsignedByte()
			val charset = getCharsetName(encoding)

			val lang = ByteArray(3)
			id3Data.readBytes(lang, 0, 3) // language
			val rest = ByteArray(frameSize - 4)
			id3Data.readBytes(rest, 0, frameSize - 4)

			val descriptionEndIndex = indexOfEos(rest, 0, encoding)
			val textStartIndex = descriptionEndIndex + delimiterLength(encoding)
			val textEndIndex = indexOfEos(rest, textStartIndex, encoding)
			return decodeStringIfValid(rest, textStartIndex, textEndIndex, charset)
		}

		private fun getCharsetName(encodingByte: Int): Charset {
			val name = when (encodingByte) {
				ID3_TEXT_ENCODING_UTF_16 -> "UTF-16"
				ID3_TEXT_ENCODING_UTF_16BE -> "UTF-16BE"
				ID3_TEXT_ENCODING_UTF_8 -> "UTF-8"
				ID3_TEXT_ENCODING_ISO_8859_1 -> "ISO-8859-1"
				else -> "ISO-8859-1"
			}
			return Charset.forName(name)
		}

		private fun indexOfEos(data: ByteArray, fromIndex: Int, encoding: Int): Int {
			var terminationPos = indexOfZeroByte(data, fromIndex)

			// For single byte encoding charsets, we're done.
			if (encoding == ID3_TEXT_ENCODING_ISO_8859_1 || encoding == ID3_TEXT_ENCODING_UTF_8) {
				return terminationPos
			}

			// Otherwise ensure an even index and look for a second zero byte.
			while (terminationPos < data.size - 1) {
				if (terminationPos % 2 == 0 && data[terminationPos + 1] == 0.toByte()) {
					return terminationPos
				}
				terminationPos = indexOfZeroByte(data, terminationPos + 1)
			}

			return data.size
		}

		private fun indexOfZeroByte(data: ByteArray, fromIndex: Int): Int {
			for (i in fromIndex until data.size) {
				if (data[i] == 0.toByte()) {
					return i
				}
			}
			return data.size
		}

		private fun delimiterLength(encodingByte: Int): Int {
			return if (encodingByte == ID3_TEXT_ENCODING_ISO_8859_1 || encodingByte == ID3_TEXT_ENCODING_UTF_8)
				1
			else
				2
		}

		private fun decodeStringIfValid(data: ByteArray, from: Int, to: Int, charset: Charset): String {
			return if (to <= from || to > data.size) {
				""
			} else String(data, from, to - from, charset)
		}
	}
}