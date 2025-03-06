/*
* minimization-algorithms-library - A collection of minimization algorithms.
* Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.mal.examples;

import java.util.Map;
import java.util.Optional;

import com.ledmington.mal.examples.genetic.Diet;
import com.ledmington.mal.examples.genetic.Knapsack;
import com.ledmington.mal.examples.genetic.NeuralNetwork;
import com.ledmington.mal.examples.genetic.Tsp;

public final class Main {

	private static final Map<String, Map<String, Runnable>> examples = Map.of(
			"genetic",
			Map.of(
					"tsp",
					Tsp::new,
					"random_strings",
					com.ledmington.mal.examples.genetic.RandomStrings::new,
					"knapsack",
					Knapsack::new,
					"neural_network",
					NeuralNetwork::new,
					"diet",
					Diet::new),
			"simulated_annealing",
			Map.of("rosenbrock", com.ledmington.mal.examples.annealing.Rosenbrock::new),
			"pattern_search",
			Map.of(
					"random_strings",
					com.ledmington.mal.examples.patternsearch.RandomStrings::new,
					"rosenbrock",
					com.ledmington.mal.examples.patternsearch.Rosenbrock::new));

	private static void printAvailableAlgorithms() {
		System.out.println("These are the available algorithms:");
		examples.keySet().stream().sorted().forEach(k -> System.out.printf(" - %s%n", k));
		System.out.println();
	}

	private static void printAvailableAlgorithmsAndExamples() {
		System.out.println("These are the available examples divided by algorithms:");
		for (final Map.Entry<String, Map<String, Runnable>> e : examples.entrySet()) {
			System.out.printf(" - %s:%n", e.getKey());
			for (final String ex : e.getValue().keySet()) {
				System.out.printf("   - %s%n", ex);
			}
		}
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			printAvailableAlgorithmsAndExamples();
			System.out.println(
					"\nRerun the program like this: java -jar examples.jar simulated_annealing random_strings");
			System.exit(1);
			return;
		}

		final String algorithm = args[0];
		final String example = args[1];

		final Optional<String> chosenAlgorithm = examples.keySet().stream()
				.filter(k -> k.equalsIgnoreCase(algorithm))
				.findFirst();
		if (chosenAlgorithm.isEmpty()) {
			System.out.printf("The algorithm '%s' does not exist.%n", algorithm);
			printAvailableAlgorithms();
			System.exit(1);
			return;
		}

		final String alg = chosenAlgorithm.orElseThrow();
		final Optional<String> chosenExample = examples.get(alg).keySet().stream()
				.filter(k -> k.equalsIgnoreCase(example))
				.findFirst();
		if (chosenExample.isEmpty()) {
			System.out.printf("The example '%s' does not exist.%n", example);
			printAvailableAlgorithmsAndExamples();
			System.exit(1);
			return;
		}

		examples.get(alg).get(chosenExample.orElseThrow()).run();

		// needed for automatic termination of the executors
		System.exit(0);
	}
}
