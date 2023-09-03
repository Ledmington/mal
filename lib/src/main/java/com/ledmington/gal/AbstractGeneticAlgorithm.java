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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public abstract sealed class AbstractGeneticAlgorithm<X> implements GeneticAlgorithm<X>
        permits ParallelGeneticAlgorithm, SerialGeneticAlgorithm {

    protected final RandomGenerator rng;
    protected List<X> population = null;
    protected List<X> nextGeneration = null;
    protected Map<X, Double> cachedScores = null;

    public AbstractGeneticAlgorithm(final RandomGenerator rng) {
        this.rng = Objects.requireNonNull(rng);
    }

    public AbstractGeneticAlgorithm() {
        this(RandomGeneratorFactory.getDefault().create(System.nanoTime()));
    }

    protected void resetState(int populationSize) {
        population = new ArrayList<>(populationSize);
        nextGeneration = new ArrayList<>(populationSize);
        cachedScores = new ConcurrentHashMap<>(populationSize);
    }
}
