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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record GeneticAlgorithmConfig<X>(
        int populationSize,
        double survivalRate,
        double crossoverRate,
        double mutationRate,
        Predicate<GeneticAlgorithmState<X>> termination,
        Supplier<X> creation,
        BiFunction<X, X, X> crossoverOperator,
        Function<X, X> mutationOperator,
        Function<X, Double> fitnessFunction,
        Comparator<Double> scoreComparator,
        Function<X, String> serializer,
        Set<X> firstGeneration,
        Consumer<GeneticAlgorithmState<X>> printer) {

    public static <T> GeneticAlgorithmConfigBuilder<T> builder() {
        return new GeneticAlgorithmConfigBuilder<>();
    }

    private static int assertPopulationSizeIsValid(int pop) {
        if (pop < 2) {
            throw new IllegalArgumentException(
                    String.format("Invalid population size: needs to be >= 2 but was %d", pop));
        }
        return pop;
    }

    private static int assertMaxGenerationsIsValid(int generations) {
        if (generations < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid max generations: needs to be >= 0 but was %d", generations));
        }
        return generations;
    }

    private static int assertMaxSecondsIsValid(int maxSeconds) {
        if (maxSeconds < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid max seconds: needs to be >= 0 but was %d", maxSeconds));
        }
        return maxSeconds;
    }

    private static double assertSurvivalRateIsValid(double rate) {
        if (rate <= 0.0 || rate >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid survival rate: needs to be > 0 and < 1 but was %f", rate));
        }
        return rate;
    }

    private static double assertCrossoverRateIsValid(double rate) {
        if (rate <= 0.0 || rate >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid crossover rate: needs to be > 0 and < 1 but was %f", rate));
        }
        return rate;
    }

    private static double assertMutationRateIsValid(double rate) {
        if (rate <= 0.0 || rate >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid mutation rate: needs to be > 0 and < 1 but was %f", rate));
        }
        return rate;
    }

    private static int assertPrintBestIsValid(int nBestToPrint) {
        if (nBestToPrint < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid nBestToPrint: must be > 0 but was %,d\n", nBestToPrint));
        }
        return nBestToPrint;
    }

    public static final class GeneticAlgorithmConfigBuilder<X> {

        private boolean alreadyBuilt = false;
        private int populationSize = 100;
        private double survivalRate = 0.1;
        private double crossoverRate = 0.7;
        private double mutationRate = 0.1;
        private Predicate<GeneticAlgorithmState<X>> maxGenerations = null;
        private Predicate<GeneticAlgorithmState<X>> stopCriterion = null;
        private Predicate<GeneticAlgorithmState<X>> maxTime = null;
        private Supplier<X> randomCreation = null;
        private BiFunction<X, X, X> crossoverOperator = null;
        private Function<X, X> mutationOperator = null;
        private Function<X, Double> fitnessFunction = null;
        private Comparator<Double> scoreComparator = null;
        private Function<X, String> serializer = Object::toString;
        private final Set<X> firstGeneration = new HashSet<>();
        private boolean verbose = false;
        private int nBestToPrint = 1;
        private int nWorstToPrint = 0;
        private boolean printMedian = false;
        private boolean printAverageScore = false;

        public GeneticAlgorithmConfigBuilder<X> populationSize(int pop) {
            assertPopulationSizeIsValid(pop);
            populationSize = pop;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> survivalRate(double rate) {
            assertSurvivalRateIsValid(rate);
            survivalRate = rate;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> crossoverRate(double rate) {
            assertCrossoverRateIsValid(rate);
            crossoverRate = rate;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> mutationRate(double rate) {
            assertMutationRateIsValid(rate);
            mutationRate = rate;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> maxGenerations(int generations) {
            assertMaxGenerationsIsValid(generations);
            maxGenerations = state -> state.currentGeneration() >= generations;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> stopCriterion(final Predicate<X> criterion) {
            Objects.requireNonNull(criterion, "The stopping criterion cannot be null");
            stopCriterion = state -> state.population().stream().anyMatch(criterion);
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> maxSeconds(int maxSeconds) {
            final long maxMillis = assertMaxSecondsIsValid(maxSeconds) * 1_000L;
            maxTime = state -> (System.currentTimeMillis() - state.startTime()) >= maxMillis;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> creation(final Supplier<X> creation) {
            Objects.requireNonNull(creation, "The creation function cannot be null");
            randomCreation = creation;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> crossover(final BiFunction<X, X, X> crossover) {
            Objects.requireNonNull(crossover, "The crossover operator cannot be null");
            crossoverOperator = crossover;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> mutation(final Function<X, X> mutation) {
            Objects.requireNonNull(mutation, "The mutation operator cannot be null");
            mutationOperator = mutation;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> maximize(final Function<X, Double> fitness) {
            Objects.requireNonNull(fitness, "The fitness function cannot be null");
            fitnessFunction = fitness;
            scoreComparator = (a, b) -> -Double.compare(a, b);
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> minimize(final Function<X, Double> fitness) {
            Objects.requireNonNull(fitness, "The fitness function cannot be null");
            fitnessFunction = fitness;
            scoreComparator = Double::compare;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> serializer(final Function<X, String> serializer) {
            Objects.requireNonNull(serializer, "The serializer function cannot be null");
            this.serializer = serializer;
            return this;
        }

        @SafeVarargs
        public final GeneticAlgorithmConfigBuilder<X> firstGeneration(final X... objects) {
            for (final X obj : objects) {
                this.firstGeneration.add(Objects.requireNonNull(obj));
            }
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> verbose() {
            verbose = true;
            nBestToPrint = 5;
            nWorstToPrint = 5;
            printMedian = true;
            printAverageScore = true;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> quiet() {
            verbose = false;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> printBest(final int nBestToPrint) {
            verbose = true;
            this.nBestToPrint = assertPrintBestIsValid(nBestToPrint);
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> printWorst(final int nWorstToPrint) {
            verbose = true;
            this.nWorstToPrint = assertPrintBestIsValid(nWorstToPrint);
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> printMedian() {
            verbose = true;
            this.printMedian = true;
            return this;
        }

        public GeneticAlgorithmConfigBuilder<X> printAverageScore() {
            verbose = true;
            this.printAverageScore = true;
            return this;
        }

        public GeneticAlgorithmConfig<X> build() {
            if (alreadyBuilt) {
                throw new IllegalStateException("Cannot build the same GeneticAlgorithmConfigBuilder two times");
            }

            final List<Predicate<GeneticAlgorithmState<X>>> terminationCriteria = Stream.of(
                            maxGenerations, stopCriterion, maxTime)
                    .filter(Objects::nonNull)
                    .toList();
            if (terminationCriteria.isEmpty()) {
                throw new IllegalArgumentException("No termination criterion was defined");
            }
            final Predicate<GeneticAlgorithmState<X>> termination =
                    state -> terminationCriteria.stream().anyMatch(p -> p.test(state));

            Consumer<GeneticAlgorithmState<X>> printer;
            if (!verbose) {
                printer = s -> {};
            } else {
                printer = state -> {
                    System.out.printf("Generation: %,d\n", state.currentGeneration());
                    final List<SimpleImmutableEntry<X, Double>> bestPopulation = state.bestOfAllTime().stream()
                            .map(x ->
                                    new SimpleImmutableEntry<>(x, state.scores().get(x)))
                            .sorted((a, b) -> scoreComparator.compare(a.getValue(), b.getValue()))
                            .toList();

                    for (int i = 0; i < nBestToPrint; i++) {
                        System.out.printf(
                                "N. %,4d: '%s' (score: %.6f)\n",
                                i + 1,
                                serializer.apply(bestPopulation.get(i).getKey()),
                                bestPopulation.get(i).getValue());
                    }
                    if (printMedian) {
                        int medianIndex = state.survivingPopulation() / 2;
                        System.out.printf(
                                "N. %,4d: '%s' (score: %.6f)\n",
                                medianIndex,
                                serializer.apply(bestPopulation.get(medianIndex).getKey()),
                                bestPopulation.get(medianIndex).getValue());
                    }
                    for (int i = state.survivingPopulation() - nWorstToPrint; i < state.survivingPopulation(); i++) {
                        System.out.printf(
                                "N. %,4d: '%s' (score: %.6f)\n",
                                i + 1,
                                serializer.apply(bestPopulation.get(i).getKey()),
                                bestPopulation.get(i).getValue());
                    }
                    if (printAverageScore) {
                        System.out.printf(
                                "Average score: %.6f\n",
                                bestPopulation.stream()
                                        .mapToDouble(SimpleImmutableEntry::getValue)
                                        .average()
                                        .orElseThrow());
                    }
                    System.out.println();
                };
            }

            this.alreadyBuilt = true;
            return new GeneticAlgorithmConfig<>(
                    populationSize,
                    survivalRate,
                    crossoverRate,
                    mutationRate,
                    termination,
                    randomCreation,
                    crossoverOperator,
                    mutationOperator,
                    fitnessFunction,
                    scoreComparator,
                    serializer,
                    firstGeneration,
                    printer);
        }
    }

    public GeneticAlgorithmConfig(
            int populationSize,
            double survivalRate,
            double crossoverRate,
            double mutationRate,
            Predicate<GeneticAlgorithmState<X>> termination,
            Supplier<X> creation,
            BiFunction<X, X, X> crossoverOperator,
            Function<X, X> mutationOperator,
            Function<X, Double> fitnessFunction,
            Comparator<Double> scoreComparator,
            Function<X, String> serializer,
            Set<X> firstGeneration,
            Consumer<GeneticAlgorithmState<X>> printer) {
        this.populationSize = assertPopulationSizeIsValid(populationSize);
        this.survivalRate = assertSurvivalRateIsValid(survivalRate);
        this.crossoverRate = assertCrossoverRateIsValid(crossoverRate);
        this.mutationRate = assertMutationRateIsValid(mutationRate);
        this.termination = Objects.requireNonNull(termination, "The termination criterion cannot be null");
        this.creation = Objects.requireNonNull(creation, "The creation function cannot be null");
        this.crossoverOperator = Objects.requireNonNull(crossoverOperator, "The crossover operator cannot be null");
        this.mutationOperator = Objects.requireNonNull(mutationOperator, "The mutation operator cannot be null");
        this.fitnessFunction = Objects.requireNonNull(fitnessFunction, "The fitness function cannot be null");
        this.scoreComparator = Objects.requireNonNull(scoreComparator, "The score comparator cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "The serializer function cannot be null");
        this.firstGeneration = Objects.requireNonNull(firstGeneration, "The first generation cannot be null");
        this.printer = Objects.requireNonNull(printer, "The printer cannot be null");

        if (firstGeneration.size() > populationSize) {
            throw new IllegalArgumentException(String.format(
                    "Invalid first generation size: should have been <= %,d but was %,d",
                    populationSize, firstGeneration.size()));
        }
    }
}
