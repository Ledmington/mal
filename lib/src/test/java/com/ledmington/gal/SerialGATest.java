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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class SerialGATest extends GATest {
    @BeforeEach
    public void setup() {
        ga = new SerialGeneticAlgorithm<>();
    }

    @Test
    public void nullRandomGenerator() {
        assertThrows(NullPointerException.class, () -> new SerialGeneticAlgorithm<>(null));
    }

    @Test
    public void determinism() {
        // two algorithms with the same config and the same RandomGenerator must return the same result (the best
        // solution)
        final long seed = System.nanoTime();
        RandomGenerator rng;

        rng = RandomGeneratorFactory.getDefault().create(seed);
        final RandomGenerator finalRNG1 = rng;
        ga = new SerialGeneticAlgorithm<>(rng);
        ga.setState(GeneticAlgorithmConfig.<String>builder()
                .maxGenerations(10)
                .creation(() -> String.valueOf(finalRNG1.nextInt()))
                .crossover((a, b) -> String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)))
                .mutation(x -> String.valueOf(Integer.parseInt(x) + 1))
                .maximize(s -> (double) s.length())
                .build());
        ga.run();
        final String firstBest = ga.getState().scores().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();

        rng = RandomGeneratorFactory.getDefault().create(seed);
        final RandomGenerator finalRNG2 = rng;
        ga = new SerialGeneticAlgorithm<>(rng);
        ga.setState(GeneticAlgorithmConfig.<String>builder()
                .maxGenerations(10)
                .creation(() -> String.valueOf(finalRNG2.nextInt()))
                .crossover((a, b) -> String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)))
                .mutation(x -> String.valueOf(Integer.parseInt(x) + 1))
                .maximize(s -> (double) s.length())
                .build());
        ga.run();
        final String secondBest = ga.getState().scores().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();

        assertEquals(firstBest, secondBest);
    }
}
