package net.osmand.plus.happycow;

import junit.framework.TestCase;
import net.osmand.plus.happycow.WeekIntervals.DayLocalInterval;
import net.osmand.plus.happycow.WeekIntervals.DayLocalTime;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;

public class WeekIntervalsUnitTest extends TestCase {
	public void testIntervalsContains() {
		final DayLocalTime monday9am = new DayLocalTime(DateTimeConstants.MONDAY, new LocalTime(9, 0));
		final DayLocalTime monday9pm = new DayLocalTime(DateTimeConstants.MONDAY, new LocalTime(18, 0));
		final DayLocalTime tuesday9am = new DayLocalTime(DateTimeConstants.TUESDAY, new LocalTime(9, 0));

		final DayLocalInterval interval1 = new DayLocalInterval(monday9am, monday9pm);
		final WeekIntervals intervals = new WeekIntervals().add(interval1);

		assertTrue(intervals.contains(monday9am));
		assertTrue(intervals.contains(monday9pm));
		assertFalse(intervals.contains(tuesday9am));
	}
}