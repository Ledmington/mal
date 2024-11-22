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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.ledmington.mal.GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder;

public abstract class GATest {

	protected GeneticAlgorithm<String> ga;

	@BeforeEach
	public abstract void setup();

	// A Supplier that counts the number of times it has been invoked
	static class CountingSupplier implements Supplier<String> {

		private int count = 0;

		@Override
		public synchronized String get() {
			count++;
			return "" + count;
		}

		public int getCount() {
			return count;
		}
	}

	// A Mutator that counts the number of times it has been invoked
	static class CountingMutator implements Function<String, String> {
		private int count = 0;

		public synchronized String apply(final String in) {
			count++;
			return in;
		}

		public int getCount() {
			return count;
		}
	}

	// A Crossover operator that counts the number of times it has been invoked
	static class CountingCrossoverOperator implements BiFunction<String, String, String> {
		private int count = 0;

		public synchronized String apply(final String first, final String second) {
			count++;
			return first;
		}

		public int getCount() {
			return count;
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {10, 20, 30, 40, 50, 60, 70, 80, 90})
	public void zeroGenerationsMeansOnlyCreation(final int populationSize) {
		final CountingSupplier cs = new CountingSupplier();
		ga.setState(GeneticAlgorithmConfig.<String>builder()
				.populationSize(populationSize)
				.maxGenerations(0)
				.crossover((a, b) -> b)
				.mutation(Function.identity())
				.maximize(s -> 0.0)
				.creation(cs)
				.build());
		ga.run();
		assertEquals(populationSize, cs.getCount());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
	public void zeroRatesMeansAlwaysRandomCreations(final int generations) {
		/*
		The survival rate, the mutation rate and the crossover rate cannot be technically zero,
		but we can give 1e-6 so that less than one individual will survive,
		less than one individual will be mutated and less than one crossover will be performed.
		*/
		final int maxPopulation = 100;
		final CountingSupplier cs = new CountingSupplier();
		final CountingMutator cm = new CountingMutator();
		final CountingCrossoverOperator cco = new CountingCrossoverOperator();

		ga.setState(GeneticAlgorithmConfig.<String>builder()
				.populationSize(maxPopulation)
				.maxGenerations(generations)
				.survivalRate(1e-6)
				.crossoverRate(1e-6)
				.mutationRate(1e-6)
				.crossover(cco)
				.mutation(cm)
				.creation(cs)
				.maximize(s -> (double) s.length())
				.build());
		ga.run();

		assertEquals(0, cco.getCount()); // zero crossovers
		assertEquals(0, cm.getCount()); // zero mutations
		assertEquals(maxPopulation * (generations + 1), cs.getCount()); // only random creations
	}

	@Test
	public void ifQuietShouldPrintNothing() {
		final PrintStream oldStdout = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			public void write(int b) {
				// reset the old stdout object to avoid bad stuff
				System.setOut(oldStdout);
				fail("This test was not supposed to print anything");
			}
		}));

		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());

		ga.setState(GeneticAlgorithmConfig.<String>builder()
				.maxGenerations(100)
				.creation(() -> String.valueOf(rng.nextInt()))
				.crossover((a, b) -> String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)))
				.mutation(x -> String.valueOf(Integer.parseInt(x) + 1))
				.maximize(s -> (double) s.length())
				.build());
		ga.run();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 5, 10, 20, 30, 40, 50})
	public void maxGenerations(int generations) {
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
		final GeneticAlgorithmConfigBuilder<String> gacb = GeneticAlgorithmConfig.<String>builder()
				.maxGenerations(generations)
				.creation(() -> String.valueOf(rng.nextInt()))
				.crossover((a, b) -> String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)))
				.mutation(x -> String.valueOf(Integer.parseInt(x) + 1))
				.maximize(s -> (double) s.length());

		ga.setState(gacb.build());
		ga.run();

		assertEquals(generations, ga.getState().generation());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 9, 99, 999, 9_999, 99_999, 999_999})
	public void stopCriterion(int limit) {
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
		final GeneticAlgorithmConfigBuilder<String> gacb = GeneticAlgorithmConfig.<String>builder()
				.stopCriterion(s -> Integer.parseInt(s) >= limit)
				.creation(() -> String.valueOf(rng.nextInt()))
				.crossover((a, b) -> String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)))
				.mutation(x -> String.valueOf(Integer.parseInt(x) + 1))
				.maximize(s -> (double) s.length());

		ga.setState(gacb.build());
		ga.run();

		assertTrue(
				// we check just the population of the latest generation because the check is performed before the
				// "computeScores" phase
				ga.getState().population().stream().anyMatch(s -> Integer.parseInt(s) >= limit),
				"There should have been at least one element satisfying the given criterion, but none was found");
	}
}
