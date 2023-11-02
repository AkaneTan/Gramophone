package org.akanework.gramophone.logic.utils


class SupportComparator<T, U>(
    private val cmp: Comparator<U>,
    private val invert: Boolean,
    private val convert: (T) -> U
) : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
        return cmp.compare(convert(o1), convert(o2)) * (if (invert) -1 else 1)
    }

    companion object {
        fun <T> createDummyComparator(): Comparator<T> {
            return Comparator { _, _ -> 0 }
        }

        fun <T> createInversionComparator(cmp: Comparator<T>, invert: Boolean = false):
                Comparator<T> {
            if (!invert) return cmp
            return SupportComparator(cmp, true) { it }
        }

        fun <T> createAlphanumericComparator(
            inverted: Boolean = false,
            cnv: (T) -> CharSequence
        ): Comparator<T> {
            return SupportComparator(
                AlphaNumericComparator(),
                inverted
            ) { cnv(it).toString() }
        }
    }
}