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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.ledmington.mal.Pair;

public final class ParallelPatternSearch<X> extends SerialPatternSearch<X> {

	private final ExecutorService executor;
	private final List<Future<Pair<X, Double>>> tasks;

	public ParallelPatternSearch(
			final double step,
			final double k,
			final double epsilon,
			final X startingPoint,
			final int d,
			final Function<X, Double> objectiveFunction,
			final TriFunction<X, Integer, Double, X> neighbor,
			final int numThreads) {
		super(step, k, epsilon, startingPoint, d, objectiveFunction, neighbor);

		this.executor = Executors.newFixedThreadPool(numThreads);
		this.tasks = new ArrayList<>(2 * d);

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

	@Override
	protected Pair<X, Double> findBestNeighbor(final X center, final double h) {
		Pair<X, Double> best = new Pair<>(null, Double.POSITIVE_INFINITY);
		for (int i = 0; i < d; i++) {
			final int finalI = i;
			// f(x+h)
			tasks.add(executor.submit(() -> {
				final X x = neighbor.apply(center, finalI, h);
				final double f = objectiveFunction.apply(x);
				return new Pair<>(x, f);
			}));
			// f(x-h)
			tasks.add(executor.submit(() -> {
				final X x = neighbor.apply(center, finalI, -h);
				final double f = objectiveFunction.apply(x);
				return new Pair<>(x, f);
			}));
		}

		for (final Future<Pair<X, Double>> fut : tasks) {
			final Pair<X, Double> res;
			try {
				res = fut.get();
			} catch (final InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			if (res.second() < best.second()) {
				best = res;
			}
		}

		return best;
	}
}
