# Genetic Algorithm Library

## Features
- Easy parametrization with Builders
- A Serial implementation for single-thread environments
- A Parallel implementation for high performance

## TO DO
- Refactoring of `GeneticAlgorithmState`: make it a small record class to be created on-the-fly when `getState()` is called and add something equivalent hidden in the implementation
- Improvement of `GeneticAlgorithm` interface: make it return best of last generation, make it extend `Iterator<X>`, add method `runUntil(ExitCondition)`

## Future ideas
- Genealogic tree of solutions
