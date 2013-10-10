package net.osmand.plus.happycow;

import java.util.List;

import net.osmand.plus.happycow.TimeDescriptionParser.TimeDescriptionElement;
import net.osmand.plus.happycow.WeekIntervals.DayLocalInterval;
import net.osmand.plus.happycow.WeekIntervals.DayLocalTime;

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
			Integer nextSeparator = findNextCommaOrEnd(tokens, index);
			Integer nextDay = findNextDay(tokens, index);
			Integer nextTime = findNextTimeRange(tokens, index);
			boolean separatorIsEnd = nextSeparator == tokens.size();
			boolean noDayInCurrentChunk = (nextDay == -1) || (nextDay > nextSeparator);
			boolean noTimeInCurrentChunk = (nextTime == -1) || (nextTime > nextSeparator);
			if (noTimeInCurrentChunk && !separatorIsEnd) return Maybe.unknown();

			if (dayBuffer == null && noDayInCurrentChunk) return Maybe.unknown();
			if (!noDayInCurrentChunk) dayBuffer = tokens.get(nextDay);

			TimeDescriptionParser.TimeRange timeBuffer = (noTimeInCurrentChunk) ?
				TimeDescriptionParser.TimeRange.WHOLE_DAY : (TimeDescriptionParser.TimeRange) tokens.get(nextTime);

			@SuppressWarnings("unchecked")
			Iterable<Integer> days = (Iterable<Integer>) dayBuffer;
			for (Integer day : days) {
				DayLocalTime startDayTime = new DayLocalTime(day, timeBuffer.startTime);
				DayLocalTime endDayTime = new DayLocalTime(day, timeBuffer.endTime);
				if (!startDayTime.equals(endDayTime)) {
					intervals = intervals.add(new DayLocalInterval(startDayTime, endDayTime));
				}
			}

			index = nextSeparator == -1 ? tokens.size() : nextSeparator + 1;
		}

		return Maybe.definitely(intervals);
	}

	private static Integer findNextDay(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isDayRange = tokens.get(i) instanceof TimeDescriptionParser.DayRange;
			boolean isDay = tokens.get(i) instanceof TimeDescriptionParser.Day;
			if (isDayRange || isDay) {
				return i;
			}
		}

		return -1;
	}

	private static Integer findNextCommaOrEnd(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isComma = tokens.get(i) instanceof TimeDescriptionParser.Comma;
			if (isComma) {
				return i;
			}
		}

		return tokens.size();
	}

	private static Integer findNextTimeRange(List<TimeDescriptionElement> tokens, Integer startIndex) {
		assert(startIndex < tokens.size());
		for(Integer i=startIndex;i<tokens.size();i++) {
			boolean isTimeRange = tokens.get(i) instanceof TimeDescriptionParser.TimeRange;
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