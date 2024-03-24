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

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.ParallelGeneticAlgorithm;

public final class Diet {

    private enum Food {
        BANANA("banana", 0.89f, 890.0f, 827.7f, 35.6f, 26.7f),
        CARROT("carrot", 0.54f, 410.0f, 356.7f, 32.8f, 20.5f),
        APPLE("apple", 0.94f, 521.978f, 495.8791f, 10.439f, 15.659f),
        PIZZA("pizza", 5.0f, 2710.0f, 894.3f, 298.1f, 271.0f),
        STEAK("steak", 15.49f, 2105.882f, 0.0f, 305.882f, 89.411f),
        LENTILS("lentils", 1.39f, 1175.0f, 200.0f, 293.75f, 21.7f);

        private final String name;
        private final float avgPricePerKilo;
        private final float avgCaloriesPerKilo;
        private final float avgCarbsPerKilo;
        private final float avgProteinsPerKilo;
        private final float avgFatsPerKilo;

        Food(
                final String name,
                final float avgPricePerKilo,
                final float avgCaloriesPerKilo,
                final float avgCarbsPerKilo,
                final float avgProteinsPerKilo,
                final float avgFatsPerKilo) {
            this.name = Objects.requireNonNull(name);
            if (avgPricePerKilo < 0.0f) {
                throw new IllegalArgumentException("Invalid price per kilo");
            }
            if (avgCaloriesPerKilo < 0.0f) {
                throw new IllegalArgumentException("Invalid calories per kilo");
            }
            if (avgCarbsPerKilo < 0.0f) {
                throw new IllegalArgumentException("Invalid carbs per kilo");
            }
            if (avgProteinsPerKilo < 0.0f) {
                throw new IllegalArgumentException("Invalid proteins per kilo");
            }
            if (avgFatsPerKilo < 0.0f) {
                throw new IllegalArgumentException("Invalid fats per kilo");
            }
            this.avgPricePerKilo = avgPricePerKilo;
            this.avgCaloriesPerKilo = avgCaloriesPerKilo;
            this.avgCarbsPerKilo = avgCarbsPerKilo;
            this.avgProteinsPerKilo = avgProteinsPerKilo;
            this.avgFatsPerKilo = avgFatsPerKilo;
        }
    }

    private static final class Solution {

        // These represent kilograms of each food
        private final float[] quantities = new float[Food.values().length];
        private final int cachedHashCode;

        public Solution(final float[] quantities) {
            Objects.requireNonNull(quantities);
            if (quantities.length != Food.values().length) {
                throw new IllegalArgumentException("Invalid length of food quantities");
            }
            System.arraycopy(quantities, 0, this.quantities, 0, Food.values().length);
            int h = 17;
            for (final float f : quantities) {
                h = 31 * h + Float.floatToIntBits(f);
            }
            cachedHashCode = h;
        }

        public float get(final int index) {
            return quantities[index];
        }

        public int hashCode() {
            return cachedHashCode;
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < quantities.length; i++) {
                if (quantities[i] == 0.0f) {
                    continue;
                }
                sb.append(String.format("%.2f %s;", quantities[i], Food.values()[i].name));
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
        final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());

        final Supplier<GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<Solution>> state =
                () -> GeneticAlgorithmConfig.<Solution>builder()
                        .populationSize(1_000)
                        .maxGenerations(100)
                        .survivalRate(0.1)
                        .crossoverRate(0.5)
                        .mutationRate(0.3)
                        .creation(() -> {
                            final float[] v = new float[Food.values().length];
                            for (int i = 0; i < v.length; i++) {
                                v[i] = rng.nextFloat(0.0f, 10.0f);
                            }
                            return new Solution(v);
                        })
                        .crossover((a, b) -> {
                            final float[] v = new float[Food.values().length];
                            for (int i = 0; i < v.length; i++) {
                                v[i] = rng.nextFloat() < 0.5f ? a.get(i) : b.get(i);
                            }
                            return new Solution(v);
                        })
                        .mutation(x -> {
                            final float[] v = new float[Food.values().length];
                            for (int i = 0; i < v.length; i++) {
                                v[i] = x.get(i);
                            }
                            final int idx = rng.nextInt(0, Food.values().length);
                            v[idx] = rng.nextFloat(Math.max(0.0f, v[idx] - 1.0f), v[idx] + 1.0f);
                            return new Solution(v);
                        })
                        .minimize(x -> {
                            final float minCalories = 2000.0f;
                            final float maxCalories = 3000.0f;
                            final float minCarbsPercentage = 0.45f;
                            final float maxCarbsPercentage = 0.55f;
                            final float minProteinsPercentage = 0.1f;
                            final float maxProteinsPercentage = 0.35f;
                            final float minFatsPercentage = 0.2f;
                            final float maxFatsPercentage = 0.35f;

                            float score = 0.0f;
                            float calories = 0.0f;
                            float carbs = 0.0f;
                            float proteins = 0.0f;
                            float fats = 0.0f;
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

                            final float minCarbs = calories * minCarbsPercentage;
                            final float maxCarbs = calories * maxCarbsPercentage;
                            if (carbs < minCarbs) {
                                score += (minCarbs - carbs) * (minCarbs - carbs);
                            } else if (carbs > maxCarbs) {
                                score += (carbs - maxCarbs) * (carbs - maxCarbs);
                            }

                            final float minProteins = calories * minProteinsPercentage;
                            final float maxProteins = calories * maxProteinsPercentage;
                            if (proteins < minProteins) {
                                score += (minProteins - proteins) * (minProteins - proteins);
                            } else if (proteins > maxProteins) {
                                score += (proteins - maxProteins) * (proteins - maxProteins);
                            }

                            final float minFats = calories * minFatsPercentage;
                            final float maxFats = calories * maxFatsPercentage;
                            if (fats < minFats) {
                                score += (minFats - fats) * (minFats - fats);
                            } else if (fats > maxFats) {
                                score += (fats - maxFats) * (fats - maxFats);
                            }

                            return (double) score;
                        });

        final ExecutorService ex =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final GeneticAlgorithm<Solution> ga = new ParallelGeneticAlgorithm<>(); // ex, rng);
        Set<Solution> g = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            System.out.printf("Run n.%,d\n", i);
            ga.setState(state.get().firstGeneration(g).build());
            ga.run();

            final Map<Solution, Double> scores = ga.getState().scores();
            scores.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(10)
                    .forEach(e -> System.out.printf("%s -> %f\n", e.getKey(), e.getValue()));
            g = scores.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            System.out.println();
        }

        while (!ex.isShutdown()) {
            ex.shutdown();
            try {
                ex.awaitTermination(1, TimeUnit.SECONDS);
            } catch (final InterruptedException ignored) {
            }
        }
    }
}
