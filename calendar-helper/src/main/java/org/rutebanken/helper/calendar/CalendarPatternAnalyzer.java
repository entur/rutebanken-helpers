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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CalendarPatternAnalyzer {
	private static final Logger log = LoggerFactory.getLogger(CalendarPatternAnalyzer.class);

	private static final int DAYS_PER_WEEK = 7;
	private static final int ERROR_MARGIN = 5;
	private static final int MIN_PERCENTAGE_ALL_DAYS_DETECTED = 90;
	private static final int MIN_DAYS_FOR_PATTERN = 5;


	/**
	 * Detect calendar pattern from set of dates.
	 * <p>
	 * Returns significant days with a period of validity as well as any deviations from the pattern within the period and additional dates inside or outside the period.
	 */
	public CalendarPattern computeCalendarPattern(Set<LocalDate> includedDays) {

		Set<DayOfWeek> significantDays = computeSignificantDays(includedDays);
		if (!significantDays.isEmpty()) {

			TreeSet<LocalDate> sortedMatchingDays =
					includedDays.stream().filter(d -> significantDays.contains(d.getDayOfWeek())).collect(Collectors.toCollection(TreeSet::new));
			if (!sortedMatchingDays.isEmpty()) {
				return buildCalendarPattern(includedDays, significantDays, sortedMatchingDays);
			}
		}

		return null;
	}

	/**
	 * Detect pattern of significant days within set of dates.
	 */
	public Set<DayOfWeek> computeSignificantDays(Set<LocalDate> includedDays) {
		Map<DayOfWeek, WeekDayEntry> dayMap = initWeekDayMap();

		includedDays.forEach(d -> dayMap.get(d.getDayOfWeek()).addCount());
		return computeSignificantDays(dayMap);
	}

	/**
	 * Detect pattern of significant days from start date and array indicating valid subsequent matching dates.
	 */
	public Set<DayOfWeek> computeSignificantDays(LocalDate d, boolean[] included) {
		Map<DayOfWeek, WeekDayEntry> dayMap = initWeekDayMap();

		// Count hits for each day type
		for (int i = 0; i < included.length; i++) {
			DayOfWeek dayOfWeek = d.plusDays(i).getDayOfWeek();
			if (included[i]) {
				dayMap.get(dayOfWeek).addCount();
			}
		}
		return computeSignificantDays(dayMap);
	}


	private CalendarPattern buildCalendarPattern(Set<LocalDate> includedDays, Set<DayOfWeek> significantDays, TreeSet<LocalDate> sortedMatchingDays) {
		LocalDate patternStart = calculateStartOfValidityIntervalInclusive(sortedMatchingDays.first(), significantDays);
		LocalDate patternEnd = calculateEndOfValidityIntervalExclusive(sortedMatchingDays.last(), significantDays);

		Set<LocalDate> additionalDates = includedDays.stream().filter(md -> md.isBefore(patternStart) || md.isAfter(patternEnd)).collect(Collectors.toSet());
		Set<LocalDate> excludedDates = new HashSet<>();
		LocalDate current = patternStart;
		while (current.isBefore(patternEnd)) {
			if (significantDays.contains(current.getDayOfWeek())) {
				if (!includedDays.contains(current)) {
					excludedDates.add(current);
				}
			} else if (includedDays.contains(current)) {
				additionalDates.add(current);
			}

			current = current.plusDays(1);
		}

		return new CalendarPattern(patternStart, patternEnd, significantDays, additionalDates, excludedDates);
	}

	private Set<DayOfWeek> computeSignificantDays(Map<DayOfWeek, WeekDayEntry> dayMap) {

		Set<DayOfWeek> significantDays = new HashSet<>();
		// compute percentages
		int totalDaysIncluded = 0;
		for (WeekDayEntry entry : dayMap.values()) {
			totalDaysIncluded += entry.getCount();
		}

		if (totalDaysIncluded > MIN_DAYS_FOR_PATTERN) {

			for (WeekDayEntry entry : dayMap.values()) {
				entry.setPercentage(((double) entry.getCount()) * 100 / (double) totalDaysIncluded);
			}

			// Try to find patterns
			List<WeekDayEntry> entries = new ArrayList<>(dayMap.values());

			entries.sort((o1, o2) -> (int) (o2.getPercentage() - o1.getPercentage()));

			// i = number of days attempted to merge together
			for (int i = 1; i <= DAYS_PER_WEEK; i++) {
				double minDayPercentage = (double) (MIN_PERCENTAGE_ALL_DAYS_DETECTED - ERROR_MARGIN) / (double) i; // for i=2 this means 42.5 for each day type

				// Start from 0
				double totalDayPercentage = 0;
				boolean allDaysAboveMinDayPercentage = true;
				for (int j = 0; j < i; j++) {
					double percentage = entries.get(j).getPercentage();
					if (percentage < minDayPercentage) {
						allDaysAboveMinDayPercentage = false;
					}
					totalDayPercentage += percentage;
				}

				if (allDaysAboveMinDayPercentage && totalDayPercentage > MIN_PERCENTAGE_ALL_DAYS_DETECTED) {
					for (int j = 0; j < i; j++) {
						significantDays.add(entries.get(j).getDayType());
					}
					// Found match
					break;
				}
			}
		} else {
			log.debug("Too few days to extract pattern, expected at least {} but only got {}", MIN_DAYS_FOR_PATTERN, totalDaysIncluded);
		}

		return significantDays;
	}

	/**
	 * Interval considered valid until from day after previous expected date before first pattern matching day
	 */
	private LocalDate calculateStartOfValidityIntervalInclusive(LocalDate firstMatchingDay, Set<DayOfWeek> expectedDays) {
		for (int i = 1; i <= DAYS_PER_WEEK; i++) {
			LocalDate candidate = firstMatchingDay.minusDays(i);
			if (expectedDays.contains(candidate.getDayOfWeek())) {
				return candidate.plusDays(1);
			}
		}
		return firstMatchingDay;
	}

	/**
	 * Interval considered valid until first expected date after last pattern matching day
	 */
	private LocalDate calculateEndOfValidityIntervalExclusive(LocalDate lastMatchingDay, Set<DayOfWeek> expectedDays) {
		for (int i = 1; i <= DAYS_PER_WEEK; i++) {
			LocalDate candidate = lastMatchingDay.plusDays(i);
			if (expectedDays.contains(candidate.getDayOfWeek())) {
				return candidate.minusDays(1);
			}
		}
		return lastMatchingDay;
	}

	private Map<DayOfWeek, WeekDayEntry> initWeekDayMap() {
		return Arrays.stream(DayOfWeek.values()).map(WeekDayEntry::new).collect(Collectors.toMap(WeekDayEntry::getDayType, Function.identity()));
	}


	private static class WeekDayEntry {

		int count = 0;

		double percentage = 0;

		DayOfWeek dayType;

		public WeekDayEntry(DayOfWeek dayType) {
			this.dayType = dayType;
		}

		public void addCount() {
			count++;
		}

		@Override
		public String toString() {
			return "WeekDayEntry{" +
					       "count=" + count +
					       ", percentage=" + percentage +
					       ", dayType=" + dayType +
					       '}';
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public double getPercentage() {
			return percentage;
		}

		public void setPercentage(double percentage) {
			this.percentage = percentage;
		}

		public DayOfWeek getDayType() {
			return dayType;
		}

		public void setDayType(DayOfWeek dayType) {
			this.dayType = dayType;
		}
	}
}