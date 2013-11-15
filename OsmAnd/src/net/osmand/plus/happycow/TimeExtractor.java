package net.osmand.plus.happycow;

import java.util.List;

import net.osmand.plus.happycow.TimeDescriptionParser.TimeDescriptionElement;
import net.osmand.plus.happycow.WeekIntervals.DayLocalInterval;
import net.osmand.plus.happycow.WeekIntervals.DayLocalTime;
import net.osmand.plus.happycow.TimeDescriptionParser.*;

import com.natpryce.maybe.Maybe;

/**
 * Responsible for extracting a set of time ranges from a happy cow description string. This is the entry point to the module.
 */
public class TimeExtractor {
	/**
	 * Parses description to determine what times the listing is open.
	 * 
	 * @param description A description string returned by the HappyCow API.
	 * @return unknown if a parse error occurred, definitely if successful parse.
	 */
	public static Maybe<WeekIntervals> parseTimes(String description) {
		Maybe<String> timeSentence = TimeExtractor.extractTimeSentence(description);
		if (timeSentence.isKnown()) {
			return TimeExtractor.extractIntervals(timeSentence.iterator().next());
		}

		return Maybe.unknown();
	}

	/* package */ static Maybe<WeekIntervals> extractIntervals(String timeDescription) {
		List<TimeDescriptionElement> tokens = TimeDescriptionParser.parse(timeDescription);
		if (tokens.isEmpty()) return Maybe.unknown();

		WeekIntervals intervals = new WeekIntervals();
		Integer index = 0;
		TimeDescriptionElement dayBuffer = null;
		while (index < tokens.size()) {
			Integer separatorIndex = findNextCommaOrEnd(tokens, index);
			Integer dayIndex = findNextDay(tokens, index);
			Integer timeIndex = findNextTimeRange(tokens, index);
			boolean separatorIsEnd = separatorIndex == tokens.size();
			boolean noDayInCurrentChunk = (dayIndex == -1) || (dayIndex > separatorIndex);
			boolean noTimeInCurrentChunk = (timeIndex == -1) || (timeIndex > separatorIndex);
			if (noTimeInCurrentChunk && !separatorIsEnd) return Maybe.unknown();

			if (dayBuffer == null && noDayInCurrentChunk) return Maybe.unknown();
			if (!noDayInCurrentChunk) dayBuffer = tokens.get(dayIndex);

			TimeRange timeBuffer = (noTimeInCurrentChunk) ? TimeRange.WHOLE_DAY : (TimeRange) tokens.get(timeIndex);

			if (!timeBuffer.startTime.equals(timeBuffer.endTime)) {
				// Account for the case where we're going over the day boundary
				TimeRange currDayTimes = timeBuffer;
				TimeRange nextDayTimes = null;
				if (timeBuffer.startTime.isAfter(timeBuffer.endTime)) {
					currDayTimes = new TimeRange(timeBuffer.startTime, TimeRange.END_OF_DAY);
					nextDayTimes = new TimeRange(TimeRange.START_OF_DAY, timeBuffer.endTime);
				}

				@SuppressWarnings("unchecked")
				Iterable<Integer> days = (Iterable<Integer>) dayBuffer;
				for (Integer day : days) {
					if (nextDayTimes != null) {
						Integer nextDay = Day.nextDay(day);
						DayLocalTime startDayTime = new DayLocalTime(nextDay, nextDayTimes.startTime);
						DayLocalTime endDayTime = new DayLocalTime(nextDay, nextDayTimes.endTime);
						intervals = intervals.add(new DayLocalInterval(startDayTime, endDayTime));
					}

					DayLocalTime startDayTime = new DayLocalTime(day, currDayTimes.startTime);
					DayLocalTime endDayTime = new DayLocalTime(day, currDayTimes.endTime);
					intervals = intervals.add(new DayLocalInterval(startDayTime, endDayTime));
				}
			}

			index = separatorIndex == -1 ? tokens.size() : separatorIndex + 1;
		}

		return Maybe.definitely(intervals);
	}

	private static Integer findNextDay(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isDayRange = tokens.get(i) instanceof DayRange;
			boolean isDay = tokens.get(i) instanceof Day;
			if (isDayRange || isDay) {
				return i;
			}
		}

		return -1;
	}

	private static Integer findNextCommaOrEnd(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isComma = tokens.get(i) instanceof Comma;
			if (isComma) {
				return i;
			}
		}

		return tokens.size();
	}

	private static Integer findNextTimeRange(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isTimeRange = tokens.get(i) instanceof TimeRange;
			if (isTimeRange) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Extracts the time sequence from a description if it exists, minus fluff like fullstops and 'Open '.
	 */
	/* package */ static Maybe<String> extractTimeSentence(String description) {
		String trimmedDescription = description.trim();
		Boolean endsWithPeriod = trimmedDescription.endsWith("\\.");
		String withoutTrailingPeriod = endsWithPeriod ? description.substring(0, description.length() - 1) : trimmedDescription;

		String[] sentences = withoutTrailingPeriod.split("\\.");
		if (sentences.length > 0) {
			String lastSentence = sentences[sentences.length - 1].trim();
			if (lastSentence.startsWith("Open ")) {
				String withoutOpen = lastSentence.replaceFirst("Open ", "");
				return Maybe.definitely(withoutOpen);
			}
		}

		return Maybe.unknown();
	}
}