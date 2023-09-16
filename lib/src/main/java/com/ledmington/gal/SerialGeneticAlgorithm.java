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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class SerialGeneticAlgorithm<X> implements GeneticAlgorithm<X> {

    protected final RandomGenerator rng;
    protected GeneticAlgorithmState<X> state;

    public SerialGeneticAlgorithm() {
        this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public SerialGeneticAlgorithm(final RandomGenerator rng) {
        this.rng = Objects.requireNonNull(rng);
    }

    public GeneticAlgorithmState<X> getState() {
        return state;
    }

    protected void resetState(final GeneticAlgorithmConfig<X> config) {
        state = new GeneticAlgorithmState<>(
                new ArrayList<>(config.populationSize()),
                new ArrayList<>(config.populationSize()),
                new HashMap<>(config.populationSize(), 1.0f),
                (int) ((double) config.populationSize() * config.survivalRate()));
    }

    protected void initialCreation(final GeneticAlgorithmConfig<X> config) {
        state.population().addAll(config.firstGeneration());
        while (state.population().size() < config.populationSize()) {
            state.population().add(config.creation().get());
        }
    }

    protected void computeScores(final GeneticAlgorithmConfig<X> config) {
        for (final X x : state.population()) {
            if (!state.scores().containsKey(x)) {
                state.scores().put(x, config.fitnessFunction().apply(x));
            }
        }
    }

    protected void elitism(final GeneticAlgorithmConfig<X> config) {
        if (state.bestOfAllTime().isEmpty()) {
            // the first time compute the last N best solutions from the global
            // Map of scores
            state.scores().entrySet().stream()
                    .sorted((a, b) -> config.scoreComparator().compare(a.getValue(), b.getValue()))
                    .limit(state.survivingPopulation())
                    .map(Map.Entry::getKey)
                    .forEach(x -> {
                        state.bestOfAllTime().add(x);
                        state.nextGeneration().add(x);
                    });
        } else {
            // all the other times, we compute the best N solutions by combining lastBest
            // and the best N from the current generation
            state.population().stream()
                    .distinct()
                    .sorted((a, b) -> config.scoreComparator()
                            .compare(state.scores().get(a), state.scores().get(b)))
                    .limit(state.survivingPopulation())
                    .forEach(x -> state.bestOfAllTime().add(x));
            if (state.bestOfAllTime().size() < state.survivingPopulation()) {
                throw new AssertionError(String.format(
                        "Wrong size: was %,d but should have been at least %,d",
                        state.bestOfAllTime().size(), state.survivingPopulation()));
            }

            state.bestOfAllTime().stream()
                    .sorted((a, b) -> config.scoreComparator()
                            .compare(state.scores().get(a), state.scores().get(b)))
                    .limit(state.survivingPopulation())
                    .forEach(x -> state.nextGeneration().add(x));

            if (state.bestOfAllTime().size() < state.survivingPopulation()) {
                throw new AssertionError(String.format(
                        "Wrong size2: was %,d but should have been at least %,d",
                        state.bestOfAllTime().size(), state.survivingPopulation()));
            }

            state.bestOfAllTime().stream()
                    .sorted((a, b) -> config.scoreComparator()
                            .compare(state.scores().get(a), state.scores().get(b)))
                    .toList()
                    .subList(state.survivingPopulation(), state.bestOfAllTime().size())
                    .forEach(x -> state.bestOfAllTime().remove(x));

            if (state.bestOfAllTime().size() < state.survivingPopulation()) {
                throw new AssertionError(String.format(
                        "Wrong size3: was %,d but should have been at least %,d",
                        state.bestOfAllTime().size(), state.survivingPopulation()));
            }
        }
    }

    protected int performCrossovers(final GeneticAlgorithmConfig<X> config) {
        int crossovers = 0;
        final Supplier<X> weightedRandom =
                Utils.weightedChoose(state.population(), x -> state.scores().get(x), rng);

        for (int i = 0; state.nextGeneration().size() < config.populationSize() && i < config.populationSize(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
                // choose randomly two parents and perform a crossover
                final X firstParent = weightedRandom.get();
                X secondParent;
                do {
                    secondParent = weightedRandom.get();
                } while (firstParent.equals(secondParent));
                state.nextGeneration().add(config.crossoverOperator().apply(firstParent, secondParent));
                crossovers++;
            }
        }

        return crossovers;
    }

    protected int performMutations(final GeneticAlgorithmConfig<X> config) {
        int mutations = 0;

        for (int i = 0; i < state.nextGeneration().size(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
                state.nextGeneration()
                        .set(
                                i,
                                config.mutationOperator()
                                        .apply(state.nextGeneration().get(i)));
                mutations++;
            }
        }

        return mutations;
    }

    protected void addRandomCreations(final GeneticAlgorithmConfig<X> config, int randomCreations) {
        for (int i = 0; i < randomCreations; i++) {
            state.nextGeneration().add(config.creation().get());
        }
    }

    protected void endGeneration() {
        state.nextGeneration().clear();
    }

    public void run(final GeneticAlgorithmConfig<X> config) {
        resetState(config);

        initialCreation(config);

        while (!config.termination().test(state)) {
            computeScores(config);

            elitism(config);

            final int crossovers = performCrossovers(config);

            final int mutations = performMutations(config);

            final int randomCreations = config.populationSize() - state.survivingPopulation() - crossovers;

            addRandomCreations(config, randomCreations);

            config.printer().accept(state);

            if (state.population().size() != config.populationSize()
                    || state.nextGeneration().size() != config.populationSize()) {
                throw new IllegalStateException(String.format(
                        "The population and the next generation don't have the right size: they were %,d and %,d but should have been %,d",
                        state.population().size(), state.nextGeneration().size(), config.populationSize()));
            }

            // swap population and nextGeneration
            state.swapPopulations();

            endGeneration();

            state.incrementGeneration();
        }

        state.bestOfAllTime().clear();
    }
}
