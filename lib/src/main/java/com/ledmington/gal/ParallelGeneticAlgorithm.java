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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class ParallelGeneticAlgorithm<X> extends SerialGeneticAlgorithm<X> {

    private final ExecutorService executor;
    private List<Future<?>> tasks;
    private AtomicInteger nextGenerationSize;

    public ParallelGeneticAlgorithm() {
        this(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public ParallelGeneticAlgorithm(int nThreads) {
        this(
                Executors.newFixedThreadPool(nThreads),
                RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    public ParallelGeneticAlgorithm(final ExecutorService executor, final RandomGenerator rng) {
        super(rng);
        this.executor = Objects.requireNonNull(executor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (executor.isTerminated()) {
                return;
            }
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
            try {
                boolean terminated = executor.isTerminated();
                while (!terminated) {
                    terminated = executor.awaitTermination(1, TimeUnit.HOURS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void waitAll(final List<Future<?>> tasks) {
        tasks.forEach(x -> {
            try {
                x.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        tasks.clear();
    }

    protected void resetState(final GeneticAlgorithmConfig<X> config) {
        population = new ArrayList<>(config.populationSize());
        nextGeneration = new ArrayList<>(config.populationSize());
        cachedScores = new ConcurrentHashMap<>(config.populationSize());
        tasks = new ArrayList<>(config.populationSize());
        survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());

        // filling population with nulls
        for (int i = 0; i < config.populationSize(); i++) {
            population.add(null);
            nextGeneration.add(null);
        }
    }

    protected void initialCreation(final GeneticAlgorithmConfig<X> config) {
        for (int i = 0; i < config.populationSize(); i++) {
            final int finalI = i;
            tasks.add(executor.submit(
                    () -> population.set(finalI, config.creation().get())));
        }
        waitAll(tasks);
    }

    protected void computeScores(final GeneticAlgorithmConfig<X> config) {
        for (final X x : population) {
            if (!cachedScores.containsKey(x)) {
                tasks.add(executor.submit(
                        () -> cachedScores.put(x, config.fitnessFunction().apply(x))));
            }
        }

        waitAll(tasks);
    }

    protected void elitism() {
        final List<X> best = cachedScores.entrySet().stream()
                .parallel()
                .sorted(Map.Entry.comparingByValue())
                .limit(survivingPopulation)
                .map(Map.Entry::getKey)
                .toList();
        for (int i = 0; i < best.size(); i++) {
            nextGeneration.set(i, best.get(i));
        }
    }

    protected int performCrossovers(final GeneticAlgorithmConfig<X> config) {
        int crossovers = 0;
        nextGenerationSize = new AtomicInteger(survivingPopulation);

        for (int i = 0; nextGenerationSize.get() < config.populationSize() && i < config.populationSize(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
                tasks.add(executor.submit(() -> {
                    // choose randomly two parents and perform a crossover
                    final X firstParent = Utils.weightedChoose(population, cachedScores::get, rng);
                    X secondParent;
                    do {
                        secondParent = Utils.weightedChoose(population, cachedScores::get, rng);
                    } while (firstParent.equals(secondParent));
                    nextGeneration.set(
                            nextGenerationSize.getAndIncrement(),
                            config.crossoverOperator().apply(firstParent, secondParent));
                }));
                crossovers++;
            }
        }

        waitAll(tasks);

        return crossovers;
    }

    protected int performMutations(final GeneticAlgorithmConfig<X> config) {
        int mutations = 0;

        for (int i = 0; i < nextGenerationSize.get(); i++) {
            if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
                final int finalI = i;
                tasks.add(executor.submit(() ->
                        nextGeneration.set(finalI, config.mutationOperator().apply(nextGeneration.get(finalI)))));
                mutations++;
            }
        }

        waitAll(tasks);

        return mutations;
    }

    protected void addRandomCreations(final GeneticAlgorithmConfig<X> config, int randomCreations) {
        for (int i = 0; i < randomCreations; i++) {
            tasks.add(executor.submit(() -> nextGeneration.set(
                    nextGenerationSize.getAndIncrement(), config.creation().get())));
        }

        waitAll(tasks);
    }

    protected void endGeneration() {
        Collections.fill(nextGeneration, null);
    }

    /*@Override
    public void run(final GeneticAlgorithmConfig<X> config) {
        resetState(config.populationSize());
        final int survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());

        // filling population with nulls
        for (int i = 0; i < config.populationSize(); i++) {
            population.add(null);
            nextGeneration.add(null);
        }

        for (int currentGeneration = 0; currentGeneration < config.maxGenerations(); currentGeneration++) {
            if (config.verbose()) {
                System.out.printf("Generation: %,d\n", currentGeneration);
            }

            // computing scores
            for (final X x : population) {
                if (!cachedScores.containsKey(x)) {
                    tasks.add(executor.submit(
                            () -> cachedScores.put(x, config.fitnessFunction().apply(x))));
                }
            }

            waitAll(tasks);

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
                nextGeneration.set(i, population.get(i));
            }

            int crossovers = 0;
            final AtomicInteger nextGenerationSize = new AtomicInteger(survivingPopulation);

            // performing crossovers
            for (int i = 0; nextGenerationSize.get() < config.populationSize() && i < config.populationSize(); i++) {
                if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
                    tasks.add(executor.submit(() -> {
                        // choose randomly two parents and perform a crossover
                        final X firstParent = Utils.weightedChoose(population, cachedScores::get, rng);
                        X secondParent;
                        do {
                            secondParent = Utils.weightedChoose(population, cachedScores::get, rng);
                        } while (firstParent.equals(secondParent));
                        nextGeneration.set(
                                nextGenerationSize.getAndIncrement(),
                                config.crossoverOperator().apply(firstParent, secondParent));
                    }));
                    crossovers++;
                }
            }

            waitAll(tasks);

            int mutations = 0;

            // performing mutations
            for (int i = 0; i < nextGenerationSize.get(); i++) {
                if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
                    final int finalI = i;
                    tasks.add(executor.submit(() ->
                            nextGeneration.set(finalI, config.mutationOperator().apply(nextGeneration.get(finalI)))));
                    mutations++;
                }
            }

            waitAll(tasks);

            int randomCreations = config.populationSize() - nextGenerationSize.get();

            // adding random creations
            for (int i = 0; i < randomCreations; i++) {
                tasks.add(executor.submit(() -> nextGeneration.set(
                        nextGenerationSize.getAndIncrement(), config.creation().get())));
            }

            waitAll(tasks);

            if (config.verbose()) {
                System.out.printf("Crossovers performed : %,d\n", crossovers);
                System.out.printf("Mutations applied : %,d\n", mutations);
                System.out.printf("Random creations : %,d\n", randomCreations);
                System.out.println();
            }

            if (population.size() != config.populationSize() || nextGenerationSize.get() != config.populationSize()) {
                throw new IllegalStateException(String.format(
                        "The population and the next generation don't have the right size: they were %,d and %,d but should have been %,d",
                        population.size(), nextGenerationSize.get(), config.populationSize()));
            }

            // swap population and nextGeneration
            final List<X> tmp = population;
            population = nextGeneration;
            nextGeneration = tmp;

        }
    }*/
}
