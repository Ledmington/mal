/*
* minimization-algorithms-library - A collection of minimization algorithms.
* Copyright (C) 2023-2024 Filippo Barbari <filippo.barbari@gmail.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ledmington.mal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class TestUtils {

	private static final RandomGenerator rng =
			RandomGeneratorFactory.getDefault().create(System.nanoTime());

	public static Stream<Arguments> inputs() {
		return Stream.of(
				Arguments.of(List.of(1, 2, 3), Function.identity(), null),
				Arguments.of(List.of(1, 2, 3), null, rng),
				Arguments.of(null, Function.identity(), rng));
	}

	@ParameterizedTest
	@MethodSource("inputs")
	public void nullChecks(final List<Integer> v, final Function<Integer, Double> w, final RandomGenerator r) {
		assertThrows(NullPointerException.class, () -> Utils.weightedChoose(v, w, r));
	}

	@Test
	public void weightedChooseCanChooseFirst() {
		final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		final Function<Integer, Double> w = x -> 9.1 - x;

		assertTrue(IntStream.range(0, 100)
				.map(x -> Utils.weightedChoose(values, w, rng).get())
				.filter(x -> values.get(0).equals(x))
				.findAny()
				.isPresent());
	}

	@Test
	public void weightedChooseCanChooseLast() {
		final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		final Function<Integer, Double> w = x -> (double) x;

		assertTrue(IntStream.range(0, 100)
				.map(x -> Utils.weightedChoose(values, w, rng).get())
				.filter(x -> values.get(values.size() - 1).equals(x))
				.findAny()
				.isPresent());
	}

	@Test
	public void weightsWork() {
		final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		final Function<Integer, Double> w = x -> (double) x;
		final Map<Integer, Integer> count = new HashMap<>();
		for (final Integer x : values) {
			count.put(x, 0);
		}

		for (int i = 0; i < 10_000; i++) {
			final Integer chosen = Utils.weightedChoose(values, w, rng).get();
			count.put(chosen, count.get(chosen) + 1);
		}

		for (int i = 0; i < values.size() - 1; i++) {
			final Integer first = values.get(i);
			final Integer second = values.get(i + 1);
			assertTrue(
					count.get(first) > 0,
					String.format("Value %d (with weight %f) did not appear once", first, w.apply(first)));
			assertTrue(
					count.get(first) < count.get(second),
					String.format(
							"Value %d (with weight %f) appeared more often than value %d (with weight %f): %,d > %,d",
							first, w.apply(first), second, w.apply(second), count.get(first), count.get(second)));
		}
	}

	@Test
	public void negativeWeightsDoNotWork() {
		final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		final Function<Integer, Double> w = x -> -(double) x;

		assertThrows(IllegalArgumentException.class, () -> Utils.weightedChoose(values, w, rng));
	}

	@Test
	public void weightedChooseWithOneValue() {
		assertEquals(
				123, Utils.weightedChoose(List.of(123), x -> (double) x, rng).get());
	}

	@Test
	public void weightedChooseWithNoValues() {
		assertThrows(IllegalArgumentException.class, () -> Utils.weightedChoose(List.of(), x -> (double) x, rng));
	}
}
