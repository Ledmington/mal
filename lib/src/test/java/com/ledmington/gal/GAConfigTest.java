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
package com.ledmington.gal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public final class GAConfigTest {

    private GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<String> b;

    @BeforeEach
    public void setup() {
        b = GeneticAlgorithmConfig.builder();
    }

    @Test
    public void cannotBuildWithNoParameters() {
        assertThrows(Exception.class, () -> b.build());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    public void invalidPopulationSize(int pop) {
        assertThrows(IllegalArgumentException.class, () -> b.populationSize(pop));
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1})
    public void invalidMaxGenerations(int gen) {
        assertThrows(IllegalArgumentException.class, () -> b.maxGenerations(gen));
    }

    @Test
    public void invalidStopCriterion() {
        assertThrows(NullPointerException.class, () -> b.stopCriterion(null));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
    public void invalidSurvivalRate(double sr) {
        assertThrows(IllegalArgumentException.class, () -> b.survivalRate(sr));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
    public void invalidCrossoverRate(double cr) {
        assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(cr));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
    public void invalidMutationRate(double mr) {
        assertThrows(IllegalArgumentException.class, () -> b.mutationRate(mr));
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
    public void invalidMaximizeFunction() {
        assertThrows(NullPointerException.class, () -> b.maximize(null));
    }

    @Test
    public void invalidMinimizeFunction() {
        assertThrows(NullPointerException.class, () -> b.minimize(null));
    }

    @Test
    public void noTerminationCriterionIsAllowed() {
        b.survivalRate(0.5)
                .populationSize(100)
                .creation(() -> null)
                .crossover((a, b) -> b)
                .crossoverRate(0.6)
                .mutationRate(0.2)
                .maximize(d -> 0.0)
                .mutation(d -> d)
                .build();
    }

    @Test
    public void noNullsFirstGeneration() {
        assertThrows(NullPointerException.class, () -> b.firstGeneration(null, ""));
        assertThrows(NullPointerException.class, () -> b.firstGeneration("", null));
        assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", null));
        assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", "c", null));
        assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", "c", "d", null));
    }

    @Test
    public void invalidFirstGeneration() {
        b.populationSize(2)
                .firstGeneration("a", "b", "c")
                .creation(() -> null)
                .crossover((a, b) -> b)
                .mutation(d -> d)
                .maximize(s -> 0.0);
        assertThrows(IllegalArgumentException.class, () -> b.build());
    }

    @Test
    public void fluentSyntax() {
        b.survivalRate(0.5)
                .populationSize(100)
                .maxGenerations(100)
                .creation(() -> null)
                .crossover((a, b) -> b)
                .crossoverRate(0.6)
                .mutationRate(0.2)
                .maximize(d -> 0.0)
                .mutation(d -> d)
                .build();
    }

    @Test
    public void cannotBuildTwoTimes() {
        b.survivalRate(0.5)
                .populationSize(100)
                .maxGenerations(100)
                .creation(() -> null)
                .crossover((a, b) -> b)
                .crossoverRate(0.6)
                .mutationRate(0.2)
                .maximize(d -> 0.0)
                .mutation(d -> d);
        b.build();
        assertThrows(IllegalStateException.class, () -> b.build());
    }

    @Test
    public void fieldsGetSet() {
        final GeneticAlgorithmConfig<String> c = b.survivalRate(0.5)
                .populationSize(100)
                .maxGenerations(100)
                .creation(() -> null)
                .crossover((a, b) -> b)
                .crossoverRate(0.6)
                .mutationRate(0.2)
                .maximize(d -> 0.0)
                .mutation(d -> d)
                .build();

        assertEquals(100, c.populationSize());
        assertEquals(0.6, c.crossoverRate());
        assertEquals(0.2, c.mutationRate());
    }
}
