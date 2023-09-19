package org.akanework.gramophone.logic.utils


class SupportComparator<T, U>(private val cmp: Comparator<U>,
                              private val invert: Boolean,
                              private val convert: (T) -> U)
	: Comparator<T> {
	override fun compare(o1: T, o2: T): Int {
		return cmp.compare(convert(o1), convert(o2)) * (if (invert) -1 else 1)
	}

	companion object {
		fun <T> createInversionComparator(cmp: Comparator<T>, invert: Boolean = false):
				Comparator<T> {
			return SupportComparator(cmp, invert) { it }
		}

		fun <T> createAlphanumericComparator(inverted: Boolean = false, converter: (T) -> CharSequence): Comparator<T> {
			return SupportComparator(
				AlphaNumericComparator(),
				inverted
			) { converter(it).toString() }
		}
	}
}