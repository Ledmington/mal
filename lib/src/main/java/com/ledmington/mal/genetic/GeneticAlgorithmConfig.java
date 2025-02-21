/*
* minimization-algorithms-library - A collection of minimization algorithms.
* Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.mal.genetic;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record GeneticAlgorithmConfig<X>(
		int populationSize,
		double survivalRate,
		double crossoverRate,
		double mutationRate,
		Supplier<X> creation,
		BiFunction<X, X, X> crossoverOperator,
		Function<X, X> mutationOperator,
		Function<X, Double> fitnessFunction,
		Comparator<Double> scoreComparator,
		Set<X> firstGeneration,
		int maxGenerations,
		Predicate<X> stopCriterion) {

	public static <T> GeneticAlgorithmConfigBuilder<T> builder() {
		return new GeneticAlgorithmConfigBuilder<>();
	}

	private static int assertPopulationSizeIsValid(int pop) {
		if (pop < 2) {
			throw new IllegalArgumentException(
					String.format("Invalid population size: needs to be >= 2 but was %,d", pop));
		}
		return pop;
	}

	private static int assertMaxGenerationsIsValid(int generations) {
		if (generations < 0) {
			throw new IllegalArgumentException(
					String.format("Invalid max generations: needs to be >= 0 but was %,d", generations));
		}
		return generations;
	}

	private static long assertMaxSecondsIsValid(long maxSeconds) {
		if (maxSeconds < 0) {
			throw new IllegalArgumentException(
					String.format("Invalid max seconds: needs to be >= 0 but was %,d", maxSeconds));
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

	public static final class GeneticAlgorithmConfigBuilder<X> {

		private boolean alreadyBuilt = false;
		private int populationSize = 100;
		private double survivalRate = 0.1;
		private double crossoverRate = 0.7;
		private double mutationRate = 0.1;
		private Supplier<X> randomCreation = null;
		private BiFunction<X, X, X> crossoverOperator = null;
		private Function<X, X> mutationOperator = null;
		private Function<X, Double> fitnessFunction = null;
		private Comparator<Double> scoreComparator = null;
		private final Set<X> firstGeneration = new HashSet<>();
		private int maxGenerations = Integer.MAX_VALUE;
		private Predicate<X> stopCriterion = x -> false;

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
			maxGenerations = generations;
			return this;
		}

		public GeneticAlgorithmConfigBuilder<X> stopCriterion(final Predicate<X> criterion) {
			Objects.requireNonNull(criterion, "The stopping criterion cannot be null");
			stopCriterion = criterion;
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
			scoreComparator = (a, b) -> Double.compare(b, a);
			return this;
		}

		public GeneticAlgorithmConfigBuilder<X> minimize(final Function<X, Double> fitness) {
			Objects.requireNonNull(fitness, "The fitness function cannot be null");
			fitnessFunction = fitness;
			scoreComparator = Double::compare;
			return this;
		}

		@SafeVarargs
		public final GeneticAlgorithmConfigBuilder<X> firstGeneration(final X... objects) {
			for (final X obj : objects) {
				this.firstGeneration.add(Objects.requireNonNull(obj));
			}
			return this;
		}

		public GeneticAlgorithmConfigBuilder<X> firstGeneration(final Set<X> objects) {
			for (final X obj : objects) {
				this.firstGeneration.add(Objects.requireNonNull(obj));
			}
			return this;
		}

		public GeneticAlgorithmConfig<X> build() {
			if (alreadyBuilt) {
				throw new IllegalStateException("Cannot build the same GeneticAlgorithmConfigBuilder two times");
			}

			this.alreadyBuilt = true;
			return new GeneticAlgorithmConfig<>(
					populationSize,
					survivalRate,
					crossoverRate,
					mutationRate,
					randomCreation,
					crossoverOperator,
					mutationOperator,
					fitnessFunction,
					scoreComparator,
					firstGeneration,
					maxGenerations,
					stopCriterion);
		}
	}

	public GeneticAlgorithmConfig(
			int populationSize,
			double survivalRate,
			double crossoverRate,
			double mutationRate,
			Supplier<X> creation,
			BiFunction<X, X, X> crossoverOperator,
			Function<X, X> mutationOperator,
			Function<X, Double> fitnessFunction,
			Comparator<Double> scoreComparator,
			Set<X> firstGeneration,
			int maxGenerations,
			Predicate<X> stopCriterion) {
		this.populationSize = assertPopulationSizeIsValid(populationSize);
		this.survivalRate = assertSurvivalRateIsValid(survivalRate);
		this.crossoverRate = assertCrossoverRateIsValid(crossoverRate);
		this.mutationRate = assertMutationRateIsValid(mutationRate);
		this.creation = Objects.requireNonNull(creation, "The creation function cannot be null");
		this.crossoverOperator = Objects.requireNonNull(crossoverOperator, "The crossover operator cannot be null");
		this.mutationOperator = Objects.requireNonNull(mutationOperator, "The mutation operator cannot be null");
		this.fitnessFunction = Objects.requireNonNull(fitnessFunction, "The fitness function cannot be null");
		this.scoreComparator = Objects.requireNonNull(scoreComparator, "The score comparator cannot be null");
		this.firstGeneration = Objects.requireNonNull(firstGeneration, "The first generation cannot be null");
		this.maxGenerations = assertMaxGenerationsIsValid(maxGenerations);
		this.stopCriterion = Objects.requireNonNull(stopCriterion, "The stop criterion cannot be null");

		if (firstGeneration.size() > populationSize) {
			throw new IllegalArgumentException(String.format(
					"Invalid first generation size: should have been <= %,d but was %,d",
					populationSize, firstGeneration.size()));
		}
	}
}
