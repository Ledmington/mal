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
package com.ledmington.mal.patternsearch;

import java.util.Objects;
import java.util.function.Function;

import com.ledmington.mal.Pair;

/**
 * A simple yet highly inefficient derivative-free minimization algorithm. Each step, the objective function is
 * evaluated on 2*d points so this algorithm is not ideal for high-dimensionality problems.
 *
 * @param <X> The type of the solution.
 */
public class SerialPatternSearch<X> implements PatternSearch<X> {

	/**
	 * How much to move on each dimension when trying new points. If x is the center, we try f(x+h) and f(x-h) for each
	 * dimension.
	 */
	private final double step;

	/** Factor to multiply h when shrinking. */
	private final double k;

	/** Minimum value of h to be tried. */
	private final double epsilon;

	/** The center point at the beginning of the search. */
	private final X startingPoint;

	/** Number of dimensions (must be fixed). */
	protected final int d;

	/** Objective function to be minimized. */
	protected final Function<X, Double> objectiveFunction;

	/** Function to generate a neighbor by modifying the i-th coordinate by h. */
	protected final TriFunction<X, Integer, Double, X> neighbor;

	public SerialPatternSearch(
			final double step,
			final double k,
			final double epsilon,
			final X startingPoint,
			final int d,
			final Function<X, Double> objectiveFunction,
			final TriFunction<X, Integer, Double, X> neighbor) {
		if (epsilon < 0.0) {
			throw new IllegalArgumentException("Epsilon must be >=0.0.");
		}
		this.epsilon = epsilon;
		if (step <= 0.0) {
			throw new IllegalArgumentException("step must be >0.0.");
		}
		this.step = step;
		if (k <= 0.0 || k >= 1.0) {
			throw new IllegalArgumentException("k must be >0.0 and <1.0.");
		}
		this.k = k;
		this.startingPoint = Objects.requireNonNull(startingPoint);
		if (d < 1) {
			throw new IllegalArgumentException("d must be >=1.");
		}
		this.d = d;
		this.objectiveFunction = Objects.requireNonNull(objectiveFunction);
		this.neighbor = Objects.requireNonNull(neighbor);
	}

	protected Pair<X, Double> findBestNeighbor(final X center, final double h) {
		X best = null;
		double f1 = Double.POSITIVE_INFINITY;
		for (int i = 0; i < d; i++) {
			// f(x+h)
			{
				final X x = neighbor.apply(center, i, h);
				final double f = objectiveFunction.apply(x);
				if (f < f1) {
					f1 = f;
					best = x;
				}
			}
			// f(x-h)
			{
				final X x = neighbor.apply(center, i, -h);
				final double f = objectiveFunction.apply(x);
				if (f < f1) {
					f1 = f;
					best = x;
				}
			}
		}

		return new Pair<>(best, f1);
	}

	@Override
	public void run() {
		X center = startingPoint;
		double f0 = objectiveFunction.apply(center);
		double h = step;
		while (h >= epsilon) {
			System.out.printf("h: %.6f.%n", h);
			System.out.printf("Current center: '%s'.%n", center);
			System.out.printf("Value at center: %.6f.%n", f0);
			System.out.println();

			final Pair<X, Double> bestNeighbor = findBestNeighbor(center, h);

			// If we found a better neighbor, we move to it without modifying h
			if (bestNeighbor.second() < f0) {
				f0 = bestNeighbor.second();
				center = bestNeighbor.first();
			} else {
				// If all neighbors are worse than the center, we shrink.
				h *= k;
			}
		}
	}
}
