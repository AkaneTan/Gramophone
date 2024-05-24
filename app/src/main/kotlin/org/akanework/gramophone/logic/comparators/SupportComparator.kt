/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.logic.comparators

class SupportComparator<T, U>(
    private val cmp: Comparator<U>,
    private val fallback: Comparator<T>?,
    private val invert: Boolean,
    private val convert: (T) -> U
) : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
        val c1 = convert(o1)
        val c2 = convert(o2)
        val i = cmp.compare(c1, c2) * (if (invert) -1 else 1)
        if (i != 0) return i
        fallback?.let { return it.compare(o1, o2) }
        return 0
    }

    companion object {
        fun <T> createDummyComparator(): Comparator<T> {
            return Comparator { _, _ -> 0 }
        }

        fun <T> createInversionComparator(cmp: Comparator<T>, invert: Boolean = false, fallback: Comparator<T>? = null):
                Comparator<T> {
            if (!invert) return cmp
            return SupportComparator(cmp, fallback, true) { it }
        }

        fun <T> createAlphanumericComparator(
            inverted: Boolean = false,
            cnv: (T) -> CharSequence,
            fallback: Comparator<T>? = null
        ): Comparator<T> {
            return SupportComparator(
                AlphaNumericComparator(),
                fallback,
                inverted
            ) { cnv(it).toString() }
        }
    }
}