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

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.SerialGeneticAlgorithm;

public final class Knapsack {
    public Knapsack() {
        final RandomGenerator rng = RandomGeneratorFactory.getDefault().create(System.nanoTime());
        final int nItems = 100;
        final int[] weights = new int[nItems];
        final double[] values = new double[nItems];
        final int capacity = 30;

        for (int i = 0; i < nItems; i++) {
            weights[i] = rng.nextInt(1, 6);
            values[i] = rng.nextDouble(0.1, 6.0);
        }

        final GeneticAlgorithm<boolean[]> ga = new SerialGeneticAlgorithm<>();

        ga.run(GeneticAlgorithmConfig.<boolean[]>builder()
                .populationSize(1000)
                .maxGenerations(1000)
                .survivalRate(0.1)
                .crossoverRate(0.8)
                .mutationRate(0.1)
                .creation(() -> {
                    final boolean[] v = new boolean[nItems];
                    int c = 0;
                    while (c < capacity) {
                        int toBeAdded;
                        do {
                            toBeAdded = rng.nextInt(0, nItems);
                        } while (v[toBeAdded]);

                        if (c + weights[toBeAdded] > capacity) {
                            break;
                        }

                        v[toBeAdded] = true;
                        c += weights[toBeAdded];
                    }
                    return v;
                })
                .crossover((a, b) -> {
                    // OR the parents together
                    final boolean[] v = new boolean[nItems];
                    int c = 0;
                    for (int i = 0; i < nItems; i++) {
                        if (a[i] || b[i]) {
                            v[i] = true;
                            c += weights[i];
                        }
                    }

                    // randomly remove items until the capacity is valid
                    while (c > capacity) {
                        int toBeRemoved;
                        do {
                            toBeRemoved = rng.nextInt(0, nItems);
                        } while (!v[toBeRemoved]);
                        v[toBeRemoved] = false;
                        c -= weights[toBeRemoved];
                    }

                    return v;
                })
                .mutation(x -> {
                    // compute capacity
                    int c = 0;
                    for (int i = 0; i < nItems; i++) {
                        if (x[i]) {
                            c += weights[i];
                        }
                    }

                    // add a random item
                    int toBeAdded;
                    do {
                        toBeAdded = rng.nextInt(0, nItems);
                    } while (x[toBeAdded]);
                    x[toBeAdded] = true;
                    c += weights[toBeAdded];

                    // remove random items until the capacity is valid
                    while (c > capacity) {
                        int toBeRemoved;
                        do {
                            toBeRemoved = rng.nextInt(0, nItems);
                        } while (toBeAdded != toBeRemoved && !x[toBeRemoved]);
                        x[toBeRemoved] = false;
                        c -= weights[toBeRemoved];
                    }

                    return x;
                })
                .fitness(x -> {
                    double s = 0.0;
                    for (int i = 0; i < nItems; i++) {
                        if (x[i]) {
                            s += values[i];
                        }
                    }
                    return s;
                })
                .serializer(x -> {
                    final StringBuilder sb = new StringBuilder("|");
                    for (int i = 0; i < nItems; i++) {
                        if (x[i]) {
                            sb.append(i).append("|");
                        }
                    }
                    return sb.toString();
                })
                .build());
    }
}
