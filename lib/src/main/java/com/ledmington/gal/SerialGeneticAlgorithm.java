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

public final class SerialGeneticAlgorithm<X> implements GeneticAlgorithm<X> {

    private final RandomGenerator rng;

    public SerialGeneticAlgorithm() {
        this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public SerialGeneticAlgorithm(final RandomGenerator rng) {
        this.rng = Objects.requireNonNull(rng);
    }

    public void run(final GeneticAlgorithmConfig<X> config) {
        List<X> population = new ArrayList<>(config.populationSize());
        List<X> nextGeneration = new ArrayList<>(config.populationSize());
        final Map<X, Double> cachedScores = new HashMap<>();
        final int survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());

        // initial creation
        for (int i = 0; i < config.populationSize(); i++) {
            population.add(i, config.creation().get());
        }

        for (int currentGeneration = 0; currentGeneration < config.maxGenerations(); currentGeneration++) {
            if (config.verbose()) {
                System.out.printf("Generation: %,d\n", currentGeneration);
            }

            // computing scores
            for (final X x : population) {
                if (!cachedScores.containsKey(x)) {
                    cachedScores.put(x, config.fitnessFunction().apply(x));
                }
            }

            population.sort((a, b) -> config.scoreComparator().compare(cachedScores.get(a), cachedScores.get(b)));

            if (config.verbose()) {
                for (int i = 0; i < config.printBest(); i++) {
                    System.out.printf(
                            "N. %d: '%s' (score: %.3f)\n",
                            i + 1, config.serializer().apply(population.get(i)), cachedScores.get(population.get(i)));
                }
            }

            // The top X% gets copied directly into the new generation
            for (int i = 0; i < survivingPopulation; i++) {
                nextGeneration.add(population.get(i));
            }

            int crossovers = 0;

            // performing crossovers
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

            int mutations = 0;

            // performing mutations
            for (int i = 0; i < nextGeneration.size(); i++) {
                if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
                    nextGeneration.set(i, config.mutationOperator().apply(nextGeneration.get(i)));
                    mutations++;
                }
            }

            int randomCreations = 0;

            // adding random creations
            while (nextGeneration.size() < config.populationSize()) {
                nextGeneration.add(config.creation().get());
                randomCreations++;
            }

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
            nextGeneration.clear();
        }
    }
}
