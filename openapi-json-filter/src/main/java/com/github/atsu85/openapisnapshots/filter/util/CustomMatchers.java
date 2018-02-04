package com.github.atsu85.openapisnapshots.filter.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class CustomMatchers {
	public static <E> Matcher<Collection<E>> hasAtLeastItemsInAnyOrder(Collection<E> expected) {
		return new HasAtLeastItemsInAnyOrder<>(expected);
	}

	@RequiredArgsConstructor
	private static class HasAtLeastItemsInAnyOrder<C extends Collection<?>> extends BaseMatcher<C> {

		private final C expected;
		private C actual;

		@Override
		public boolean matches(Object actual) {
			if (!(actual instanceof Collection)) {
				return false;
			}
			this.actual = (C) actual;
			return this.actual.containsAll(expected);
		}

		@Override
		public void describeTo(Description description) {
			description
					.appendText("a collection containing at least following values: ")
					.appendValue(expected);
			if (actual != null) {
				description.appendText("\nincluding ");
				List remaining = new ArrayList<>(expected);
				remaining.removeAll(actual);
				description
						.appendValueList("{\n\t", ",\n\t", "\n}", remaining)
						.appendText(" that were not found");
			}
		}

	}

}
