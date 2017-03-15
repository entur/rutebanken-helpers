package org.rutebanken.helper.calendar;


import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class CalendarPatternAnalyzerTest {
	CalendarPatternAnalyzer analyzer = new CalendarPatternAnalyzer();
	LocalDate calStartDate = LocalDate.of(2016, 1, 18); // Jan 18 2016


	@Test
	public void testParseFromStringEmptyPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(emptyPattern));
		Assert.assertTrue(significantDays.isEmpty());
	}

	@Test
	public void testParseFromStringSundayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(sundayPattern));
		Assert.assertEquals(significantDays, Sets.newHashSet(DayOfWeek.SUNDAY));
	}

	@Test
	public void testParseFromStringSaturdayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(saturdayPattern));
		Assert.assertEquals(significantDays, Sets.newHashSet(DayOfWeek.SATURDAY));
	}

	@Test
	public void testParseFromStringFridayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(fridayPattern));
		Assert.assertEquals(significantDays, Sets.newHashSet(DayOfWeek.FRIDAY));
	}

	@Test
	public void testParseFromStringWednesdayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(wednesdayPattern));
		Assert.assertEquals(significantDays, Sets.newHashSet(DayOfWeek.WEDNESDAY));
	}

	@Test
	public void testFromStringSingleDayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(noPattern));
		Assert.assertTrue(significantDays.isEmpty());
	}

	@Test
	public void testParseFromStringTuesdayToFridayPattern() {
		Set<DayOfWeek> significantDays = analyzer.computeSignificantDays(calStartDate, includedDaysAsBoolArray(tuesdayToFridayPattern));
		Assert.assertEquals(significantDays, Sets.newHashSet(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
	}

	@Test
	public void testComputeCalendarPatternEmptyPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, emptyPattern));
		Assert.assertNull(pattern);
	}

	@Test
	public void testComputeCalendarPatternSundayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, sundayPattern));
		Assert.assertEquals(pattern.significantDays, Sets.newHashSet(DayOfWeek.SUNDAY));
		Assert.assertEquals(pattern.from, LocalDate.of(2016, 2, 1));
		Assert.assertEquals(pattern.to, LocalDate.of(2016, 6, 25));
	}

	@Test
	public void testComputeCalendarPatternSaturdayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, saturdayPattern));
		Assert.assertEquals(pattern.significantDays, Sets.newHashSet(DayOfWeek.SATURDAY));
	}

	@Test
	public void testComputeCalendarPatternFridayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, fridayPattern));
		Assert.assertEquals(pattern.significantDays, Sets.newHashSet(DayOfWeek.FRIDAY));
	}

	@Test
	public void testComputeCalendarPatternWednesdayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, wednesdayPattern));
		Assert.assertEquals(pattern.significantDays, Sets.newHashSet(DayOfWeek.WEDNESDAY));
	}

	@Test
	public void testComputeCalendarPatternNoPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, noPattern));
		Assert.assertNull(pattern);
	}

	@Test
	public void testComputeCalendarPatternTuesdayToFridayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, tuesdayToFridayPattern));
		Assert.assertEquals(pattern.significantDays, Sets.newHashSet(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
		Assert.assertEquals(pattern.from, LocalDate.of(2016, 1, 16));
		Assert.assertEquals(pattern.to, LocalDate.of(2016, 6, 20));
		Assert.assertEquals(pattern.excludedDates, Sets.newHashSet(calStartDate.plusDays(64), calStartDate.plusDays(65), calStartDate.plusDays(66),
				calStartDate.plusDays(67), calStartDate.plusDays(108), calStartDate.plusDays(120)));
		Assert.assertEquals(pattern.additionalDates, Sets.newHashSet(calStartDate.plusDays(6), calStartDate.plusDays(343)));

	}

	@Test
	public void testComputeCalendarPatternNoDatesIncluded() {
		Assert.assertNull(analyzer.computeCalendarPattern(new HashSet<>()));
	}


	@Test
	public void testComputeCalendarPatternEveryDayPattern() {
		CalendarPattern  pattern = analyzer.computeCalendarPattern(includedDaysAsLocalDates(calStartDate, everyDayPattern));
		Assert.assertEquals(pattern.from, calStartDate);
		Assert.assertEquals(pattern.to, calStartDate.plusDays(74));
		Assert.assertTrue(pattern.additionalDates.isEmpty());
		Assert.assertTrue(pattern.excludedDates.isEmpty());
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
