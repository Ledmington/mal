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
package com.ledmington.mal.examples.genetic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

import com.ledmington.mal.genetic.GeneticAlgorithm;
import com.ledmington.mal.genetic.GeneticAlgorithmConfig;
import com.ledmington.mal.genetic.ParallelGeneticAlgorithm;

public final class Knapsack {

	private static final class Solution {
		private final boolean[] array;
		private final int cachedHashCode;

		public Solution(final boolean[] array) {
			this.array = array;
			int h = 17;
			for (final boolean b : array) {
				h = 31 * h + (b ? 1 : 0);
			}
			cachedHashCode = h;
		}

		public boolean get(final int i) {
			return array[i];
		}

		public boolean[] array() {
			return array;
		}

		public int hashCode() {
			return cachedHashCode;
		}

		public boolean equals(final Object other) {
			if (other == null) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (!this.getClass().equals(other.getClass())) {
				return false;
			}
			final Solution s = (Solution) other;
			if (this.array.length != s.array.length) {
				return false;
			}
			for (int i = 0; i < array.length; i++) {
				if (this.array[i] != s.array[i]) {
					return false;
				}
			}
			return true;
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append('[');
			boolean firstElement = true;
			for (int i = 0; i < array.length; i++) {
				if (array[i]) {
					if (firstElement) {
						firstElement = false;
					} else {
						sb.append(", ");
					}
					sb.append(String.format("%2d", i));
				}
			}
			sb.append(']');
			return sb.toString();
		}
	}

	public Knapsack() {
		final long beginning = System.nanoTime();
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
		final int nItems = 100;
		final double[] weights = new double[nItems];
		final double[] values = new double[nItems];
		final double capacity = 20.0;

		System.out.println("Knapsack data:");
		System.out.printf("Knapsack's capacity : %.3f%n", capacity);
		System.out.printf("Number of items : %,d%n", nItems);
		System.out.printf("Total possible solutions : %.3e%n", new BigDecimal(BigInteger.ONE.shiftLeft(nItems)));
		System.out.println();

		System.out.println("Items data:");
		for (int i = 0; i < nItems; i++) {
			weights[i] = rng.nextDouble(0.1, 6.0);
			values[i] = rng.nextDouble(0.1, 6.0);
			System.out.printf("%3d: (w: %.3f; v: %.3f)%n", i, weights[i], values[i]);
		}
		System.out.println();

		final Supplier<GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<Solution>> state =
				() -> GeneticAlgorithmConfig.<Solution>builder()
						.populationSize(1_000)
						.maxGenerations(100)
						.survivalRate(0.1)
						.crossoverRate(0.7)
						.mutationRate(0.2)
						.creation(() -> {
							final boolean[] v = new boolean[nItems];

							for (int i = 0; i < nItems; i++) {
								v[i] = rng.nextBoolean();
							}

							return new Solution(v);
						})
						.crossover((a, b) -> {
							final boolean[] v = new boolean[nItems];

							for (int i = 0; i < nItems; i++) {
								v[i] = rng.nextBoolean() ? a.get(i) : b.get(i);
							}

							return new Solution(v);
						})
						.mutation(x -> {
							final boolean[] v = new boolean[nItems];
							System.arraycopy(x.array(), 0, v, 0, nItems);
							final int idx = rng.nextInt(0, nItems);
							v[idx] = !v[idx];
							return new Solution(v);
						})
						.maximize(x -> {
							double totalWeight = 0.0;
							double s = 0.0;
							int n = 0;
							for (int i = 0; i < nItems; i++) {
								if (x.get(i)) {
									s += values[i];
									totalWeight += weights[i];
									n++;
								}
							}

							final double validSolutionPrize = 1_000.0;
							if (totalWeight > capacity) {
								return (double) (nItems - n);
							}
							return s + validSolutionPrize;
						});

		final GeneticAlgorithm<Solution> ga =
				new ParallelGeneticAlgorithm<>(Runtime.getRuntime().availableProcessors());
		Set<Solution> g = new HashSet<>();
		final Set<Solution> allSolutions = new HashSet<>();

		for (int it = 0; it < 10; it++) {
			System.out.printf("Run n.%,d%n", it);
			ga.setState(state.get().firstGeneration(g).build());
			ga.run();

			final Map<Solution, Double> scores = ga.getState().scores();
			allSolutions.addAll(scores.keySet());
			scores.entrySet().stream()
					.sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
					.limit(10)
					.forEach(e -> {
						double totalWeight = 0;
						for (int i = 0; i < nItems; i++) {
							if (e.getKey().get(i)) {
								totalWeight += weights[i];
							}
						}
						System.out.printf(
								"%s -> (total-weight: %.3f; total-value: %.3f)%n",
								e.getKey(), totalWeight, e.getValue());
					});
			g = scores.entrySet().stream()
					.sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
					.limit(10)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
			System.out.println();
		}

		final long end = System.nanoTime();

		System.out.printf("%n%,d solutions evaluated%n", allSolutions.size());
		System.out.printf("Total search time: %.3f seconds%n", (double) (end - beginning) / 1_000_000_000.0);
	}
}
