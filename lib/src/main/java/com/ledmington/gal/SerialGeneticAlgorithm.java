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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class SerialGeneticAlgorithm<X> implements GeneticAlgorithm<X> {

    protected final RandomGenerator rng;
    protected List<X> population = null;
    protected List<X> nextGeneration = null;
    protected Map<X, Double> cachedScores = null;
    protected int survivingPopulation = -1;

    public SerialGeneticAlgorithm() {
        this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public SerialGeneticAlgorithm(final RandomGenerator rng) {
        this.rng = Objects.requireNonNull(rng);
    }

    protected void resetState(final GeneticAlgorithmConfig<X> config) {
        population = new ArrayList<>(config.populationSize());
        nextGeneration = new ArrayList<>(config.populationSize());
        cachedScores = new HashMap<>(config.populationSize());
        survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());
    }

    protected void initialCreation(final GeneticAlgorithmConfig<X> config) {
        for (int i = 0; i < config.populationSize(); i++) {
            population.add(i, config.creation().get());
        }
    }

    protected void computeScores(final GeneticAlgorithmConfig<X> config) {
        for (final X x : population) {
            if (!cachedScores.containsKey(x)) {
                cachedScores.put(x, config.fitnessFunction().apply(x));
            }
        }
    }

    protected void elitism(final GeneticAlgorithmConfig<X> config) {
        cachedScores.keySet().stream()
                .sorted((a, b) -> config.scoreComparator().compare(cachedScores.get(a), cachedScores.get(b)))
                .limit(survivingPopulation)
                .forEach(x -> nextGeneration.add(x));
    }

    protected int performCrossovers(final GeneticAlgorithmConfig<X> config) {
        int crossovers = 0;

        for (int i = 0; nextGeneration.size() < config.populationSize() && i < config.populationSize(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
                // choose randomly two parents and perform a crossover
                final X firstParent = Utils.weightedChoose(population, cachedScores::get, rng);
                X secondParent;
                do {
                    secondParent = Utils.weightedChoose(population, cachedScores::get, rng);
                } while (firstParent.equals(secondParent));
                nextGeneration.add(config.crossoverOperator().apply(firstParent, secondParent));
                crossovers++;
            }
        }

        return crossovers;
    }

    protected int performMutations(final GeneticAlgorithmConfig<X> config) {
        int mutations = 0;

        for (int i = 0; i < nextGeneration.size(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
                nextGeneration.set(i, config.mutationOperator().apply(nextGeneration.get(i)));
                mutations++;
            }
        }

        return mutations;
    }

    protected void addRandomCreations(final GeneticAlgorithmConfig<X> config, int randomCreations) {
        for (int i = 0; i < randomCreations; i++) {
            nextGeneration.add(config.creation().get());
        }
    }

    protected void endGeneration() {
        nextGeneration.clear();
    }

    public void run(final GeneticAlgorithmConfig<X> config) {
        resetState(config);

        initialCreation(config);

        for (int currentGeneration = 0; currentGeneration < config.maxGenerations(); currentGeneration++) {
            if (config.verbose()) {
                System.out.printf("Generation: %,d\n", currentGeneration);
            }

            computeScores(config);

            if (config.verbose()) {
                final List<X> best = cachedScores.keySet().stream()
                        .sorted((a, b) -> config.scoreComparator().compare(cachedScores.get(a), cachedScores.get(b)))
                        .limit(config.printBest())
                        .toList();

                for (int i = 0; i < best.size(); i++) {
                    System.out.printf(
                            "N. %d: '%s' (score: %.3f)\n",
                            i + 1, config.serializer().apply(best.get(i)), cachedScores.get(best.get(i)));
                }
            }

            elitism(config);

            int crossovers = performCrossovers(config);

            int mutations = performMutations(config);

            int randomCreations = config.populationSize() - survivingPopulation - crossovers;

            addRandomCreations(config, randomCreations);

            if (config.verbose()) {
                System.out.printf("Crossovers performed : %,d\n", crossovers);
                System.out.printf("Mutations applied : %,d\n", mutations);
                System.out.printf("Random creations : %,d\n", randomCreations);
                System.out.println();
            }

            if (population.size() != config.populationSize() || nextGeneration.size() != config.populationSize()) {
                throw new IllegalStateException(String.format(
                        "The population and the next generation don't have the right size: they were %,d and %,d but should have been %,d",
                        population.size(), nextGeneration.size(), config.populationSize()));
            }

            // swap population and nextGeneration
            final List<X> tmp = population;
            population = nextGeneration;
            nextGeneration = tmp;

            endGeneration();
        }
    }
}
