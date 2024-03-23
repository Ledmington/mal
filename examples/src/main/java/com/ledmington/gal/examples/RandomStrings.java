/*
* genetic-algorithms-library - A library for genetic algorithms.
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
package com.ledmington.gal.examples;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.SerialGeneticAlgorithm;

public final class RandomStrings {
    public RandomStrings() {
        final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ !";
        final String targetString = "I love Genetic Algorithms!";
        final int length = targetString.length();
        final Supplier<Character> randomChar = () -> alphabet.charAt(rng.nextInt(0, alphabet.length()));

        final GeneticAlgorithm<String> ga = new SerialGeneticAlgorithm<>();
        ga.setState(GeneticAlgorithmConfig.<String>builder()
                .populationSize(1000)
                .survivalRate(0.1)
                .crossoverRate(0.7)
                .mutationRate(0.01)
                .maxGenerations(1000)
                .creation(() -> Stream.generate(randomChar)
                        .limit(length)
                        .map(Object::toString)
                        .collect(Collectors.joining()))
                .crossover((a, b) -> {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < targetString.length(); i++) {
                        final double choice = rng.nextDouble(0.0, 1.0);
                        if (choice < 0.45) {
                            // take char from parent A
                            sb.append(a.charAt(i));
                        } else if (choice < 0.9) {
                            // take char from parent B
                            sb.append(b.charAt(i));
                        } else {
                            // add random character
                            sb.append(randomChar.get());
                        }
                    }
                    return sb.toString();
                })
                .mutation(s -> {
                    final int idx = rng.nextInt(0, length);
                    return s.substring(0, idx) + randomChar.get() + s.substring(idx + 1, length);
                })
                .maximize(s -> {
                    if (s.length() != length) {
                        throw new IllegalArgumentException(
                                String.format("Invalid length: was %d but should have been %d", s.length(), length));
                    }
                    int count = 0;
                    for (int i = 0; i < s.length(); i++) {
                        if (s.charAt(i) == targetString.charAt(i)) {
                            count++;
                        }
                    }
                    return (double) count;
                })
                .build());
        ga.run();
    }
}
