/*
* minimization-algorithms-library - A collection of minimization algorithms.
* Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.mal.genetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public final class ParallelGATest extends GATest {
	@BeforeEach
	public void setup() {
		ga = new ParallelGeneticAlgorithm<>();
	}

	@ParameterizedTest
	@ValueSource(ints = {-2, -1, 0})
	public void invalidThreads(final int nThreads) {
		assertThrows(IllegalArgumentException.class, () -> new ParallelGeneticAlgorithm<String>(nThreads));
	}

	private int tryWithSeed(final long seed) {
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(seed);
		final GeneticAlgorithm<Integer> ga = new ParallelGeneticAlgorithm<>(
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), rng);
		ga.setState(GeneticAlgorithmConfig.<Integer>builder()
				.maxGenerations(10)
				.creation(() -> rng.nextInt())
				.crossover((a, b) -> (a + b) / 2)
				.mutation(x -> x + 1)
				.maximize(x -> (double) Math.abs(x))
				.build());
		ga.run();
		return ga.getState().scores().entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.orElseThrow()
				.getKey();
	}

	@Test
	public void determinism() {
		// two algorithms with the same config and the same RandomGenerator must return the same result (the best
		// solution)
		final long seed = System.nanoTime();

		final int firstBest = tryWithSeed(seed);
		final int secondBest = tryWithSeed(seed);

		assertEquals(firstBest, secondBest);
	}
}
