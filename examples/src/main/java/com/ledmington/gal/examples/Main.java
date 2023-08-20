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
package com.ledmington.gal.examples;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.SerialGeneticAlgorithm;

public final class Main {
    public static void main(final String[] args) {
        final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ !";
        final String targetString = "I love Genetic Algorithms!";
        final int length = targetString.length();
        final Supplier<Character> randomChar = () -> alphabet.charAt(rng.nextInt(0, alphabet.length()));

        final GeneticAlgorithm<String> ga = new SerialGeneticAlgorithm<>();
        ga.run(GeneticAlgorithmConfig.<String>builder()
                .populationSize(1000)
                .survivalRate(0.4)
                .maxGenerations(100)
                .creation(() -> Stream.generate(randomChar)
                        .limit(length)
                        .map(Object::toString)
                        .collect(Collectors.joining()))
                .mutation(s -> {
                    final int idx = rng.nextInt(0, length);
                    return s.substring(0, idx) + randomChar.get() + s.substring(idx + 1, length);
                })
                .fitness(s -> {
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
    }
}
