package org.rutebanken.helper.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public class CalendarPattern {
	public LocalDate from;
	public LocalDate to;
	public Set<DayOfWeek> significantDays;
	public Set<LocalDate> additionalDates;
	public Set<LocalDate> excludedDates;

	public CalendarPattern(LocalDate from, LocalDate to, Set<DayOfWeek> significantDays, Set<LocalDate> additionalDates, Set<LocalDate> excludedDates) {
		this.from = from;
		this.to = to;
		this.significantDays = significantDays;
		this.additionalDates = additionalDates;
		this.excludedDates = excludedDates;
	}
}
