/*
 * minimization-algorithms-library - A collection of minimization algorithms.
 * Copyright (C) 2023-2026 Filippo Barbari <filippo.barbari@gmail.com>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ledmington.mal.genetic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class GAConfigTest {

	private GeneticAlgorithmConfig.GeneticAlgorithmConfigBuilder<String> b;

	@BeforeEach
	void setup() {
		b = GeneticAlgorithmConfig.builder();
	}

	@Test
	void cannotBuildWithNoParameters() {
		assertThrows(Exception.class, b::build);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 1})
	void invalidPopulationSize(final int pop) {
		assertThrows(IllegalArgumentException.class, () -> b.populationSize(pop));
	}

	@ParameterizedTest
	@ValueSource(ints = {-2, -1})
	void invalidMaxGenerations(final int gen) {
		assertThrows(IllegalArgumentException.class, () -> b.maxGenerations(gen));
	}

	@Test
	void invalidStopCriterion() {
		assertThrows(NullPointerException.class, () -> b.stopCriterion(null));
	}

	@ParameterizedTest
	@ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
	void invalidSurvivalRate(final double sr) {
		assertThrows(IllegalArgumentException.class, () -> b.survivalRate(sr));
	}

	@ParameterizedTest
	@ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
	void invalidCrossoverRate(final double cr) {
		assertThrows(IllegalArgumentException.class, () -> b.crossoverRate(cr));
	}

	@ParameterizedTest
	@ValueSource(doubles = {-0.1, 0.0, 1.0, 1.1})
	void invalidMutationRate(final double mr) {
		assertThrows(IllegalArgumentException.class, () -> b.mutationRate(mr));
	}

	@Test
	void invalidIndividualSupplier() {
		assertThrows(NullPointerException.class, () -> b.creation(null));
	}

	@Test
	void invalidCrossoverOperator() {
		assertThrows(NullPointerException.class, () -> b.crossover(null));
	}

	@Test
	void invalidMutationOperator() {
		assertThrows(NullPointerException.class, () -> b.mutation(null));
	}

	@Test
	void invalidMaximizeFunction() {
		assertThrows(NullPointerException.class, () -> b.maximize(null));
	}

	@Test
	void invalidMinimizeFunction() {
		assertThrows(NullPointerException.class, () -> b.minimize(null));
	}

	@Test
	void noTerminationCriterionIsAllowed() {
		assertDoesNotThrow(() -> b.survivalRate(0.5)
				.populationSize(100)
				.creation(() -> null)
				.crossover((_, b) -> b)
				.crossoverRate(0.6)
				.mutationRate(0.2)
				.maximize(_ -> 0.0)
				.mutation(d -> d)
				.build());
	}

	@Test
	void noNullsFirstGeneration() {
		assertThrows(NullPointerException.class, () -> b.firstGeneration(null, ""));
		assertThrows(NullPointerException.class, () -> b.firstGeneration("", null));
		assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", null));
		assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", "c", null));
		assertThrows(NullPointerException.class, () -> b.firstGeneration("a", "b", "c", "d", null));
	}

	@Test
	void invalidFirstGeneration() {
		b.populationSize(2)
				.firstGeneration("a", "b", "c")
				.creation(() -> null)
				.crossover((_, b) -> b)
				.mutation(d -> d)
				.maximize(_ -> 0.0);
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test
	void invalidFirstGenerationSet() {
		b.populationSize(2)
				.firstGeneration(Set.of("a", "b", "c"))
				.creation(() -> null)
				.crossover((_, b) -> b)
				.mutation(d -> d)
				.maximize(_ -> 0.0);
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test
	void fluentSyntax() {
		b.survivalRate(0.5)
				.populationSize(100)
				.maxGenerations(100)
				.creation(() -> null)
				.crossover((_, b) -> b)
				.crossoverRate(0.6)
				.mutationRate(0.2)
				.maximize(_ -> 0.0)
				.mutation(d -> d)
				.build();
	}

	@Test
	void cannotBuildTwoTimes() {
		b.survivalRate(0.5)
				.populationSize(100)
				.maxGenerations(100)
				.creation(() -> null)
				.crossover((_, b) -> b)
				.crossoverRate(0.6)
				.mutationRate(0.2)
				.maximize(_ -> 0.0)
				.mutation(d -> d);
		b.build();
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test
	void fieldsGetSet() {
		final GeneticAlgorithmConfig<String> c = b.survivalRate(0.5)
				.populationSize(100)
				.maxGenerations(100)
				.creation(() -> null)
				.crossover((_, b) -> b)
				.crossoverRate(0.6)
				.mutationRate(0.2)
				.maximize(_ -> 0.0)
				.mutation(d -> d)
				.build();

		assertEquals(100, c.populationSize());
		assertEquals(0.6, c.crossoverRate());
		assertEquals(0.2, c.mutationRate());
	}
}
