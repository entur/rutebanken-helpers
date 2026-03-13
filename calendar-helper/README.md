# calendar-helper

Analyzes a set of dates and detects recurring day-of-week patterns. Useful for inferring service schedules (e.g., "Monday–Friday", "weekends") from raw date sets.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>calendar-helper</artifactId>
    <version>VERSION</version>
</dependency>
```

## Key Classes

### `CalendarPatternAnalyzer`

| Method | Description |
|--------|-------------|
| `computeCalendarPattern(Set<LocalDate>)` | Main entry point. Returns a `CalendarPattern` or `null` if no pattern found |
| `computeSignificantDays(Set<LocalDate>)` | Returns the `Set<DayOfWeek>` representing the detected pattern |

**Detection rules:**
- Requires at least **5 dates** in the input set.
- A pattern must cover at least **90%** of the provided dates (5% error margin allowed).
- Each included day-of-week must individually meet a minimum frequency threshold.

### `CalendarPattern`

Result object returned by the analyzer.

| Field | Type | Description |
|-------|------|-------------|
| `significantDays` | `Set<DayOfWeek>` | The detected recurring days (e.g., `{MON, TUE, WED, THU, FRI}`) |
| `from` | `LocalDate` | Start of the validity period (inclusive) |
| `to` | `LocalDate` | End of the validity period (exclusive) |
| `additionalDates` | `Set<LocalDate>` | Dates outside the pattern period that are included |
| `excludedDates` | `Set<LocalDate>` | Gaps within the pattern period (days that should match but don't) |

## Usage

```java
CalendarPatternAnalyzer analyzer = new CalendarPatternAnalyzer();

Set<LocalDate> serviceDates = Set.of(
    LocalDate.of(2024, 1, 1),  // Monday
    LocalDate.of(2024, 1, 2),  // Tuesday
    // ... more dates
);

CalendarPattern pattern = analyzer.computeCalendarPattern(serviceDates);

if (pattern != null) {
    System.out.println("Pattern days: " + pattern.significantDays);
    // e.g. [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

    System.out.println("Valid from " + pattern.from + " to " + pattern.to);
    System.out.println("Excluded dates: " + pattern.excludedDates);
    System.out.println("Additional dates: " + pattern.additionalDates);
} else {
    System.out.println("No recurring pattern detected");
}
```

## Algorithm Summary

1. Count how often each day-of-week appears in the input.
2. Rank days by frequency, then try to find N consecutive days (highest coverage first).
3. Accept the pattern if it covers ≥ 90% of all input dates.
4. Remaining dates become `additionalDates`; missing days within the period become `excludedDates`.

---

[Back to root](../README.md)