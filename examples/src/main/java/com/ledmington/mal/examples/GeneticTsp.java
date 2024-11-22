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
package com.ledmington.mal.examples;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

import com.ledmington.mal.GeneticAlgorithm;
import com.ledmington.mal.GeneticAlgorithmConfig;
import com.ledmington.mal.ParallelGeneticAlgorithm;

public final class GeneticTsp {

	private static final RandomGenerator rng =
			RandomGeneratorFactory.getDefault().create(System.nanoTime());

	private static void shuffle(int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			int j;
			do {
				j = rng.nextInt(0, arr.length);
			} while (i == j);
			int tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
		}
	}

	private static final class Solution {

		private final int[] array;
		private final int cachedHashCode;

		public Solution(final int[] v) {
			this.array = Objects.requireNonNull(v);

			int h = 17;
			for (final int j : v) {
				h = 31 * h + j;
			}
			cachedHashCode = h;
		}

		public int[] array() {
			return array;
		}

		public int get(final int i) {
			return array[i];
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
			if (s.array.length != this.array.length) {
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
			for (final int j : array) {
				if (firstElement) {
					firstElement = false;
				} else {
					sb.append(", ");
				}
				sb.append(String.format("%2d", j));
			}
			sb.append(']');
			return sb.toString();
		}
	}

	private static BigInteger factorial(BigInteger n) {
		if (n.compareTo(BigInteger.ZERO) == 0 || n.compareTo(BigInteger.ONE) == 0) {
			return BigInteger.ONE;
		}
		BigInteger x = BigInteger.ONE;
		while (n.compareTo(BigInteger.ZERO) > 0) {
			x = x.multiply(n);
			n = n.subtract(BigInteger.ONE);
		}
		return x;
	}

	public GeneticTsp() {
		final long beginning = System.nanoTime();
		final int nCities = 30;
		final double[][] coordinates = new double[2][nCities];

		System.out.println("Traveling Salesman Problem's data:");
		System.out.printf("Number of cities : %,d%n", nCities);
		System.out.printf(
				"Number of unique paths : %.3e%n",
				new BigDecimal(factorial(BigInteger.valueOf(nCities - 1)).divide(BigInteger.TWO)));

		System.out.println();
		System.out.println("Cities coordinates:");
		for (int i = 0; i < nCities; i++) {
			coordinates[0][i] = rng.nextDouble(-10.0, 10.0);
			coordinates[1][i] = rng.nextDouble(-10.0, 10.0);
			System.out.printf("%2d: (%+.3f; %+.3f)%n", i, coordinates[0][i], coordinates[1][i]);
		}
		System.out.println();

		final double[][] distances = new double[nCities][nCities];

		for (int i = 0; i < nCities; i++) {
			for (int j = 0; j < nCities; j++) {
				// euclidean distance
				final double dist =
						Math.hypot(coordinates[0][i] - coordinates[0][j], coordinates[1][i] - coordinates[1][j]);
				distances[i][j] = dist;
				distances[j][i] = dist;
			}
		}

		final Supplier<GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<Solution>> state =
				() -> GeneticAlgorithmConfig.<Solution>builder()
						.populationSize(1_000)
						.maxGenerations(100)
						.survivalRate(0.2)
						.crossoverRate(0.7)
						.mutationRate(0.2)
						.creation(() -> {
							final int[] v = new int[nCities];
							for (int i = 0; i < nCities; i++) {
								v[i] = i;
							}
							shuffle(v);
							return new Solution(v);
						})
						.crossover((a, b) -> {
							final int[] result = new int[nCities];

							for (int i = 0; i < nCities; i++) {
								result[i] = rng.nextBoolean() ? a.get(i) : b.get(i);
							}

							return new Solution(result);
						})
						.mutation(x -> {
							final int[] y = new int[nCities];
							System.arraycopy(x.array(), 0, y, 0, nCities);
							final int i = rng.nextInt(0, nCities);
							y[i] = rng.nextInt(0, nCities);
							return new Solution(y);
						})
						.minimize(x -> {
							final boolean[] visited = new boolean[nCities];
							int visitedCities = 0;
							double s = 0.0;
							for (int i = 0; i < nCities; i++) {
								if (!visited[x.get(i)]) {
									visitedCities++;
								}
								visited[x.get(i)] = true;
								s += distances[x.array()[i]][x.array()[(i + 1) % nCities]];
							}
							if (visitedCities < nCities) {
								// This solution has not visited all cities, so this solution is not valid: we
								// return a really high value (not infinity for now)
								// FIXME: allow usage of infinities
								return 1_000_000.0;
							}
							return s;
						});

		final ExecutorService ex =
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final GeneticAlgorithm<Solution> ga = new ParallelGeneticAlgorithm<>(ex, rng);
		Set<Solution> g = new HashSet<>();
		final Set<Solution> allSolutions = new HashSet<>();

		for (int it = 0; it < 10; it++) {
			System.out.printf("Run n.%,d%n", it);
			ga.setState(state.get().firstGeneration(g).build());
			ga.run();

			final Map<Solution, Double> scores = ga.getState().scores();
			allSolutions.addAll(scores.keySet());
			scores.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.limit(10)
					.forEach(e -> System.out.printf("%s -> %f%n", e.getKey(), e.getValue()));
			g = scores.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.limit(10)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
			System.out.println();
		}

		final long end = System.nanoTime();

		System.out.printf("%n%,d solutions evaluated%n", allSolutions.size());
		System.out.printf("Total search time: %.3f seconds%n", (double) (end - beginning) / 1_000_000_000.0);

		if (!ex.isShutdown()) {
			ex.shutdown();
		}
		while (!ex.isTerminated()) {
			try {
				if (ex.awaitTermination(1, TimeUnit.SECONDS)) {
					break;
				}
			} catch (final InterruptedException ignored) {
			}
		}
	}
}
