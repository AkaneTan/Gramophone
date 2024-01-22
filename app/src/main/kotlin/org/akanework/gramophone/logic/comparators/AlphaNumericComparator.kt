/*
 *     Copyright (C) 2016  Farbod Safaei
 *                   2024  Akane Foundation
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

import java.math.BigInteger
import java.text.Collator
import java.util.Locale
import java.util.Objects

/**
 *
 * An alphanumeric comparator for comparing strings in a human readable format.
 * It uses a combination numeric and alphabetic comparisons to compare two strings.
 * This class uses standard Java classes, independent of 3rd party libraries.
 *
 *
 *
 *
 * For given list of strings:
 * <blockquote><pre>
 * file-01.doc
 * file-2.doc
 * file-03.doc</pre></blockquote>
 *
 *
 * The regular lexicographical sort e.g. [java.util.Collections.sort] will result in a sorted list of:
 *
 * <blockquote><pre>
 * file-01.doc
 * file-03.doc
 * file-2.doc</pre></blockquote>
 *
 *
 * But using this class, the result will be a more human readable and organizable sorted list of:
 *
 * <blockquote><pre>
 * file-01.doc
 * file-2.doc
 * file-03.doc</pre></blockquote>
 *
 *
 * Additionally this comparator uses [java.text.Collator] class to correctly sort
 * strings containing special Unicode characters such as Umlauts and other similar letters of
 * alphabet in different languages, such as: å, è, ü, ö, ø, or ý.
 *
 *
 * For given list of strings:
 *
 * <blockquote><pre>
 * b
 * e
 * ě
 * f
 * è
 * g
 * k</pre></blockquote>
 *
 *
 * Using a regular lexicographical sort e.g. [java.util.Collections.sort], will
 * sort the collection in the following order:
 *
 * <blockquote><pre>[b, e, f, g, k, è, ě]</pre></blockquote>
 *
 *
 * However using this class because of utilizing a [java.text.Collator], the previous values will be
 * sorted in following order:
 *
 * <blockquote><pre>[b, e, è, ě, f, g, k]</pre></blockquote>
 *
 * @author Farbod Safaei - farbod@binaryheart.com
 */
@Suppress("unused")
class AlphaNumericComparator : Comparator<String?> {
    private val collator: Collator

    /**
     * Default constructor, uses the default Locale and default collator strength
     *
     * @see AlphaNumericComparator
     */
    constructor() {
        collator = Collator.getInstance()
    }

    /**
     * Constructor using the provided `Locale` and default collator strength
     *
     * @param locale Desired `Locale`
     */
    constructor(locale: Locale) {
        collator = Collator.getInstance(Objects.requireNonNull(locale))
    }

    /**
     * Constructor with given `Locale` and collator strength value
     *
     * @param locale   Desired `Locale`
     * @param strength Collator strength value, any of collator values from: [java.text.Collator.PRIMARY],
     * [java.text.Collator.SECONDARY], [java.text.Collator.TERTIARY],
     * or [java.text.Collator.IDENTICAL]
     * @see java.text.Collator
     */
    constructor(locale: Locale, strength: Int) {
        collator = Collator.getInstance(Objects.requireNonNull(locale))
        collator.strength = strength
    }

    /**
     * Compares two given `String` parameters. Both string parameters will be trimmed before comparison.
     *
     * @param s1 the first string to be compared
     * @param s2 the second string to be compared
     * @return If any of the given parameters is `null` or is an empty string like
     * `""`, `-1` or `1` will be returned based on the order:
     * `-1` will be returned if the first parameter is `null` or empty,
     * `1` will be returned if the second parameter is  `null` or empty.
     * When both are either `null` or empty or any combination of those, a
     * `0` will be returned.
     */
    override fun compare(s1: String?, s2: String?): Int {
        var ss1 = s1
        var ss2 = s2
        if ((ss1 == null || ss1.trim { it <= ' ' }
                .isEmpty()) && ss2 != null && ss2.trim { it <= ' ' }.isNotEmpty()) {
            return -1
        }
        if ((ss2 == null || ss2.trim { it <= ' ' }
                .isEmpty()) && ss1 != null && ss1.trim { it <= ' ' }.isNotEmpty()) {
            return 1
        }
        if ((ss1 == null || ss1.trim { it <= ' ' }
                .isEmpty()) && (ss2 == null || ss2.trim { it <= ' ' }
                .isEmpty())) {
            return 0
        }
        assert(ss1 != null)
        ss1 = ss1!!.trim { it <= ' ' }
        assert(ss2 != null)
        ss2 = ss2!!.trim { it <= ' ' }
        var s1Index = 0
        var s2Index = 0
        while (s1Index < ss1.length && s2Index < ss2.length) {
            var result: Int
            val s1Slice = this.slice(ss1, s1Index)
            val s2Slice = this.slice(ss2, s2Index)
            s1Index += s1Slice.length
            s2Index += s2Slice.length
            result =
                if (Character.isDigit(s1Slice[0]) && Character.isDigit(s2Slice[0])) {
                    compareDigits(s1Slice, s2Slice)
                } else {
                    compareCollatedStrings(s1Slice, s2Slice)
                }
            if (result != 0) {
                return result
            }
        }
        return Integer.signum(ss1.length - ss2.length)
    }

    private fun slice(s: String, index: Int): String {
        var index1 = index
        val result = StringBuilder()
        if (Character.isDigit(s[index1])) {
            while (index1 < s.length && Character.isDigit(s[index1])) {
                result.append(s[index1])
                index1++
            }
        } else {
            result.append(s[index1])
        }
        return result.toString()
    }

    private fun compareDigits(s1: String, s2: String): Int {
        return BigInteger(s1).compareTo(BigInteger(s2))
    }

    private fun compareCollatedStrings(s1: String, s2: String): Int {
        return collator.compare(s1, s2)
    }
}