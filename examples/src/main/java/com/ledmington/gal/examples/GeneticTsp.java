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

import java.util.Arrays;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.SerialGeneticAlgorithm;

public final class GeneticTsp {

    private static final RandomGenerator rng =
            RandomGeneratorFactory.getDefault().create(System.nanoTime());

    private static void shuffle(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            int j;
            do {
                j = rng.nextInt(0, arr.length);
            } while (i == j);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private record Solution(int[] array) {}

    public GeneticTsp() {

        final int nCities = 20;
        final double[][] coordinates = new double[2][nCities];

        for (int i = 0; i < nCities; i++) {
            coordinates[0][i] = rng.nextDouble(-10.0, 10.0);
            coordinates[1][i] = rng.nextDouble(-10.0, 10.0);
        }

        final double[][] distances = new double[nCities][nCities];

        for (int i = 0; i < nCities; i++) {
            for (int j = 0; j < nCities; j++) {
                final double dist =
                        Math.hypot(coordinates[0][i] - coordinates[0][j], coordinates[1][i] - coordinates[1][j]);
                distances[i][j] = dist;
                distances[j][i] = dist;
            }
        }

        final GeneticAlgorithmConfig<Solution> config = GeneticAlgorithmConfig.<Solution>builder()
                .populationSize(100)
                .survivalRate(0.2)
                .mutationRate(0.2)
                .crossoverRate(0.1)
                .creation(() -> {
                    final int[] v = new int[nCities];
                    for (int i = 0; i < nCities; i++) {
                        v[i] = i;
                    }
                    shuffle(v);
                    return new Solution(v);
                })
                .crossover((a, b) -> {
                    final int[] result = new int[nCities];

                    for (int i = 0; i < nCities; i++) {
                        final double choice = rng.nextDouble(0.0, 1.0);
                        if (choice < 0.45) {
                            result[i] = a.array()[i];
                        } else if (choice < 0.9) {
                            result[i] = b.array()[i];
                        } else {
                            result[i] = rng.nextInt(0, nCities);
                        }
                    }

                    int mistakes;
                    // we create a new input randomly mixing the two parents, until we get one that is valid
                    do {
                        final int i = rng.nextInt(0, nCities);
                        final double choice = rng.nextDouble(0.0, 1.0);
                        if (choice < 0.45) {
                            result[i] = a.array()[i];
                        } else if (choice < 0.9) {
                            result[i] = b.array()[i];
                        } else {
                            result[i] = rng.nextInt(0, nCities);
                        }

                        mistakes = (int)
                                (nCities - Arrays.stream(result).distinct().count());

                    } while (mistakes > 0);
                    return new Solution(result);
                })
                .mutation(x -> {
                    final int[] y = new int[nCities];
                    System.arraycopy(x.array(), 0, y, 0, nCities);
                    final int i = rng.nextInt(0, nCities);
                    int j;
                    do {
                        j = rng.nextInt(0, nCities);
                    } while (i == j);
                    int tmp = y[i];
                    y[i] = y[j];
                    y[j] = tmp;
                    return new Solution(y);
                })
                .minimize(x -> {
                    double s = 0.0;
                    for (int i = 0; i < nCities; i++) {
                        s += distances[x.array()[i]][x.array()[(i + 1) % nCities]];
                    }
                    // we return it inverted because it is a maximization algorithm for a minimization problem
                    return s;
                })
                .build();

        final GeneticAlgorithm<Solution> ga = new SerialGeneticAlgorithm<>();
        ga.setState(config);
        ga.run();
    }
}
