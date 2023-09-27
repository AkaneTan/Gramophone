/*
 * Copyright 2016 Farbod Safaei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.akanework.gramophone.logic.utils;

import java.math.BigInteger;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>An alphanumeric comparator for comparing strings in a human readable format.
 * It uses a combination numeric and alphabetic comparisons to compare two strings.
 * This class uses standard Java classes, independent of 3rd party libraries.<p>
 *
 * For given list of strings:
 * <blockquote><pre>
 * file-01.doc
 * file-2.doc
 * file-03.doc</pre></blockquote>
 *
 * The regular lexicographical sort e.g. {@link java.util.Collections#sort(java.util.List)} will result in a sorted list of:
 *
 * <blockquote><pre>
 * file-01.doc
 * file-03.doc
 * file-2.doc</pre></blockquote>
 *
 * But using this class, the result will be a more human readable and organizable sorted list of:
 *
 * <blockquote><pre>
 * file-01.doc
 * file-2.doc
 * file-03.doc</pre></blockquote>
 *
 * <p>Additionally this comparator uses {@link java.text.Collator} class to correctly sort
 * strings containing special Unicode characters such as Umlauts and other similar letters of
 * alphabet in different languages, such as: å, è, ü, ö, ø, or ý.</p>
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
 * Using a regular lexicographical sort e.g. {@link java.util.Collections#sort(java.util.List)}, will
 * sort the collection in the following order:
 *
 * <blockquote><pre>[b, e, f, g, k, è, ě]</pre></blockquote>
 *
 * However using this class because of utilizing a {@link java.text.Collator}, the previous values will be
 * sorted in following order:
 *
 * <blockquote><pre>[b, e, è, ě, f, g, k]</pre></blockquote>
 *
 * @author Farbod Safaei - farbod@binaryheart.com
 *
 */
@SuppressWarnings("unused")
public class AlphaNumericComparator implements Comparator<String> {

	private final Collator collator;

	/**
	 * Default constructor, uses the default Locale and default collator strength
	 *
	 * @see AlphaNumericComparator#AlphaNumericComparator(Locale)
	 */
	public AlphaNumericComparator() {
		collator = Collator.getInstance();
	}

	/**
	 * Constructor using the provided {@code Locale} and default collator strength
	 *
	 * @param locale	Desired {@code Locale}
	 */
	public AlphaNumericComparator(final Locale locale) {
		collator = Collator.getInstance(Objects.requireNonNull(locale));
	}


	/**
	 * Constructor with given {@code Locale} and collator strength value
	 *
	 * @param locale
	 *             Desired {@code Locale}
	 *
	 * @param strength
	 *             Collator strength value, any of collator values from: {@link java.text.Collator#PRIMARY},
	 *             {@link java.text.Collator#SECONDARY}, {@link java.text.Collator#TERTIARY},
	 *             or {@link java.text.Collator#IDENTICAL}
	 *
	 * @see java.text.Collator
	 */
	public AlphaNumericComparator(final Locale locale, final int strength) {
		collator = Collator.getInstance(Objects.requireNonNull(locale));
		collator.setStrength(strength);
	}

	/**
	 * Compares two given {@code String} parameters. Both string parameters will be trimmed before comparison.
	 *
	 * @param s1
	 *             the first string to be compared
	 * @param s2
	 *             the second string to be compared
	 *
	 * @return     If any of the given parameters is {@code null} or is an empty string like
	 *             {@code ""}, {@code -1} or {@code 1} will be returned based on the order:
	 *             {@code -1} will be returned if the first parameter is {@code null} or empty,
	 *             {@code 1} will be returned if the second parameter is  {@code null} or empty.
	 *             When both are either {@code null} or empty or any combination of those, a
	 *             {@code 0} will be returned.
	 */
	@Override
	public int compare(String s1, String s2) {
		if ((s1 == null || s1.trim().isEmpty()) && (s2 != null && !s2.trim().isEmpty())) {
			return -1;
		}
		if ((s2 == null || s2.trim().isEmpty()) && (s1 != null && !s1.trim().isEmpty())) {
			return 1;
		}
		if ((s1 == null || s1.trim().isEmpty()) && (s2 == null || s2.trim().isEmpty())) {
			return 0;
		}

		assert s1 != null;
		s1 = s1.trim();
		assert s2 != null;
		s2 = s2.trim();
		int s1Index = 0;
		int s2Index = 0;
		while (s1Index < s1.length() && s2Index < s2.length()) {
			int result;
			String s1Slice = this.slice(s1, s1Index);
			String s2Slice = this.slice(s2, s2Index);
			s1Index += s1Slice.length();
			s2Index += s2Slice.length();
			if (Character.isDigit(s1Slice.charAt(0)) && Character.isDigit(s2Slice.charAt(0))) {
				result = this.compareDigits(s1Slice, s2Slice);
			} else {
				result = this.compareCollatedStrings(s1Slice, s2Slice);
			}
			if (result != 0) {
				return result;
			}
		}
		return Integer.signum(s1.length() - s2.length());
	}

	private String slice(String s, int index) {
		StringBuilder result = new StringBuilder();
		if (Character.isDigit(s.charAt(index))) {
			while (index < s.length() && Character.isDigit(s.charAt(index))) {
				result.append(s.charAt(index));
				index++;
			}
		} else {
			result.append(s.charAt(index));
		}
		return result.toString();
	}

	private int compareDigits(String s1, String s2) {
		return new BigInteger(s1).compareTo(new BigInteger(s2));
	}

	private int compareCollatedStrings(String s1, String s2) {
		return collator.compare(s1, s2);
	}

}