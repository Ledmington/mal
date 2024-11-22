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

import java.util.Arrays;
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

public final class Diet {

	private enum Food {
		BANANA("banana", 0.89, 890.0, 0.93, 0.04, 0.03),
		CARROT("carrot", 0.54, 410.0, 0.86, 0.9, 0.05),
		APPLE("apple", 0.94, 521.978, 0.2653, 0.0045, 0.00317),
		PIZZA("pizza", 5.0, 2710.0, 0.33, 0.11, 0.1),
		STEAK("steak", 15.49, 2105.882, 0.0, 0.1452, 0.0424),
		LENTILS("lentils", 1.39, 1175.0, 0.1702, 0.25, 0.01846),
		PARMESAN("parmesan", 10.55, 4310.0, 0.003, 0.3463, 0.6535);

		private final String name;
		private final double avgPricePerKilo;
		private final double avgCaloriesPerKilo;
		private final double avgCarbsPerKilo;
		private final double avgProteinsPerKilo;
		private final double avgFatsPerKilo;

		Food(
				final String name,
				final double avgPricePerKilo,
				final double avgCaloriesPerKilo,
				final double carbsPercentage,
				final double proteinsPercentage,
				final double fatsPercentage) {
			this.name = Objects.requireNonNull(name);
			if (avgPricePerKilo < 0.0) {
				throw new IllegalArgumentException("Invalid price per kilo");
			}
			if (avgCaloriesPerKilo < 0.0) {
				throw new IllegalArgumentException("Invalid calories per kilo");
			}
			if (carbsPercentage < 0.0 || carbsPercentage > 1.0) {
				throw new IllegalArgumentException(String.format("Invalid carbs percentage %f", carbsPercentage));
			}
			if (proteinsPercentage < 0.0 || proteinsPercentage > 1.0) {
				throw new IllegalArgumentException(String.format("Invalid proteins percentage %f", proteinsPercentage));
			}
			if (fatsPercentage < 0.0 || fatsPercentage > 1.0) {
				throw new IllegalArgumentException(String.format("Invalid fats percentage %f", fatsPercentage));
			}
			this.avgPricePerKilo = avgPricePerKilo;
			this.avgCaloriesPerKilo = avgCaloriesPerKilo;
			this.avgCarbsPerKilo = avgCaloriesPerKilo * carbsPercentage;
			this.avgProteinsPerKilo = avgCaloriesPerKilo * proteinsPercentage;
			this.avgFatsPerKilo = avgCaloriesPerKilo * fatsPercentage;
		}
	}

	private static final class Solution {

		// These represent kilograms of each food
		private final double[] quantities = new double[Food.values().length];
		private final int cachedHashCode;

		public Solution(final double[] quantities) {
			Objects.requireNonNull(quantities);
			if (quantities.length != Food.values().length) {
				throw new IllegalArgumentException("Invalid length of food quantities");
			}
			System.arraycopy(quantities, 0, this.quantities, 0, Food.values().length);
			int h = 17;
			for (final double q : quantities) {
				final long x = Double.doubleToLongBits(q);
				h = 31 * h + (int) (x >>> 32);
				h = 31 * h + (int) (x & 0x00000000ffffffffL);
			}
			cachedHashCode = h;
		}

		public double get(final int index) {
			return quantities[index];
		}

		public int hashCode() {
			return cachedHashCode;
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < quantities.length; i++) {
				if (quantities[i] < 1e-3) { // avoid printing quantities less than 1g
					continue;
				}
				sb.append(String.format("%.3fkg of %s; ", quantities[i], Food.values()[i].name));
			}
			return sb.toString();
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
			return Arrays.equals(this.quantities, ((Solution) other).quantities);
		}
	}

	public Diet() {
		final long beginning = System.nanoTime();
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());

		final Supplier<GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<Solution>> state =
				() -> GeneticAlgorithmConfig.<Solution>builder()
						.populationSize(1_000)
						.maxGenerations(100)
						.survivalRate(0.1)
						.crossoverRate(0.5)
						.mutationRate(0.3)
						.creation(() -> {
							final double[] v = new double[Food.values().length];
							for (int i = 0; i < v.length; i++) {
								v[i] = rng.nextDouble(0.0, 10.0);
							}
							return new Solution(v);
						})
						.crossover((a, b) -> {
							final double[] v = new double[Food.values().length];
							for (int i = 0; i < v.length; i++) {
								v[i] = rng.nextBoolean() ? a.get(i) : b.get(i);
							}
							return new Solution(v);
						})
						.mutation(x -> {
							final double[] v = new double[Food.values().length];
							for (int i = 0; i < v.length; i++) {
								v[i] = x.get(i);
							}
							final int idx = rng.nextInt(0, Food.values().length);
							v[idx] = rng.nextDouble(Math.max(0.0, v[idx] - 1.0), v[idx] + 1.0);
							return new Solution(v);
						})
						.minimize(x -> {
							final double minCalories = 2200.0;
							final double maxCalories = 2700.0;
							final double minCarbsPercentage = 0.45;
							final double maxCarbsPercentage = 0.55;
							final double minProteinsPercentage = 0.1;
							final double maxProteinsPercentage = 0.35;
							final double minFatsPercentage = 0.2;
							final double maxFatsPercentage = 0.35;

							double score = 0.0;
							double calories = 0.0;
							double carbs = 0.0;
							double proteins = 0.0;
							double fats = 0.0;
							for (int i = 0; i < Food.values().length; i++) {
								score += x.get(i) * Food.values()[i].avgPricePerKilo;
								calories += x.get(i) * Food.values()[i].avgCaloriesPerKilo;
								carbs += x.get(i) * Food.values()[i].avgCarbsPerKilo;
								proteins += x.get(i) * Food.values()[i].avgProteinsPerKilo;
								fats += x.get(i) * Food.values()[i].avgFatsPerKilo;
							}

							if (calories < minCalories) {
								score += (minCalories - calories) * (minCalories - calories);
							} else if (calories > maxCalories) {
								score += (calories - maxCalories) * (calories - maxCalories);
							}

							final double minCarbs = calories * minCarbsPercentage;
							final double maxCarbs = calories * maxCarbsPercentage;
							if (carbs < minCarbs) {
								score += (minCarbs - carbs) * (minCarbs - carbs);
							} else if (carbs > maxCarbs) {
								score += (carbs - maxCarbs) * (carbs - maxCarbs);
							}

							final double minProteins = calories * minProteinsPercentage;
							final double maxProteins = calories * maxProteinsPercentage;
							if (proteins < minProteins) {
								score += (minProteins - proteins) * (minProteins - proteins);
							} else if (proteins > maxProteins) {
								score += (proteins - maxProteins) * (proteins - maxProteins);
							}

							final double minFats = calories * minFatsPercentage;
							final double maxFats = calories * maxFatsPercentage;
							if (fats < minFats) {
								score += (minFats - fats) * (minFats - fats);
							} else if (fats > maxFats) {
								score += (fats - maxFats) * (fats - maxFats);
							}

							return score;
						});

		final ExecutorService ex =
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final GeneticAlgorithm<Solution> ga = new ParallelGeneticAlgorithm<>(ex, rng);
		Set<Solution> g = new HashSet<>();
		final Set<Solution> allSolutions = new HashSet<>();

		for (int it = 0; it < 10; it++) {
			System.out.printf("Run n.%,d\n", it);
			ga.setState(state.get().firstGeneration(g).build());
			ga.run();

			final Map<Solution, Double> scores = ga.getState().scores();
			allSolutions.addAll(scores.keySet());
			scores.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.limit(10)
					.forEach(e -> {
						double p = 0.0;
						for (int i = 0; i < Food.values().length; i++) {
							p += e.getKey().get(i) * Food.values()[i].avgPricePerKilo;
						}
						System.out.printf("%s -> %f (%.2f$)\n", e.getKey(), e.getValue(), p);
					});
			g = scores.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.limit(10)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
			System.out.println();
		}

		final long end = System.nanoTime();

		System.out.printf("\n%,d solutions evaluated\n", allSolutions.size());
		System.out.printf("Total search time: %.3f seconds\n", (double) (end - beginning) / 1_000_000_000.0);

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
