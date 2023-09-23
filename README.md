# Genetic Algorithm Library

## Features
- Easy parametrization with Builders
- A Serial implementation for single-thread environments
- A Parallel implementation for high performance

## Requirements
To build:
- gradle
- java >= 17

## Hints for usage/performance
### 1) Cache `hashCode`
Both implementations use a `HashMap` to cache the scores of all solutions during execution. You should provide use (as your solutions) classes which can cache their hashCode to speed up the whole process. Take a look at examples `Knapsack` and `NeuralNetwork`. As you can see [here](https://godbolt.org/z/e76aPoro4), record classes work fine as wrappers and can inline their hashCode everywhere but be sure to check if the hashCode changes when one of the internal fields/values changes. Otherwise, you should consider using functions which return entirely new instances every time, as mutation and crossover operators.
### 2) `setState` and check
For a better experience, it is recommended to create a Supplier of small/quick configurations to feed into the same algorithm instance by:
1. `setState`
2. `run`
3. `getState` to check the results
4. Return to step 1 if the results are not "good enough"

## Future ideas
- Genealogic tree of solutions
