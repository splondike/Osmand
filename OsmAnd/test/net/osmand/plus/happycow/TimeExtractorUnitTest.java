package net.osmand.plus.happycow;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.osmand.plus.happycow.WeekIntervals.DayLocalInterval;
import net.osmand.plus.happycow.WeekIntervals.DayLocalTime;

import org.joda.time.LocalTime;
import org.junit.Test;

import com.natpryce.maybe.Maybe;

public class TimeExtractorUnitTest extends TestCase {
	@Test
	public void testExtractTimeSentence() {
		String none = "Open is the word we're looking for, but at the end. I.e. here.";
		String some = "This time the word is correct. Open at some time. ";

		assertEquals(false, TimeExtractor.extractTimeSentence(none).isKnown());
		assertEquals(Maybe.definitely("at some time"), TimeExtractor.extractTimeSentence(some));
	}

	public void testExtractIntervals() {
		List<DayLocalInterval> monToSun = new LinkedList<DayLocalInterval>();
		for(Integer i=1;i<8;i++) {
			monToSun.add(buildCase(i,7,30,23,0));
		}
		Object[][] testCases = {
			{"Mon-Tue", buildIntervals(buildCase(1), buildCase(2))},
			{"Mon 8am-5:30pm", buildIntervals(buildCase(1,8,0,17,30))},
			{"Mon 8am-5:30pm, Fri 7:30am-4pm", buildIntervals(buildCase(1,8,0,17,30), buildCase(5,7,30,16,0))},
			{"Sun-Mon 8am-5:30pm", buildIntervals(buildCase(7,8,0,17,30), buildCase(1,8,0,17,30))},
			{"Mon 8-5:30pm", buildIntervals(buildCase(1,8,0,17,30))},
			{"daily 7:30am-11pm", buildIntervals(monToSun.toArray(new DayLocalInterval[]{}))},
			{"Mon lunch 11:30am-2:30pm, dinner 5-10pm, closed Sun, Sat closed",
				buildIntervals(buildCase(1,11,30,14,30), buildCase(1,17,0,22,0))},
			{"Sun-Mon, Tue 11am-9pm", Maybe.unknown()},
			{"Mon 11am-9.30pm", buildIntervals(buildCase(1,11,0,21,30))},
			{"Mon 10-12am, Tue 10-2am", buildIntervals(buildCase(1,22,0,21,30))},
		};

		for (Object[] testCase : testCases) {
			String input = (String) testCase[0];
			@SuppressWarnings("unchecked")
			Maybe<WeekIntervals> expected = (Maybe<WeekIntervals>) testCase[1];
			Maybe<WeekIntervals> actual = TimeExtractor.extractIntervals(input);

			if (expected.isKnown()) {
				assertTrue(actual.isKnown());
				assertTrue(actual.iterator().next().toString(), expected.iterator().next().equivalent(actual.iterator().next()));
			}
			else {
				assertFalse(actual.isKnown());
			}
		}
	}

	private static Maybe<WeekIntervals> buildIntervals(DayLocalInterval... intervals) {
		WeekIntervals wi = new WeekIntervals();
		for (DayLocalInterval interval : intervals) {
			wi = wi.add(interval);
		}

		return Maybe.definitely(wi);
	}

	private static DayLocalInterval buildCase(Integer day, Integer startHour, Integer startMinute, Integer endHour, Integer endMinute) {
		DayLocalTime start = new DayLocalTime(day, new LocalTime(startHour, startMinute));
		DayLocalTime end = new DayLocalTime(day, new LocalTime(endHour, endMinute));
		return new DayLocalInterval(start, end);
	}

	/**
	 * The whole day.
	 */
	private static DayLocalInterval buildCase(Integer day) {
		DayLocalTime start = new DayLocalTime(day, TimeDescriptionParser.TimeRange.WHOLE_DAY.startTime);
		DayLocalTime end = new DayLocalTime(day, TimeDescriptionParser.TimeRange.WHOLE_DAY.endTime);
		return new DayLocalInterval(start, end);
	}
}
/*
Open Mon-Fri 8am-5pm.
Open Mon-Fri 8am-6:30pm, closed Sat-Sun.
Open Mon-Thur 7:30am-5pm, Fri 7:30am-4pm.
Open Mon-Wed 7am-7pm, Thur-Fri 7am-9pm, Sat 8am-9pm, Sun 9am-7pm.
Open Mon-Sat lunch 11:30am-2:30pm, dinner 5-10pm, closed Sun.
Open Mon-Fri 7:30am-8pm, Sat 10am-6pm, Sun 10am-6pm.
Open Mon-Fri 10am-9pm, Sat 10am-7pm, Sun 12-6pm.
Open Mon-Fri 10am-9pm, Sat 11am-7pm.
Mon-Sat: 11AM-11PM Sun: 12PM-11PM / Patio
Open Mon-Sun 11:30am-9:30pm.
Open Mon-Fri 11am-8:30pm.
Open Mon-Sun.
Open Mon-Fri 10am-11pm, Sat 11am-9pm, Sun 12-7pm.
Open Tue-Fri 12-3pm, 5-10pm, Sat-Sun 5-10pm.
Open Mon-Sun 11:30am-9pm.
Open Tue-Fri 9-5:30, Sat 8-5, Sun-Mon closed.
Open Wed-Sun 5:30-11pm, closed Mon-Tue.
Open Mon-Fri 6am-7pm, Sat 8am-2pm, closed Sun.
Open Mon-Fri 10:30am-9pm, Sat 11am-6pm, closed Sun.
Open Sun-Tue, Thur 11am-9pm, Fri-Sat 11am-9:30pm, closed Wed.
Open Mon-Fri 7:30am-10pm, Sat-Sun 9am-10pm.
Open 11am-8:30pm, closed Tues.
Open Mon-Sat 12-7pm, Sun 2-7pm.
Open Mon-Sun 11am-7pm.
Open Mon-Sat 11am-9.30pm, Sun 11am-9pm.
Open Mon-Fri 7:30am-10pm, Sat-Sun 9am-10pm.
Open 11am-8:30pm, closed Tues.
Tue-Sun 5-10:30pm, Mon closed.
Open Mon-Fri 10am-2pm.
Open Mon-Sat 9am-7pm.
Open Mon-Thur 5:30-9pm, Fri-Sun 1:30-9pm.
Open Tue-Sun 5pm-11pm, closed Mon.
Open Mon-Sun 10am-8pm.
Open daily 7:30am-11pm.
Open Sun-Wed 9am-10pm, Thur-Sat 9-12am.
Open Mon-Fri 11:30am-7:30pm, closed Sat-Sun.
Open Mon-Fri 11am-10pm, Sat-Sun 10am-10pm.
Open Mon-Fri 11am-8:30pm, Sat 11am-7pm, Sun 11am-6:30pm.
Open Mon-Fri 11:30am-12am, Sat-Sun 11am-12am.
Open Mon-Sun.
*/