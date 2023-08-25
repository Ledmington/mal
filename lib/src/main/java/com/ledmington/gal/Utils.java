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

import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public final class Utils {
    private Utils() {}

    public static <X> X weightedChoose(
            final List<X> values, final Function<X, Double> weight, final RandomGenerator rng) {
        final double minWeight =
                values.stream().mapToDouble(weight::apply).min().orElseThrow();
        final double maxWeight =
                values.stream().mapToDouble(weight::apply).max().orElseThrow();
        final Function<Double, Double> scaler = w -> (w - minWeight) / (maxWeight - minWeight);
        final double totalWeight =
                values.stream().mapToDouble(x -> scaler.apply(weight.apply(x))).sum();
        final double chosenWeight = rng.nextDouble(0.0, totalWeight);
        double sum = 0.0;
        for (int i = 0; i < values.size(); i++) {
            if (sum >= chosenWeight) {
                return values.get(i - 1);
            }
            sum += scaler.apply(weight.apply(values.get(i)));
        }
        return values.get(values.size() - 1);
    }
}
