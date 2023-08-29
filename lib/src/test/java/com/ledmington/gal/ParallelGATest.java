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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Executors;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public final class ParallelGATest extends GATest {
    @BeforeEach
    public void setup() {
        ga = new ParallelGeneticAlgorithm<>();
    }

    @Test
    public void nullExecutor() {
        assertThrows(
                NullPointerException.class,
                () -> new ParallelGeneticAlgorithm<String>(
                        null, RandomGeneratorFactory.getDefault().create(System.nanoTime())));
    }

    @Test
    public void nullRandomGenerator() {
        assertThrows(
                NullPointerException.class,
                () -> new ParallelGeneticAlgorithm<String>(Executors.newSingleThreadExecutor(), null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 0})
    public void invalidThreads(int nThreads) {
        assertThrows(IllegalArgumentException.class, () -> new ParallelGeneticAlgorithm<String>(nThreads));
    }
}
