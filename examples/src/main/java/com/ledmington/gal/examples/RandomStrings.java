/*
* genetic-algorithms-library - A library for genetic algorithms.
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
package com.ledmington.gal.examples;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.SerialGeneticAlgorithm;

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
                        .crossoverRate(0.7)
                        .mutationRate(0.2)
                        .creation(() -> {
                            final StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < targetLength; i++) {
                                sb.append(randomChar.get());
                            }
                            return sb.toString();
                        })
                        .crossover((a, b) -> {
                            final StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < targetLength; i++) {
                                sb.append(rng.nextBoolean() ? a.charAt(i) : b.charAt(i));
                            }
                            return sb.toString();
                        })
                        .mutation(s -> {
                            final int idx = rng.nextInt(0, targetLength);
                            return s.substring(0, idx) + randomChar.get() + s.substring(idx + 1, targetLength);
                        })
                        .maximize(s -> {
                            if (s.length() != targetLength) {
                                throw new IllegalArgumentException(String.format(
                                        "Invalid length: was %d but should have been %d", s.length(), targetLength));
                            }
                            int count = 0;
                            for (int i = 0; i < targetLength; i++) {
                                if (s.charAt(i) == targetString.charAt(i)) {
                                    count++;
                                }
                            }
                            return (double) count;
                        })
                        .stopCriterion(s -> s.equals(targetString));

        final GeneticAlgorithm<String> ga = new SerialGeneticAlgorithm<>(rng);
        Set<String> g = new HashSet<>();
        final Set<String> allSolutions = new HashSet<>();

        for (int it = 0; it < 10; it++) {
            System.out.printf("Run n.%,d\n", it);
            ga.setState(state.get().firstGeneration(g).build());
            ga.run();

            final Map<String, Double> scores = ga.getState().scores();
            allSolutions.addAll(scores.keySet());
            scores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(10)
                    .forEach(e -> {
                        System.out.printf("%s -> %f\n", e.getKey(), e.getValue());
                    });
            g = scores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
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
