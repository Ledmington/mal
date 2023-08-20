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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

public final class SerialGeneticAlgorithm<X> implements GeneticAlgorithm<X> {

    private final RandomGenerator rng;

    public SerialGeneticAlgorithm() {
        this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public SerialGeneticAlgorithm(final RandomGenerator rng) {
        this.rng = Objects.requireNonNull(rng);
    }

    public void run(final GeneticAlgorithmConfig<X> config) {
        final List<X> population = new ArrayList<>(config.populationSize());
        final Map<X, Double> cachedScores = new HashMap<>();
        final int survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());
        final int mutatingPopulation = (int) ((double) config.populationSize() * config.mutationRate());

        // initial creation
        for (int i = 0; i < config.populationSize(); i++) {
            population.add(i, config.creation().get());
        }

        for (int currentGeneration = 0; currentGeneration < config.maxGenerations(); currentGeneration++) {
            System.out.printf("Generation: %,d\n", currentGeneration);
            for (final X x : population) {
                if (!cachedScores.containsKey(x)) {
                    cachedScores.put(x, config.fitnessFunction().apply(x));
                }
            }

            population.sort(Comparator.comparing(cachedScores::get).reversed());

            System.out.printf(
                    "Best: '%s' (score: %.3f)\n",
                    config.serializer().apply(population.get(0)), cachedScores.get(population.get(0)));

            // elitism: we keep only the top X% of the population (by ignoring it)
            int mutations = 0;
            int randomCreations = 0;
            int i;

            // performing mutations
            for (i = survivingPopulation; i < survivingPopulation + mutatingPopulation; i++) {
                population.set(
                        i,
                        config.mutationOperator()
                                .apply(Utils.weightedChoose(
                                        IntStream.range(0, survivingPopulation)
                                                .mapToObj(population::get)
                                                .toList(),
                                        cachedScores::get,
                                        rng)));
                mutations++;
            }

            // adding random creations
            for (; i < config.populationSize(); i++) {
                population.set(i, config.creation().get());
                randomCreations++;
            }

            System.out.printf("Mutations applied : %,d\n", mutations);
            System.out.printf("Random creations : %,d\n", randomCreations);
            System.out.println();
        }
    }
}
