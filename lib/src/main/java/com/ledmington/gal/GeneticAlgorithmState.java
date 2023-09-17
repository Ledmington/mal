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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GeneticAlgorithmState<X> {
    private final long startTime;
    private int generation = 0;
    private List<X> population;
    private List<X> nextGeneration;
    private final Map<X, Double> cachedScores;
    private final int survivingPopulation;
    private final Set<X> bestOfAllTime; // for optimization

    public GeneticAlgorithmState(
            final List<X> population,
            final List<X> nextGeneration,
            final Map<X, Double> cachedScores,
            final int survivors) {
        this.startTime = System.currentTimeMillis();
        this.population = Objects.requireNonNull(population);
        this.nextGeneration = Objects.requireNonNull(nextGeneration);
        this.cachedScores = Objects.requireNonNull(cachedScores);
        this.survivingPopulation = survivors;
        bestOfAllTime = new LinkedHashSet<>(survivors * 2, 1.0f);
    }

    public long startTime() {
        return startTime;
    }

    public List<X> population() {
        return population;
    }

    public List<X> nextGeneration() {
        return nextGeneration;
    }

    public Map<X, Double> scores() {
        return cachedScores;
    }

    public int survivingPopulation() {
        return survivingPopulation;
    }

    public Set<X> bestOfAllTime() {
        return bestOfAllTime;
    }

    public void swapPopulations() {
        final List<X> tmp = population;
        population = nextGeneration;
        nextGeneration = tmp;
    }

    public int currentGeneration() {
        return generation;
    }

    public void incrementGeneration() {
        generation++;
    }
}
