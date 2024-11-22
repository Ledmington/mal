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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

import com.ledmington.mal.GeneticAlgorithm;
import com.ledmington.mal.GeneticAlgorithmConfig;
import com.ledmington.mal.SerialGeneticAlgorithm;

public final class RandomStrings {
	public RandomStrings() {
		final long beginning = System.nanoTime();
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
		final String alphabet =
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,:;-_@#[]{}()!?='\"+*/";
		final String targetString =
				"This library for Genetic Algorithms is absolutely fantastic! I cannot wait to try and use it with Java 17 and Gradle 8.7! Now let's write another time just to have longer strings and, therefore, add artificial complexity to the problem.";
		final int targetLength = targetString.length();
		final Supplier<Character> randomChar = () -> alphabet.charAt(rng.nextInt(0, alphabet.length()));

		final Supplier<GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<String>> state =
				() -> GeneticAlgorithmConfig.<String>builder()
						.populationSize(1_000)
						.maxGenerations(100)
						.survivalRate(0.1)
						.crossoverRate(0.3)
						.mutationRate(0.7)
						.creation(() -> {
							final StringBuilder sb = new StringBuilder();
							final int len = rng.nextInt(1, targetLength);
							for (int i = 0; i < len; i++) {
								sb.append(randomChar.get());
							}
							return sb.toString();
						})
						.crossover((a, b) -> {
							final StringBuilder sb = new StringBuilder();
							final int minLength = Math.min(a.length(), b.length());
							final int maxLength = Math.max(a.length(), b.length());
							final int len = (minLength == maxLength) ? minLength : rng.nextInt(minLength, maxLength);
							int i = 0;
							for (; i < minLength; i++) {
								sb.append(rng.nextBoolean() ? a.charAt(i) : b.charAt(i));
							}
							for (; i < len; i++) {
								if (i < a.length()) {
									sb.append(a.charAt(i));
								} else {
									sb.append(b.charAt(i));
								}
							}
							return sb.toString();
						})
						.mutation(s -> {
							enum Choice {
								ADD,
								REMOVE,
								REPLACE
							};
							Choice choice =
									s.isEmpty() ? Choice.ADD : Choice.values()[rng.nextInt(0, Choice.values().length)];
							return switch (choice) {
								case ADD -> // we add a new character at the end
								s + randomChar.get();
								case REMOVE -> {
									// we remove a random character
									final int idx = rng.nextInt(0, s.length());
									yield s.substring(0, idx) + s.substring(idx + 1);
								}
								case REPLACE -> {
									// we replace a character with a different one
									final int idx = rng.nextInt(0, s.length());
									yield s.substring(0, idx) + randomChar.get() + s.substring(idx + 1);
								}
							};
						})
						.minimize(s -> {
							int count = Math.abs(targetLength - s.length());
							for (int i = 0; i < Math.min(targetLength, s.length()); i++) {
								if (s.charAt(i) != targetString.charAt(i)) {
									count++;
								}
							}
							return (double) count;
						})
						.stopCriterion(s -> s.equals(targetString));

		final GeneticAlgorithm<String> ga = new SerialGeneticAlgorithm<>(rng);
		Set<String> g = new HashSet<>();
		final Set<String> allSolutions = new HashSet<>();

		for (int it = 0; it < 100; it++) {
			System.out.printf("Run n.%,d\n", it);
			ga.setState(state.get().firstGeneration(g).build());
			ga.run();

			final Map<String, Double> scores = ga.getState().scores();
			allSolutions.addAll(scores.keySet());
			scores.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.limit(10)
					.forEach(e -> {
						System.out.printf("%s -> %f\n", e.getKey(), e.getValue());
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
	}
}
