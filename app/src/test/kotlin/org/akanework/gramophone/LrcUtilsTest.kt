package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.LrcUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
	fun testA2Format() {
		val a2Lrc = "[00:12.00]This is <00:12.10>a <00:12.20>test <00:12.30>of <00:12.40>A2 <00:12.50>format"
		val parsed = parse(a2Lrc)
		assertNotNull(parsed)
		assertEquals(1, parsed?.size)
		val lyric = parsed?.get(0)
		assertEquals(12000L, lyric?.timeStamp)
		assertEquals("This is a test of A2 format", lyric?.content)
		assertEquals(
			listOf(
				Pair(0, 12100L),
				Pair(8, 12200L),
				Pair(10, 12300L),
				Pair(15, 12400L),
				Pair(18, 12500L)
			),
			lyric?.wordTimestamps
		)
	}

	@Test
	fun testA2FormatMixedWithStandard() {
		val mixedLrc = """
            [00:10.00]This is a standard line
            [00:12.00]This <00:12.10>is <00:12.20>an <00:12.30>A2 <00:12.40>line
            [00:14.00]Back to standard
        """.trimIndent()
		val parsed = parse(mixedLrc)
		assertNotNull(parsed)
		assertEquals(3, parsed?.size)

		assertEquals(10000L, parsed?.get(0)?.timeStamp)
		assertEquals("This is a standard line", parsed?.get(0)?.content)
		assertTrue(parsed?.get(0)?.wordTimestamps?.isEmpty() ?: false)

		assertEquals(12000L, parsed?.get(1)?.timeStamp)
		assertEquals("This is an A2 line", parsed?.get(1)?.content)
		assertEquals(
			listOf(
				Pair(0, 12100L),
				Pair(5, 12200L),
				Pair(8, 12300L),
				Pair(11, 12400L)
			),
			parsed?.get(1)?.wordTimestamps
		)

		assertEquals(14000L, parsed?.get(2)?.timeStamp)
		assertEquals("Back to standard", parsed?.get(2)?.content)
		assertTrue(parsed?.get(2)?.wordTimestamps?.isEmpty() ?: false)
	}

	@Test
	fun testA2FormatWithPartialWords() {
		val partialA2Lrc = "[00:12.00]This is <00:12.10>a test <00:12.30>of A2 <00:12.50>format"
		val parsed = parse(partialA2Lrc)
		assertNotNull(parsed)
		assertEquals(1, parsed?.size)
		val lyric = parsed?.get(0)
		assertEquals(12000L, lyric?.timeStamp)
		assertEquals("This is a test of A2 format", lyric?.content)
		assertEquals(
			listOf(
				Pair(0, 12100L),
				Pair(8, 12300L),
				Pair(15, 12500L)
			),
			lyric?.wordTimestamps
		)
	}

	@Test
	fun testA2FormatWithEmptyWords() {
		val emptyWordsA2Lrc = "[00:12.00]<00:12.10><00:12.20>Test<00:12.30><00:12.40>A2<00:12.50>"
		val parsed = parse(emptyWordsA2Lrc)
		assertNotNull(parsed)
		assertEquals(1, parsed?.size)
		val lyric = parsed?.get(0)
		assertEquals(12000L, lyric?.timeStamp)
		assertEquals("TestA2", lyric?.content)
		assertEquals(
			listOf(
				Pair(0, 12100L),
				Pair(0, 12200L),
				Pair(0, 12300L),
				Pair(4, 12400L),
				Pair(4, 12500L)
			),
			lyric?.wordTimestamps
		)
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

	@Ignore("idk why but its very broken")
	@Test
	fun testTemplateLrc2() {
		val lrc = parse(LrcTestData.AS_IT_WAS.replace("\n", ""))
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrc3() {
		// TODO re-enable multiline after fixing bug in legacy multiline parser where single
		//  newline is not handled the same
		val lrc = parse(LrcTestData.AS_IT_WAS + "\n", multiline = false)
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrc4() {
		val a = parse(LrcTestData.AS_IT_WAS_NO_TRIM, trim = false)
		val b = parse(LrcTestData.AS_IT_WAS_NO_TRIM, trim = true)
		assertNotEquals(b, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_FALSE, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_TRUE, b)
	}

	@Test
	fun testTemplateLrc5() {
		val lrc = parse(LrcTestData.DREAM_THREAD, multiline = false) // TODO multiline crashes
		assertNotNull(lrc)
		assertEquals(LrcTestData.DREAM_THREAD_PARSED, lrc)
	}

	@Test
	fun testParserSkippedHello() {
		parse("hello", mustSkip = true)
	}

	@Test
	fun testParserSkipped2() {
		parse("2", mustSkip = true)
	}
}