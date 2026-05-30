/*
 * minimization-algorithms-library - A collection of minimization algorithms.
 * Copyright (C) 2023-2026 Filippo Barbari <filippo.barbari@gmail.com>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ledmington.mal.patternsearch;

import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class PatternSearchBuilder<X> {

	private static final int DEFAULT_NUM_THREADS = 1;

	private double step = 1.0;
	private double k = 0.5;
	private double epsilon = 1e-6;
	private int dimensions = 2;
	private X startingPoint = null;
	private Function<X, Double> objectiveFunction = null;
	private TriFunction<X, Integer, Double, X> neighbor = null;
	private int numThreads = DEFAULT_NUM_THREADS;

	public PatternSearchBuilder() {}

	public PatternSearchBuilder<X> step(final double step) {
		final double minimumStep = 0.0;
		if (step <= minimumStep) {
			throw new IllegalArgumentException("step must be >0.0.");
		}
		this.step = step;
		return this;
	}

	public PatternSearchBuilder<X> factor(final double k) {
		final double minimumFactor = 0.0;
		final double maximumFactor = 1.0;
		if (k <= minimumFactor || k >= maximumFactor) {
			throw new IllegalArgumentException("factor must be >0.0 and <1.0.");
		}
		this.k = k;
		return this;
	}

	public PatternSearchBuilder<X> epsilon(final double epsilon) {
		final double minimumEpsilon = 0.0;
		if (epsilon < minimumEpsilon) {
			throw new IllegalArgumentException("Epsilon must be >=0.0.");
		}
		this.epsilon = epsilon;
		return this;
	}

	public PatternSearchBuilder<X> startingPoint(final X startingPoint) {
		this.startingPoint = Objects.requireNonNull(startingPoint);
		return this;
	}

	public PatternSearchBuilder<X> dimensions(final int dimensions) {
		final int minimumDimensions = 1;
		if (dimensions < minimumDimensions) {
			throw new IllegalArgumentException("dimensions must be >=1.");
		}
		this.dimensions = dimensions;
		return this;
	}

	public PatternSearchBuilder<X> minimize(final Function<X, Double> objectiveFunction) {
		this.objectiveFunction = Objects.requireNonNull(objectiveFunction);
		return this;
	}

	public PatternSearchBuilder<X> neighbor(final TriFunction<X, Integer, Double, X> neighbor) {
		this.neighbor = Objects.requireNonNull(neighbor);
		return this;
	}

	public PatternSearchBuilder<X> parallel(final int numThreads) {
		if (this.numThreads != DEFAULT_NUM_THREADS) {
			throw new IllegalArgumentException("Cannot set number of threads twice.");
		}
		final int minimumThreads = 1;
		if (numThreads < minimumThreads || numThreads > Runtime.getRuntime().availableProcessors()) {
			throw new IllegalArgumentException(String.format(
					"Number of threads must be >=%,d and <=%,d.",
					minimumThreads, Runtime.getRuntime().availableProcessors()));
		}
		this.numThreads = numThreads;
		return this;
	}

	public PatternSearch<X> build() {
		if (numThreads == 1) {
			return new SerialPatternSearch<>(step, k, epsilon, startingPoint, dimensions, objectiveFunction, neighbor);
		} else {
			return new ParallelPatternSearch<>(
					step, k, epsilon, startingPoint, dimensions, objectiveFunction, neighbor, numThreads);
		}
	}
}
