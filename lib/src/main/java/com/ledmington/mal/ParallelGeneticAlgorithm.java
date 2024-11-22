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
package com.ledmington.mal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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
import java.util.function.Supplier;
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

	public void setState(final GeneticAlgorithmConfig<X> config) {
		this.config = config;
		population = new ArrayList<>(config.populationSize());
		nextGeneration = new ArrayList<>(config.populationSize());
		cachedScores = new ConcurrentHashMap<>(config.populationSize(), 1.0f);
		survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());
		bestOfAllTime = new LinkedHashSet<>(survivingPopulation * 2, 1.0f);
		cachedScoresComparator = (a, b) -> config.scoreComparator().compare(cachedScores.get(a), cachedScores.get(b));
		tasks = new ArrayList<>(config.populationSize());

		// filling population with nulls
		for (int i = 0; i < config.populationSize(); i++) {
			population.add(null);
			nextGeneration.add(null);
		}
	}

	protected void initialCreation() {
		int i = 0;
		for (final X obj : config.firstGeneration()) {
			population.set(i, obj);
			i++;
		}
		for (; i < config.populationSize(); i++) {
			final int finalI = i;
			tasks.add(executor.submit(
					() -> population.set(finalI, config.creation().get())));
		}
		waitAll(tasks);
	}

	protected void computeScores() {
		for (final X x : population) {
			if (!cachedScores.containsKey(x)) {
				tasks.add(executor.submit(
						() -> cachedScores.put(x, config.fitnessFunction().apply(x))));
			}
		}

		waitAll(tasks);
	}

	protected void elitism() {
		if (bestOfAllTime.isEmpty()) {
			// the first time compute the last N best solutions from the global
			// Map of scores
			final List<X> tmp = cachedScores.entrySet().stream()
					.sorted((a, b) -> config.scoreComparator().compare(a.getValue(), b.getValue()))
					.limit(survivingPopulation)
					.map(Map.Entry::getKey)
					.toList();
			for (int i = 0; i < tmp.size(); i++) {
				bestOfAllTime.add(tmp.get(i));
				nextGeneration.set(i, tmp.get(i));
			}
		} else {
			// all the other times, we compute the best N solutions by combining lastBest
			// and the best N from the current generation
			population.stream()
					.distinct()
					.sorted(cachedScoresComparator)
					.limit(survivingPopulation)
					.forEach(x -> bestOfAllTime.add(x));

			final List<X> tmp = bestOfAllTime.stream()
					.sorted(cachedScoresComparator)
					.limit(survivingPopulation)
					.toList();
			for (int i = 0; i < tmp.size(); i++) {
				nextGeneration.set(i, tmp.get(i));
			}

			bestOfAllTime.stream()
					.sorted(cachedScoresComparator)
					.toList()
					.subList(survivingPopulation, bestOfAllTime.size())
					.forEach(x -> bestOfAllTime.remove(x));
		}
	}

	protected int performCrossovers() {
		int crossovers = 0;
		nextGenerationSize = new AtomicInteger(survivingPopulation);
		final Supplier<X> weightedRandom = Utils.weightedChoose(population, x -> cachedScores.get(x), rng);

		for (int i = 0; nextGenerationSize.get() < config.populationSize() && i < config.populationSize(); i++) {
			if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
				tasks.add(executor.submit(() -> {
					// choose randomly two parents and perform a crossover
					final X firstParent = weightedRandom.get();
					X secondParent;
					do {
						secondParent = weightedRandom.get();
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

	protected int performMutations() {
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

	protected void addRandomCreations(int randomCreations) {
		for (int i = 0; i < randomCreations; i++) {
			tasks.add(executor.submit(() -> nextGeneration.set(
					nextGenerationSize.getAndIncrement(), config.creation().get())));
		}

		waitAll(tasks);
	}

	protected void endGeneration() {
		Collections.fill(nextGeneration, null);
	}
}
