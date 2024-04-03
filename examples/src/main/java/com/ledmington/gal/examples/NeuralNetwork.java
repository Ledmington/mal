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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import com.ledmington.gal.GeneticAlgorithm;
import com.ledmington.gal.GeneticAlgorithmConfig;
import com.ledmington.gal.ParallelGeneticAlgorithm;

public final class NeuralNetwork {

    private static final RandomGenerator rng =
            RandomGeneratorFactory.getDefault().create(System.nanoTime());

    private static float sigmoid(float x) {
        return (float) (Math.exp(x) / (1.0f + Math.exp(x)));
    }

    private record Point(float x, float y) {}

    /**
     * A class representing a simple neural network with one middle layer.
     */
    private static final class Network {

        private final float[][] w1; // weights of the middle layer
        private final float[] b1; // biases of the middle layer
        private final float[][] w2; // weights of the output layer
        private final float[] b2; // biases of the output layer
        private int cachedHashCode;
        private boolean isHashCodeSet = false;

        public Network(int nInputs, int nMiddle, int nOutputs, boolean initialize) {
            this.w1 = new float[nInputs][nMiddle];
            this.b1 = new float[nMiddle];
            this.w2 = new float[nMiddle][nOutputs];
            this.b2 = new float[nOutputs];

            if (initialize) {
                for (int i = 0; i < nMiddle; i++) {
                    for (int j = 0; j < nInputs; j++) {
                        this.w1[j][i] = rng.nextFloat(-1.0f, 1.0f);
                    }
                    this.b1[i] = rng.nextFloat(-nInputs, nInputs);
                }
                for (int i = 0; i < nOutputs; i++) {
                    for (int j = 0; j < nMiddle; j++) {
                        this.w2[j][i] = rng.nextFloat(-1.0f, 1.0f);
                    }
                    this.b2[i] = rng.nextFloat(-nMiddle, nMiddle);
                }
            }
        }

        public Network(final Network n) {
            final int nInputs = n.w1.length;
            final int nMiddle = n.w2.length;
            final int nOutputs = n.w2[0].length;
            this.w1 = new float[nInputs][nMiddle];
            this.b1 = new float[nMiddle];
            this.w2 = new float[nMiddle][nOutputs];
            this.b2 = new float[nOutputs];
            this.cachedHashCode = -1;
            this.isHashCodeSet = false;

            for (int i = 0; i < nInputs; i++) {
                System.arraycopy(n.w1[i], 0, this.w1[i], 0, nMiddle);
            }
            System.arraycopy(n.b1, 0, this.b1, 0, nMiddle);
            for (int i = 0; i < nMiddle; i++) {
                System.arraycopy(n.w2[i], 0, this.w2[i], 0, nOutputs);
            }
            System.arraycopy(n.b2, 0, this.b2, 0, nOutputs);
        }

        public float[] predict(final float[] inputs) {
            if (inputs.length != w1.length) {
                throw new IllegalArgumentException(
                        String.format("Wrong input size: expected %,d but was %,d", w1.length, inputs.length));
            }

            final float[] middleLayerResult = new float[w1[0].length];
            for (int i = 0; i < w1.length; i++) {
                for (int j = 0; j < w1[i].length; j++) {
                    middleLayerResult[j] += inputs[i] * w1[i][j];
                }
            }
            for (int i = 0; i < b1.length; i++) {
                middleLayerResult[i] = sigmoid(middleLayerResult[i] + b1[i]);
            }

            final float[] outputLayerResult = new float[w2[0].length];
            for (int i = 0; i < w2.length; i++) {
                for (int j = 0; j < w2[i].length; j++) {
                    outputLayerResult[j] += middleLayerResult[i] * w2[i][j];
                }
            }
            for (int i = 0; i < b2.length; i++) {
                outputLayerResult[i] = sigmoid(outputLayerResult[i] + b2[i]);
            }

            return outputLayerResult;
        }

        public int hashCode() {
            if (isHashCodeSet) {
                return cachedHashCode;
            }

            int h = 17;
            for (final float[] vf : w1) {
                for (final float f : vf) {
                    h = 31 * h + Float.floatToIntBits(f);
                }
            }
            for (final float b : b1) {
                h = 31 * h + Float.floatToIntBits(b);
            }
            for (final float[] vf : w2) {
                for (final float f : vf) {
                    h = 31 * h + Float.floatToIntBits(f);
                }
            }
            for (final float b : b2) {
                h = 31 * h + Float.floatToIntBits(b);
            }

            cachedHashCode = h;
            isHashCodeSet = true;
            return cachedHashCode;
        }
    }

    public NeuralNetwork() {
        /*
        The problem we want to solve is really simple: a classification of three
        overlapping clusters of points in the 2-D cartesian plane.
        So, the input variables are 2 and the output variables are 3.
         */
        final int inputVariables = 2;
        final int outputVariables = 3;
        final int sizeMiddleLayer = 32;
        final int nPoints = 10_000;
        final Point[] dataset = new Point[nPoints];
        final float[][] solutions = new float[nPoints][outputVariables]; // default initialized to 0.0f

        for (int i = 0; i < nPoints; i++) {
            switch (rng.nextInt(0, 3)) {
                case 0 -> {
                    dataset[i] = new Point(rng.nextFloat(-3.0f, 3.0f), rng.nextFloat(0.0f, 4.0f));
                    solutions[i][0] = 1.0f;
                }
                case 1 -> {
                    dataset[i] = new Point(rng.nextFloat(-5.0f, 1.0f), rng.nextFloat(-1.0f, 2.0f));
                    solutions[i][1] = 1.0f;
                }
                case 2 -> {
                    dataset[i] = new Point(rng.nextFloat(2.0f, 4.0f), rng.nextFloat(-3.0f, 1.0f));
                    solutions[i][2] = 1.0f;
                }
                default -> throw new IllegalStateException("There should have been only three clusters of points");
            }
        }

        final ExecutorService ex =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final GeneticAlgorithm<Network> ga = new ParallelGeneticAlgorithm<>(ex, rng);

        ga.setState(GeneticAlgorithmConfig.<Network>builder()
                .populationSize(1_000)
                .maxGenerations(100)
                .survivalRate(0.1)
                .crossoverRate(0.8)
                .mutationRate(0.1)
                .creation(() -> new Network(inputVariables, sizeMiddleLayer, outputVariables, true))
                .crossover((a, b) -> {
                    final Network son = new Network(inputVariables, sizeMiddleLayer, outputVariables, false);

                    for (int i = 0; i < sizeMiddleLayer; i++) {
                        for (int j = 0; j < inputVariables; j++) {
                            son.w1[j][i] = rng.nextBoolean() ? a.w1[j][i] : b.w1[j][i];
                        }
                        son.b1[i] = rng.nextBoolean() ? a.b1[i] : b.b1[i];
                    }
                    for (int i = 0; i < outputVariables; i++) {
                        for (int j = 0; j < sizeMiddleLayer; j++) {
                            son.w2[j][i] = rng.nextBoolean() ? a.w2[j][i] : b.w2[j][i];
                        }
                        son.b2[i] = rng.nextBoolean() ? a.b2[i] : b.b2[i];
                    }

                    return son;
                })
                .mutation(x -> {
                    final Network mutated = new Network(x);

                    // choosing to mutate a weight or a bias of which layer
                    if (rng.nextBoolean()) {
                        if (rng.nextBoolean()) {
                            mutated.w1[rng.nextInt(0, inputVariables)][rng.nextInt(0, sizeMiddleLayer)] =
                                    rng.nextFloat(-1.0f, 1.0f);
                        } else {
                            mutated.b1[rng.nextInt(0, sizeMiddleLayer)] =
                                    rng.nextFloat(-inputVariables, inputVariables);
                        }
                    } else {
                        if (rng.nextBoolean()) {
                            mutated.w2[rng.nextInt(0, sizeMiddleLayer)][rng.nextInt(0, outputVariables)] =
                                    rng.nextFloat(-1.0f, 1.0f);
                        } else {
                            mutated.b2[rng.nextInt(0, outputVariables)] =
                                    rng.nextFloat(-sizeMiddleLayer, sizeMiddleLayer);
                        }
                    }

                    return mutated;
                })
                .minimize(x -> {
                    // minimize RMSE
                    double score = 0.0;
                    for (int i = 0; i < nPoints; i++) {
                        final float[] outputs = x.predict(new float[] {dataset[i].x, dataset[i].y});
                        for (int j = 0; j < outputVariables; j++) {
                            score += (outputs[j] - solutions[i][j]) * (outputs[j] - solutions[i][j]);
                        }
                    }

                    return Math.sqrt(score);
                })
                .build());
        ga.run();

        if (!ex.isShutdown()) {
            ex.shutdown();
        }
        while (!ex.isTerminated()) {
            try {
                if (ex.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (final InterruptedException ignored) {
            }
        }
    }
}
