/*
* genetic-algorithms-library - A library for genetic algorithms.
* Copyright (C) 2023-2023 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.gal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public final class TestUtils {

    private static final RandomGenerator rng =
            RandomGeneratorFactory.getDefault().create(System.nanoTime());

    @Test
    public void weightedChooseCanChooseFirst() {
        final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final Function<Integer, Double> w = x -> 9.1 - x;

        assertTrue(IntStream.range(0, 100)
                .map(x -> Utils.weightedChoose(values, w, rng))
                .filter(x -> values.get(0).equals(x))
                .findAny()
                .isPresent());
    }

    @Test
    public void weightedChooseCanChooseLast() {
        final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final Function<Integer, Double> w = x -> (double) x;

        assertTrue(IntStream.range(0, 100)
                .map(x -> Utils.weightedChoose(values, w, rng))
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
            final Integer chosen = Utils.weightedChoose(values, w, rng);
            count.put(chosen, count.get(chosen) + 1);
        }

        for (int i = 0; i < values.size() - 1; i++) {
            final Integer first = values.get(i);
            final Integer second = values.get(i + 1);
            assertTrue(
                    count.get(first) < count.get(second),
                    String.format(
                            "Value %d (with weight %f) appeared more often than value %d (with weight %f): %,d > %,d",
                            first, w.apply(first), second, w.apply(second), count.get(first), count.get(second)));
        }
    }

    @Test
    public void negativeWeightsWork() {
        final List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final Function<Integer, Double> w = x -> -(double) x;
        final Map<Integer, Integer> count = new HashMap<>();
        for (final Integer x : values) {
            count.put(x, 0);
        }

        for (int i = 0; i < 10_000; i++) {
            final Integer chosen = Utils.weightedChoose(values, w, rng);
            count.put(chosen, count.get(chosen) + 1);
        }

        for (int i = 0; i < values.size() - 1; i++) {
            final Integer first = values.get(i);
            final Integer second = values.get(i + 1);
            assertTrue(
                    count.get(first) > count.get(second),
                    String.format(
                            "Value %d (with weight %f) appeared more often than value %d (with weight %f): %,d < %,d",
                            second, w.apply(second), first, w.apply(first), count.get(second), count.get(first)));
        }
    }
}
