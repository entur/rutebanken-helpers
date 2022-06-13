/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.rutebanken.helper.calendar;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

class CalendarPatternAnalyzerTest {
	CalendarPatternAnalyzer analyzer = new CalendarPatternAnalyzer();
	LocalDate calStartDate = LocalDate.of(2016, 1, 18); // Jan 18 2016


	@Test
	void testParseFromStringEmptyPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(emptyPattern));
		Assertions.assertTrue(significantDays.isEmpty());
	}

	@Test
	void testParseFromStringSundayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(sundayPattern));
		Assertions.assertEquals(significantDays, Set.of(DayOfWeek.SUNDAY));
	}

	@Test
	void testParseFromStringSaturdayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(saturdayPattern));
		Assertions.assertEquals(significantDays, Set.of(DayOfWeek.SATURDAY));
	}

	@Test
	void testParseFromStringFridayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(fridayPattern));
		Assertions.assertEquals(significantDays, Set.of(DayOfWeek.FRIDAY));
	}

	@Test
	void testParseFromStringWednesdayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(wednesdayPattern));
		Assertions.assertEquals(significantDays, Set.of(DayOfWeek.WEDNESDAY));
	}

	@Test
	void testFromStringSingleDayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(noPattern));
		Assertions.assertTrue(significantDays.isEmpty());
	}

	@Test
	void testParseFromStringTuesdayToFridayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(tuesdayToFridayPattern));
		Assertions.assertEquals(significantDays, Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
	}

	@Test
	void testComputeCalendarPatternEmptyPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, emptyPattern));
		Assertions.assertNull(pattern);
	}

	@Test
	void testComputeCalendarPatternSundayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, sundayPattern));
		Assertions.assertEquals(pattern.significantDays, Set.of(DayOfWeek.SUNDAY));
		Assertions.assertEquals(pattern.from, LocalDate.of(2016, 2, 1));
		Assertions.assertEquals(pattern.to, LocalDate.of(2016, 6, 25));
	}

	@Test
	void testComputeCalendarPatternSaturdayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, saturdayPattern));
		Assertions.assertEquals(pattern.significantDays, Set.of(DayOfWeek.SATURDAY));
	}

	@Test
	void testComputeCalendarPatternFridayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, fridayPattern));
		Assertions.assertEquals(pattern.significantDays, Set.of(DayOfWeek.FRIDAY));
	}

	@Test
	void testComputeCalendarPatternWednesdayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, wednesdayPattern));
		Assertions.assertEquals(pattern.significantDays, Set.of(DayOfWeek.WEDNESDAY));
	}

	@Test
	void testComputeCalendarPatternNoPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, noPattern));
		Assertions.assertNull(pattern);
	}

	@Test
	void testComputeCalendarPatternTuesdayToFridayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, tuesdayToFridayPattern));
		Assertions.assertEquals(pattern.significantDays, Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
		Assertions.assertEquals(pattern.from, LocalDate.of(2016, 1, 16));
		Assertions.assertEquals(pattern.to, LocalDate.of(2016, 6, 20));
		Assertions.assertEquals(pattern.excludedDates, Set.of(calStartDate.plusDays(64), calStartDate.plusDays(65), calStartDate.plusDays(66),
				calStartDate.plusDays(67), calStartDate.plusDays(108), calStartDate.plusDays(120)));
		Assertions.assertEquals(pattern.additionalDates, Set.of(calStartDate.plusDays(6), calStartDate.plusDays(343)));

	}

	@Test
	void testComputeCalendarPatternNoDatesIncluded() {
		Assertions.assertNull(analyzer.computeCalendarPattern(new HashSet<>()));
	}


	@Test
	void testComputeCalendarPatternEveryDayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, everyDayPattern));
		Assertions.assertEquals(pattern.from, calStartDate);
		Assertions.assertEquals(pattern.to, calStartDate.plusDays(74));
		Assertions.assertTrue(pattern.additionalDates.isEmpty());
		Assertions.assertTrue(pattern.excludedDates.isEmpty());
	}

	String everyDayPattern = "111111111111111111111111111111111111111111111111111111111111111111111111111";


	String emptyPattern =
			"00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

	String sundayPattern =
			"00000000000000000000100000010000001000000100000010000001000000100000000000001000000100000010000001000000100010010000001100000100000010000001000000100000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	String saturdayPattern =
			"00000100000010000001000000100000010000001000000100000010000001000000000000010000001000000100000010000001000000100000010000001000000100000010000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	String noPattern =
			"00000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	String fridayPattern =
			"00001000000100000010000001000000100000010000001000000100000010000000000000100000010000001000000100000010000001000000100000010000001000000100000010000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	String wednesdayPattern =
			"00100000010000001000000100000010000001000000100000010000001000000000000010000001000000100000010000001000000100000010000001000000100000010000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	String tuesdayToFridayPattern =
			"01111010111100011110001111000111100011110001111000111100011110000000000111100011110001111000111100011110001101000111100001110001111000111100011110001111000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000";


	private boolean[] includedDaysAsBoolArray(String includedArray) {
		int length = includedArray.length();
		boolean[] initialIncludedArray = new boolean[length];

		// Convert to boolean array
		for (int i = 0; i < length; i++) {
			initialIncludedArray[i] = includedArray.charAt(i) == '1';
		}
		return initialIncludedArray;
	}

	private Set<LocalDate> includedDaysAsLocalDates(LocalDate startDate, String includedArray) {
		Set<LocalDate> dates = new HashSet<>();
		int i = 0;
		for (char c : includedArray.toCharArray()) {
			if (c == '1') {
				dates.add(startDate.plusDays(i));
			}
			i++;
		}

		return dates;
	}
}
