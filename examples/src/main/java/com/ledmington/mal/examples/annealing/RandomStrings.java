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
package com.ledmington.mal.examples.annealing;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import com.ledmington.mal.annealing.SimulatedAnnealing;

public final class RandomStrings {
	public RandomStrings() {
		final long beginning = System.nanoTime();
		final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
		final String alphabet =
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,:;-_@#[]{}()!?='\"+*/";
		final String targetString =
				"This library for Minimization Algorithms is absolutely fantastic! I cannot wait to try and use it with Java 21 and Gradle 8.11! Now let's write another time just to have longer strings and, therefore, add artificial complexity to the problem.";
		final int targetLength = targetString.length();
		final Supplier<Character> randomChar = () -> alphabet.charAt(rng.nextInt(0, alphabet.length()));

		final SimulatedAnnealing<String> sa = new SimulatedAnnealing<>(
				() -> {
					final StringBuilder sb = new StringBuilder();
					for (int i = 0; i < targetLength; i++) {
						sb.append(randomChar.get());
					}
					return sb.toString();
				},
				s -> {
					final StringBuilder sb = new StringBuilder();
					final double p = rng.nextDouble(0.0, 1.0);
					for (int i = 0; i < s.length(); i++) {
						sb.append(rng.nextDouble(0.0, 1.0) < p ? randomChar.get() : s.charAt(i));
					}
					return sb.toString();
				},
				s -> {
					int count = Math.abs(targetLength - s.length());
					for (int i = 0; i < Math.min(targetLength, s.length()); i++) {
						if (s.charAt(i) != targetString.charAt(i)) {
							count++;
						}
					}
					return (double) count;
				});
		sa.run();

		final long end = System.nanoTime();

		System.out.printf("Total search time: %.3f seconds%n", (double) (end - beginning) / 1_000_000_000.0);
	}
}
