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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class SerialGeneticAlgorithm<X> implements GeneticAlgorithm<X> {

	protected final RandomGenerator rng;
	protected GeneticAlgorithmConfig<X> config = null;
	protected long startTime;
	private int generation = 0;
	protected List<X> population;
	protected List<X> nextGeneration;
	protected Map<X, Double> cachedScores;
	protected int survivingPopulation;

	// stats
	private int mutations = 0;
	private int crossovers = 0;
	private int randomCreations = 0;

	protected Set<X> bestOfAllTime; // For optimization. Should not be returned.
	protected Comparator<X> cachedScoresComparator = null;

	public SerialGeneticAlgorithm() {
		this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
	}

	public SerialGeneticAlgorithm(final RandomGenerator rng) {
		this.rng = Objects.requireNonNull(rng);
	}

	public GeneticAlgorithmState<X> getState() {
		// return a new State with immutable view of the data
		return new GeneticAlgorithmState<>(
				generation,
				Collections.unmodifiableList(population),
				Collections.unmodifiableMap(cachedScores),
				mutations,
				crossovers,
				randomCreations);
	}

	public void setState(final GeneticAlgorithmConfig<X> config) {
		this.config = config;
		population = new ArrayList<>(config.populationSize());
		nextGeneration = new ArrayList<>(config.populationSize());
		cachedScores = new HashMap<>(config.populationSize(), 1.0f);
		survivingPopulation = (int) ((double) config.populationSize() * config.survivalRate());
		bestOfAllTime = new LinkedHashSet<>(survivingPopulation * 2, 1.0f);
		startTime = System.currentTimeMillis();
		cachedScoresComparator = (a, b) -> config.scoreComparator().compare(cachedScores.get(a), cachedScores.get(b));
	}

	protected void initialCreation() {
		population.addAll(config.firstGeneration());
		while (population.size() < config.populationSize()) {
			population.add(config.creation().get());
		}
	}

	protected void computeScores() {
		for (final X x : population) {
			if (!cachedScores.containsKey(x)) {
				cachedScores.put(x, config.fitnessFunction().apply(x));
			}
		}
	}

	protected void elitism() {
		if (bestOfAllTime.isEmpty()) {
			// the first time compute the last N best solutions from the global
			// Map of scores
			cachedScores.entrySet().stream()
					.sorted((a, b) -> config.scoreComparator().compare(a.getValue(), b.getValue()))
					.limit(survivingPopulation)
					.map(Map.Entry::getKey)
					.forEach(x -> {
						bestOfAllTime.add(x);
						nextGeneration.add(x);
					});
		} else {
			// all the other times, we compute the best N solutions by combining lastBest
			// and the best N from the current generation
			population.stream()
					.distinct()
					.sorted(cachedScoresComparator)
					.limit(survivingPopulation)
					.forEach(x -> bestOfAllTime.add(x));
			if (bestOfAllTime.size() < survivingPopulation) {
				throw new AssertionError(String.format(
						"Wrong size: was %,d but should have been at least %,d",
						bestOfAllTime.size(), survivingPopulation));
			}

			bestOfAllTime.stream()
					.sorted(cachedScoresComparator)
					.limit(survivingPopulation)
					.forEach(x -> nextGeneration.add(x));

			if (bestOfAllTime.size() < survivingPopulation) {
				throw new AssertionError(String.format(
						"Wrong size2: was %,d but should have been at least %,d",
						bestOfAllTime.size(), survivingPopulation));
			}

			bestOfAllTime.stream()
					.sorted(cachedScoresComparator)
					.toList()
					.subList(survivingPopulation, bestOfAllTime.size())
					.forEach(x -> bestOfAllTime.remove(x));

			if (bestOfAllTime.size() < survivingPopulation) {
				throw new AssertionError(String.format(
						"Wrong size3: was %,d but should have been at least %,d",
						bestOfAllTime.size(), survivingPopulation));
			}
		}
	}

	protected int performCrossovers() {
		int c = 0;
		final Supplier<X> weightedRandom = Utils.weightedChoose(population, x -> cachedScores.get(x), rng);

		for (int i = 0; nextGeneration.size() < config.populationSize() && i < config.populationSize(); i++) {
			if (rng.nextDouble(0.0, 1.0) < config.crossoverRate()) {
				// choose randomly two parents and perform a crossover
				final X firstParent = weightedRandom.get();
				X secondParent;
				do {
					secondParent = weightedRandom.get();
				} while (firstParent.equals(secondParent));
				nextGeneration.add(config.crossoverOperator().apply(firstParent, secondParent));
				c++;
			}
		}

		return c;
	}

	protected int performMutations() {
		int m = 0;

		for (int i = 0; i < nextGeneration.size(); i++) {
			if (rng.nextDouble(0.0, 1.0) < config.mutationRate()) {
				nextGeneration.set(i, config.mutationOperator().apply(nextGeneration.get(i)));
				m++;
			}
		}

		return m;
	}

	protected void addRandomCreations(int randomCreations) {
		for (int i = 0; i < randomCreations; i++) {
			nextGeneration.add(config.creation().get());
		}
	}

	protected void endGeneration() {
		nextGeneration.clear();
	}

	private void swapPopulations() {
		final List<X> tmp = population;
		population = nextGeneration;
		nextGeneration = tmp;
	}

	/**
	 * Checks if the algorithm needs to terminate.
	 *
	 * @return True if at least one more generation can be done.
	 */
	private boolean checkTerminationConditions() {
		return (System.currentTimeMillis() - startTime) < config.maxTimeMillis()
				&& generation < config.maxGenerations()
				&& population.stream().noneMatch(config.stopCriterion());
	}

	public void run() {
		generation = 0;
		initialCreation();

		while (checkTerminationConditions()) {
			computeScores();

			elitism();

			crossovers = performCrossovers();

			mutations = performMutations();

			randomCreations = config.populationSize() - survivingPopulation - crossovers;

			addRandomCreations(randomCreations);

			if (population.size() != config.populationSize() || nextGeneration.size() != config.populationSize()) {
				throw new IllegalStateException(String.format(
						"The population and the next generation don't have the right size: they were %,d and %,d but should have been %,d",
						population.size(), nextGeneration.size(), config.populationSize()));
			}

			// swap population and nextGeneration
			swapPopulations();

			endGeneration();

			generation++;
		}

		bestOfAllTime.clear();
	}
}
