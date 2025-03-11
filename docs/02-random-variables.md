# Random variables and deterministic functions

## Core concepts

The Codephy format is built around two fundamental concepts from probabilistic modeling:

1. **Random variables**: Variables drawn from probability distributions
2. **Deterministic functions**: Functions that transform one or more variables into a new value

This separation creates a clear distinction between the stochastic and deterministic components of a model, making it easier to reason about the model structure and its inference requirements.

## Random variables

In Codephy, a random variable is defined by its probability distribution. Each random variable has:

- A unique name (the key in the `randomVariables` object)
- A distribution specification (e.g., `LogNormal`, `Dirichlet`, `Yule`)
- Optional observed values (if the variable represents observed data)

Here's a simple example of a random variable representing a parameter drawn from a log-normal distribution:

```json
"kappaParam": {
  "distribution": {
    "type": "LogNormal",
    "generates": "REAL",
    "parameters": {
      "meanlog": 1.0,
      "sdlog": 0.5
    }
  }
}
```

This defines a random variable named `kappaParam` with a log-normal distribution that has parameters `meanlog = 1.0` and `sdlog = 0.5`. The `generates` field indicates that this distribution produces a real-valued random variable.

Random variables can also represent more complex entities like phylogenetic trees:

```json
"tree": {
  "distribution": {
    "type": "Yule",
    "generates": "TREE",
    "parameters": {
      "birthRate": {
        "variable": "birthRateParam"
      }
    }
  }
}
```

In this case, `tree` is a random variable drawn from a Yule process, which is parameterized by another random variable named `birthRateParam`.

### Observed variables

A key feature of Codephy is the ability to designate random variables as observed by adding an `observedValue` field:

```json
"alignment": {
  "distribution": {
    "type": "PhyloCTMC",
    "generates": "ALIGNMENT",
    "parameters": {
      "tree": {
        "variable": "tree"
      },
      "Q": {
        "variable": "substitutionModel"
      }
    }
  },
  "observedValue": [
    {"taxon": "TaxonA", "sequence": "ACGT"},
    {"taxon": "TaxonB", "sequence": "ACGA"}
  ]
}
```

Here, `alignment` is a random variable drawn from a PhyloCTMC (Phylogenetic Continuous-Time Markov Chain) distribution, but we've observed its value—the sequence data that forms the basis of our phylogenetic inference.

The presence of `observedValue` indicates that the variable is conditioned on, rather than inferred during the analysis. This makes the distinction between data and parameters explicit in the model specification.

## Deterministic functions

Deterministic functions take one or more variables as input and produce a new value through a deterministic transformation. In Codephy, a deterministic function has:

- A unique name (the key in the `deterministicFunctions` object)
- A function type (e.g., `"hky"`, `"gtr"`, `"vectorElement"`)
- Arguments that reference other variables or values

Here's an example of a deterministic function representing an HKY substitution model:

```json
"substitutionModel": {
  "function": "hky",
  "arguments": {
    "kappa": {
      "variable": "kappaParam"
    },
    "baseFrequencies": {
      "variable": "baseFreqParam"
    }
  }
}
```

This defines a function named `substitutionModel` that applies the HKY (Hasegawa-Kishino-Yano) substitution model with parameters `kappaParam` and `baseFreqParam`, which are references to previously defined random variables.

Deterministic functions can also perform simpler operations like extracting elements from vectors:

```json
"birthRate": {
  "function": "vectorElement",
  "arguments": {
    "vector": {
      "variable": "birthDeathParams"
    },
    "index": 0
  }
}
```

This defines a function that extracts the first element (index 0) from the `birthDeathParams` vector, which might be a multivariate random variable.

## Parameter values

In Codephy, parameter values can be specified in several ways:

1. **Direct values**: Simple numeric or string values
   ```json
   "sdlog": 0.5
   ```

2. **References to variables**: References to other random variables
   ```json
   "birthRate": {
     "variable": "birthRateParam"
   }
   ```

3. **Mathematical expressions**: Formulas combining variables and constants
   ```json
   "rate": {
     "expression": "globalRate * localMultiplier"
   }
   ```

4. **Arrays**: Lists of values, which can themselves be any of these types
   ```json
   "alpha": [1.0, 1.0, 1.0, 1.0]
   ```

This flexibility allows for complex parameter relationships while maintaining a clear structure.

## Dependency structure

By separating random variables from deterministic functions, Codephy makes the dependency structure of the model explicit. This creates a directed graph where:

- Nodes are either random variables or deterministic functions
- Edges represent dependencies (e.g., a function depends on its argument variables)

This graph structure is important for several reasons:

1. It allows inference engines to determine the correct order of computation
2. It enables validation tools to check for issues like circular dependencies
3. It facilitates visualization of the model structure
4. It makes it easier to reason about the probabilistic relationships in the model

The dependency structure also helps in identifying which variables need to be inferred (latent variables) versus those that are observed or deterministic.

## Distribution types and parameters

The Codephy schema defines several standard distribution types, each with its own set of parameters:

| Distribution | Generates | Parameters | Description |
|--------------|-----------|------------|-------------|
| `LogNormal` | `REAL`, `REAL_VECTOR` | `meanlog`, `sdlog`, optional `dimension` | Log-normal distribution for continuous positive parameters |
| `Normal` | `REAL`, `REAL_VECTOR` | `mean`, `sd`, optional `dimension` | Normal distribution for real-valued parameters |
| `Gamma` | `REAL`, `REAL_VECTOR` | `shape`, `rate`, optional `dimension` | Gamma distribution for continuous positive parameters |
| `Beta` | `REAL`, `REAL_VECTOR` | `alpha`, `beta`, optional `dimension` | Beta distribution for parameters in (0,1) |
| `Exponential` | `REAL`, `REAL_VECTOR` | `rate`, optional `dimension` | Exponential distribution for rate parameters |
| `Uniform` | `REAL`, `REAL_VECTOR` | `lower`, `upper`, optional `dimension` | Uniform distribution for bounded parameters |
| `Dirichlet` | `REAL_VECTOR` | `alpha` (array) | Dirichlet distribution for vector parameters that sum to 1 |
| `MultivariateNormal` | `REAL_VECTOR` | `mean` (array), `covariance` (matrix) | Multivariate normal for correlated parameters |
| `Mixture` | Varies | `components`, `weights` | Mixture distribution for complex patterns |
| `PosteriorApproximation` | `REAL`, `REAL_VECTOR` | `source`, `approximation` | Using results from previous studies as priors |
| `Yule` | `TREE` | `birthRate` | Yule process for generating trees |
| `BirthDeath` | `TREE` | `birthRate`, `deathRate`, optional `rootHeight` | Birth-Death process for trees |
| `Coalescent` | `TREE` | `populationSize` | Coalescent process for trees |
| `ConstrainedYule` | `TREE` | `birthRate`, optional constraints | Yule with topological constraints |
| `PhyloCTMC` | `ALIGNMENT` | `tree`, `Q`, optional parameters | Continuous-time Markov chain for sequences |

When `generates` is `REAL_VECTOR`, many distributions accept a `dimension` parameter to specify the number of IID samples.

More complex distributions (like mixtures and hierarchical priors) are covered in the Complex Priors section of the documentation.

## Constraints

Codephy allows for explicit specification of constraints on parameters through the `constraints` section. These can include:

- `lessThan`: Ensures one parameter is less than another
- `greaterThan`: Ensures one parameter is greater than another
- `equals`: Enforces equality between parameters or with a constant
- `bounded`: Restricts a parameter to a specific range
- `sumTo`: Requires a set of parameters to sum to a target value

These constraints are important for many models in phylogenetics, such as ensuring rates sum to 1.0 or maintaining relative ordering of time points.

## JSON representation

In a complete Codephy model, random variables and deterministic functions are specified in separate top-level objects:

```json
{
  "randomVariables": {
    "var1": { ... },
    "var2": { ... },
    ...
  },
  "deterministicFunctions": {
    "func1": { ... },
    "func2": { ... },
    ...
  }
}
```

This structure emphasizes the conceptual distinction between these two types of model components while allowing them to reference each other as needed.

## Naming conventions

While Codephy doesn't enforce specific naming conventions, it's good practice to:

- Use descriptive names that indicate the role of the variable or function
- Use camelCase for multi-word names
- Include parameter types in names when applicable (e.g., `kappaParam`, `treeHeightPrior`)
- Use consistent naming patterns across related models

Well-chosen names make models more readable and help communicate their structure and purpose to others.

## Summary

The separation of random variables and deterministic functions is central to the Codephy format. This approach:

- Makes the probabilistic structure of the model explicit
- Clearly distinguishes between parameters, data, and transformations
- Creates a clean dependency graph for inference
- Enables validation of model correctness
- Facilitates mapping to different inference engine implementations

The schema provides a structured way to express complex phylogenetic models with proper parameter dependencies, constraints, and metadata.