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
package com.ledmington.mal.annealing;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class SimulatedAnnealing<X> {

	private static final RandomGenerator rng =
			RandomGeneratorFactory.getDefault().create(System.nanoTime());

	private final Supplier<X> randomSolution;
	private final Function<X, X> generateRandomNeighbor;
	private final Function<X, Double> fitness;

	public SimulatedAnnealing(
			final Supplier<X> randomSolution,
			final Function<X, X> generateRandomNeighbor,
			final Function<X, Double> fitness) {
		this.randomSolution = Objects.requireNonNull(randomSolution);
		this.generateRandomNeighbor = Objects.requireNonNull(generateRandomNeighbor);
		this.fitness = Objects.requireNonNull(fitness);
	}

	private double acceptance(final double currentEnergy, final double nextEnergy, final double temperature) {
		if (temperature > 1.0 || temperature < 0.0) {
			throw new AssertionError(
					String.format("Temperature should always be between 0.0 and 1.0 but was %+.6e.", temperature));
		}
		if (nextEnergy < currentEnergy) {
			return 1.0;
		}
		return Math.exp((currentEnergy - nextEnergy) / temperature);
	}

	public void run() {
		final int maxIterations = 10_000;

		X current = randomSolution.get();
		X next;
		double currentEnergy = fitness.apply(current);
		double nextEnergy;

		for (int k = 0; k < maxIterations; k++) {
			final double temperature = 1.0 - ((double) (k + 1) / (double) maxIterations);
			System.out.printf("Iteration %,d of %,d (t = %.6e)%n", k, maxIterations, temperature);
			System.out.printf("  Best solution : '%s'%n", current);
			System.out.printf("  Best score : %+.6f%n", currentEnergy);
			System.out.println();

			next = generateRandomNeighbor.apply(current);
			nextEnergy = fitness.apply(next);
			if (acceptance(currentEnergy, nextEnergy, temperature) <= rng.nextDouble(0.0, 1.0)) {
				current = next;
				currentEnergy = nextEnergy;
			}
		}
	}
}
