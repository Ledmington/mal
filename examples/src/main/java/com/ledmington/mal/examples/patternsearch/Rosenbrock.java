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
package com.ledmington.mal.examples.patternsearch;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import com.ledmington.mal.patternsearch.PatternSearch;

public final class Rosenbrock {

	private static final class Solution {

		private final double[] x;

		Solution(final double[] x) {
			this.x = x;
		}

		public double get(final int idx) {
			return x[idx];
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append('(');
			if (x.length > 0) {
				sb.append(String.format("%.6f", x[0]));
				for (int i = 1; i < x.length; i++) {
					sb.append(", ").append(String.format("%.6f", x[i]));
				}
			}
			sb.append(')');
			return sb.toString();
		}
	}

	public Rosenbrock() {
		final long beginning = System.nanoTime();

		final double a = 1.0;
		final double b = 10.0;
		final int d = 16;
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());

		final PatternSearch<Solution> ps = new PatternSearch<>(
				1.0,
				0.9,
				1.0e-6,
				new Solution(IntStream.range(0, d)
						.mapToDouble(x -> rng.nextDouble(-5.0, 5.0))
						.toArray()),
				d,
				sol -> {
					double s = 0.0;
					for (int i = 0; i < d - 1; i++) {
						final double x = sol.get(i);
						final double y = sol.get(i + 1);
						final double t1 = a - x;
						final double t2 = y - x * x;
						s += t1 * t1 + b * t2 * t2;
					}
					return s;
				},
				(solution, position, h) -> {
					final double[] newX = new double[d];
					for (int i = 0; i < d; i++) {
						newX[i] = solution.get(i);
					}
					newX[position] += h;
					return new Solution(newX);
				});
		ps.run();

		final long end = System.nanoTime();

		System.out.printf("Total search time: %.3f seconds%n", (double) (end - beginning) / 1_000_000_000.0);
	}
}
