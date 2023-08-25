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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class GAConfigTest {

    private GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<String> b;

    @BeforeEach
    public void setup() {
        b = GeneticAlgorithmConfig.builder();
    }

    @Test
    public void cannotBuildWithNoParameters() {
        assertThrows(NullPointerException.class, () -> b.build());
    }

    @Test
    public void invalidPopulationSize() {
        assertThrows(IllegalArgumentException.class, () -> b.populationSize(-1));
        assertThrows(IllegalArgumentException.class, () -> b.populationSize(0));
        assertThrows(IllegalArgumentException.class, () -> b.populationSize(1));
    }

    @Test
    public void invalidMaxGenerations() {
        assertThrows(IllegalArgumentException.class, () -> b.maxGenerations(-2));
        assertThrows(IllegalArgumentException.class, () -> b.maxGenerations(-1));
    }

    @Test
    public void invalidSurvivalRate() {
        assertThrows(IllegalArgumentException.class, () -> b.survivalRate(-0.1));
        assertThrows(IllegalArgumentException.class, () -> b.survivalRate(0.0));
        assertThrows(IllegalArgumentException.class, () -> b.survivalRate(1.0));
        assertThrows(IllegalArgumentException.class, () -> b.survivalRate(1.1));
    }

    @Test
    public void invalidCrossoverRate() {
        assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(-0.1));
        assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(0.0));
        assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(1.0));
        assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(1.1));
    }

    @Test
    public void invalidMutationRate() {
        assertThrows(IllegalArgumentException.class, () -> b.mutationRate(-0.1));
        assertThrows(IllegalArgumentException.class, () -> b.mutationRate(0.0));
        assertThrows(IllegalArgumentException.class, () -> b.mutationRate(1.0));
        assertThrows(IllegalArgumentException.class, () -> b.mutationRate(1.1));
    }

    @Test
    public void invalidIndividualSupplier() {
        assertThrows(NullPointerException.class, () -> b.creation(null));
    }

    @Test
    public void invalidCrossoverOperator() {
        assertThrows(NullPointerException.class, () -> b.crossover(null));
    }

    @Test
    public void invalidMutationOperator() {
        assertThrows(NullPointerException.class, () -> b.mutation(null));
    }

    @Test
    public void invalidFitnessFunction() {
        assertThrows(NullPointerException.class, () -> b.fitness(null));
    }

    @Test
    public void invalidSerializer() {
        assertThrows(NullPointerException.class, () -> b.serializer(null));
    }

    @Test
    public void fluentSyntax() {
        b.survivalRate(0.5)
                .populationSize(100)
                .creation(() -> null)
                .crossover((a, b) -> b)
                .crossoverRate(0.6)
                .mutationRate(0.2)
                .fitness(d -> 0.0)
                .mutation(d -> d)
                .serializer(x -> "example")
                .build();
    }
}
