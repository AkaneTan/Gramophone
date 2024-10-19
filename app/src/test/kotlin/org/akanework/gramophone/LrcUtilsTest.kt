package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.LrcUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

class LrcUtilsTest {

	private fun parse(lrcContent: String, trim: Boolean? = null, multiline: Boolean? = null, mustSkip: Boolean? = false): MutableList<MediaStoreUtils.Lyric>? {
		if (trim == null) {
			val a = parse(lrcContent, true, multiline, mustSkip)
			val b = parse(lrcContent, false, multiline, mustSkip)
			assertEquals("trim true and false should result in same list for this string", b, a)
			return a
		}
		if (multiline == null) {
			val a = parse(lrcContent, trim, true, mustSkip)
			val b = parse(lrcContent, trim, false, mustSkip)
			assertEquals("multiline true and false should result in same list for this string (trim=$trim)", b, a)
			return a
		}
		val a = LrcUtils.parseLrcString(lrcContent, trim, multiline)
		if (mustSkip != null) {
			val b = listOf(MediaStoreUtils.Lyric(content = lrcContent))
			if (mustSkip) {
				assertEquals("excepted skip (trim=$trim multiline=$multiline)", b, a)
			} else {
				assertNotEquals("excepted no skip (trim=$trim multiline=$multiline)", b, a)
			}
		}
		return a
	}

	private fun lyricArrayToString(lrc: List<MediaStoreUtils.Lyric>?): String {
		val str = StringBuilder("val testData = ")
		if (lrc == null) {
			str.appendLine("null")
		} else {
			str.appendLine("listOf(")
			for (i in lrc) {
				str.appendLine("\tMediaStoreUtils.Lyric(timeStamp = ${i.timeStamp}, content = \"\"\"${i.content}\"\"\", isTranslation = ${i.isTranslation}),")
			}
			str.appendLine(")")
		}
		return str.toString()
	}

	@Test
	fun emptyInEmptyOut() {
		val emptyLrc = parse("")
		assertNull(emptyLrc)
	}

	@Test
	fun blankInEmptyOut() {
		val blankLrc = parse("   \t  \n    \u00A0")
		assertNull(blankLrc)
	}

	@Test
	fun testPrintUtility() {
		val lrc = lyricArrayToString(parse(LrcTestData.AS_IT_WAS))
		assertEquals(LrcTestData.AS_IT_WAS_PARSED_STR, lyricArrayToString(LrcTestData.AS_IT_WAS_PARSED))
		assertEquals(LrcTestData.AS_IT_WAS_PARSED_STR, lrc)
	}

	@Test
	fun testTemplateLrc1() {
		val lrc = parse(LrcTestData.AS_IT_WAS)
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	/*
	 * Test the synthetic newline feature. If this is intentionally broken, it's not a big deal.
	 * But don't break it accidentally.
	 */
	@Test
	fun testTemplateLrcSyntheticNewlines() {
		val lrc = parse(LrcTestData.AS_IT_WAS.replace("\n", ""))
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrc2() {
		val lrc = parse(LrcTestData.AS_IT_WAS + "\n")
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrcTrimToggle() {
		val a = parse(LrcTestData.AS_IT_WAS_NO_TRIM, trim = false)
		val b = parse(LrcTestData.AS_IT_WAS_NO_TRIM, trim = true)
		assertNotEquals(b, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_FALSE, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_TRUE, b)
	}

	@Test
	fun testTemplateLrcTranslate2Compressed() {
		val lrc = parse(LrcTestData.DREAM_THREAD)
		assertNotNull(lrc)
		assertEquals(LrcTestData.DREAM_THREAD_PARSED, lrc)
	}

	// TODO test if multiline works
	// TODO test if translations type 1 works
	// TODO test if extended lrc works
	// TODO test if extended lrc without normal sync points works
	// TODO test if wakaloke works
	// TODO test if apple music v1/v2/bg works
	// TODO test if [offset: ] works
	// TODO test if sorting out invalid lrc with 000000 works

	@Test
	fun testParserSkippedHello() {
		parse("hello", mustSkip = true)
	}

	@Test
	fun testParserSkipped2() {
		parse("2", mustSkip = true)
	}
}