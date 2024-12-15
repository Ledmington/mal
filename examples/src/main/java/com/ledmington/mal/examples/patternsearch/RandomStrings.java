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

import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ledmington.mal.patternsearch.PatternSearch;

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

		final PatternSearch<String> ps = PatternSearch.<String>builder()
				.step(1.0)
				.factor(0.5)
				.epsilon(1.0)
				.startingPoint(IntStream.range(0, targetLength)
						.mapToObj(x -> "" + randomChar.get())
						.collect(Collectors.joining()))
				.dimensions(targetLength)
				.minimize(s -> {
					int count = Math.abs(targetLength - s.length());
					for (int i = 0; i < Math.min(targetLength, s.length()); i++) {
						if (s.charAt(i) != targetString.charAt(i)) {
							count++;
						}
					}
					return (double) count;
				})
				.neighbor((s, d, h) -> {
					final int index = alphabet.indexOf(s.charAt(d));
					final int newIndex = ((int) (index + h) + alphabet.length()) % alphabet.length();
					final char newChar = alphabet.charAt(newIndex);
					if (d == 0) {
						return newChar + s.substring(1);
					}
					return s.substring(0, d) + newChar + s.substring(d + 1);
				})
				.build();
		ps.run();

		final long end = System.nanoTime();

		System.out.printf("Total search time: %.3f seconds%n", (double) (end - beginning) / 1_000_000_000.0);
	}
}
