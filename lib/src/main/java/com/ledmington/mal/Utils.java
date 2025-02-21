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
package com.ledmington.mal;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

public final class Utils {

	private Utils() {}

	public static <X> Supplier<X> weightedChoose(
			final List<X> values, final Function<X, Double> weight, final RandomGenerator rng) {
		Objects.requireNonNull(values, "The list of values cannot be null");
		Objects.requireNonNull(weight, "The weight function cannot be null");
		Objects.requireNonNull(rng, "The RandomGenerator cannot be null");

		if (values.isEmpty()) {
			throw new IllegalArgumentException("The list of values cannot be empty");
		}

		final Function<X, Double> safeWeight = x -> {
			final double result = weight.apply(x);
			if (result < 0.0) {
				throw new IllegalArgumentException(String.format(
						"Negative weights are not allowed: the object '%s' produced the weight %f",
						x.toString(), result));
			}
			return result;
		};
		final double totalWeight =
				values.stream().mapToDouble(safeWeight::apply).sum();

		return () -> {
			final double chosenWeight = rng.nextDouble(0.0, totalWeight);

			double sum = 0.0;
			for (int i = 0; i < values.size() - 1; i++) {
				final X ith_element = values.get(i);
				sum += safeWeight.apply(ith_element);
				if (sum >= chosenWeight) {
					return ith_element;
				}
			}

			return values.getLast();
		};
	}
}
