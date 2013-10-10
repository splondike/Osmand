package net.osmand.plus.happycow;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

/**
 * An immutable collection of time intervals in a week, e.g. Mon-Fri 11-2pm, Sat 9am-3pm. Allows user to check
 * whether a time is within the included intervals.
 */
public class WeekIntervals {
	private final Set<DayLocalInterval> intervals;

	/* package */ WeekIntervals() {
		this.intervals = new HashSet<DayLocalInterval>();
	}

	private WeekIntervals(Set<DayLocalInterval> intervals) {
		this.intervals = intervals;
	}

	public boolean contains(DateTime time) {
		DayLocalTime dlt = new DayLocalTime(time.getDayOfWeek(),
			new LocalTime(time.hourOfDay().get(), time.getMinuteOfHour()));
		return this.contains(dlt);
	}

	/* package */ boolean contains(DayLocalTime time) {
		for(DayLocalInterval interval : this.intervals) {
			if (interval.contains(time)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds an interval to the collection, and returns a reference to the new collection.
	 * 
	 * @param day The day to add
	 * @param startTime The start time of the range
	 * @param endTime The end time of the range
	 * @return A new WeekIntervals object with the given range added.
	 */
	/* package */ WeekIntervals add(DayLocalInterval interval) {
		final Set<DayLocalInterval> newSet = new HashSet<DayLocalInterval>(this.intervals);
		newSet.add(interval);
		return new WeekIntervals(newSet);
	}

	/**
	 * This isn't called equals to remind me to make it check for equivalence, rather than simple of the intervals sets.
	 * TODO: Doesn't currently do this.
	 */
	public boolean equivalent(WeekIntervals other) {
		return this.intervals.equals(other.intervals);
	}

	@Override
	public String toString() {
		return "WeekIntervals " + intervals;
	}

	/**
	 * Immutable interval on a given day of the week.
	 */
	/* package */ static class DayLocalInterval {
		private final DayLocalTime startTime;
		private final DayLocalTime endTime;

		public DayLocalInterval(DayLocalTime startTime, DayLocalTime endTime) {
			assert(startTime.day == endTime.day);
			assert(this.startTime.time.compareTo(this.endTime.time) <= 0);
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public boolean contains(DayLocalTime other) {
			if (other.day != this.startTime.day) return false;
			final boolean moreThanOrEqualsStart = this.startTime.time.compareTo(other.time) <= 0;
			final boolean lessThanOrEqualsEnd = this.endTime.time.compareTo(other.time) >= 0;

			return moreThanOrEqualsStart && lessThanOrEqualsEnd;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((endTime == null) ? 0 : endTime.hashCode());
			result = prime * result
					+ ((startTime == null) ? 0 : startTime.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DayLocalInterval other = (DayLocalInterval) obj;
			if (endTime == null) {
				if (other.endTime != null)
					return false;
			} else if (!endTime.equals(other.endTime))
				return false;
			if (startTime == null) {
				if (other.startTime != null)
					return false;
			} else if (!startTime.equals(other.startTime))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "DayLocalInterval [startTime=" + startTime + ", endTime="
					+ endTime + "]";
		}
	}

	/**
	 * Immutable time on a day of the week.
	 */
	/* package */ static class DayLocalTime {
		private final Integer day;
		private final LocalTime time;
		public DayLocalTime(Integer day, LocalTime time) {
			assert(day > 0 && day < 8);
			this.day = day;
			this.time = time;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((day == null) ? 0 : day.hashCode());
			result = prime * result + ((time == null) ? 0 : time.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DayLocalTime other = (DayLocalTime) obj;
			if (day == null) {
				if (other.day != null)
					return false;
			} else if (!day.equals(other.day))
				return false;
			if (time == null) {
				if (other.time != null)
					return false;
			} else if (!time.equals(other.time))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "DayLocalTime [day=" + day + ", time=" + time + "]";
		}
	}
}