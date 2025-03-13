# Handling complex priors

While the basic Codephy specification handles simple prior distributions effectively, many phylogenetic analyses require more sophisticated prior structures. 
This section outlines approaches for representing hierarchical priors, mixture distributions, and joint priors within the Codephy framework.

## Beyond simple priors

Modern Bayesian phylogenetic analyses often incorporate advanced prior structures that go beyond independent distributions for each parameter. 
These more complex priors can:

- Capture dependencies between parameters
- Model hierarchical relationships
- Represent mixtures of different distributions
- Incorporate prior information from previous studies
- Apply constraints to the parameter space
- Handle correlated parameters

Codephy provides several mechanisms to represent these more sophisticated prior structures while maintaining its clean separation of random variables and deterministic functions.

## Hierarchical priors

Hierarchical models are a powerful tool in Bayesian phylogenetics, allowing parameters to be drawn from distributions whose hyperparameters are themselves uncertain. 
In Codephy, we represent hierarchical priors by defining random variables for the hyperparameters and referencing them from child distributions:

```json
{
  "randomVariables": {
    "popSizeMean": {
      "distribution": {
        "type": "LogNormal",
        "generates": "Real",
        "parameters": {
          "meanlog": 0.0,
          "sdlog": 1.0
        }
      }
    },
    "popSizeCV": {
      "distribution": {
        "type": "LogNormal",
        "generates": "Real",
        "parameters": {
          "meanlog": -1.0,
          "sdlog": 0.5
        }
      }
    },
    "popSize": {
      "distribution": {
        "type": "LogNormal",
        "generates": "Real",
        "parameters": {
          "meanlog": {
            "variable": "popSizeMean"
          },
          "sdlog": {
            "expression": "popSizeMean * popSizeCV"
          }
        }
      }
    }
  }
}
```

This example introduces two new concepts:

- **Variable references**: Direct references to other random variables using the `variable` field
- **Expressions**: Mathematical expressions combining multiple variables using the `expression` field

In this hierarchical model:
1. `popSizeMean` is drawn from a log-normal distribution with fixed hyperparameters
2. `popSizeCV` is the coefficient of variation, also drawn from a log-normal distribution
3. `popSize` is then drawn from a log-normal distribution where the parameters are determined by the hyperparameters

### Using variable references

The `variable` field creates a direct reference to another random variable:

```json
"meanlog": {
  "variable": "popSizeMean"
}
```

This indicates that the value of `meanlog` should be taken from the `popSizeMean` random variable. 
Inference engines should evaluate the referenced variable and use its value for the parameter.

### Using expressions

The `expression` field allows for mathematical combinations of variables:

```json
"sdlog": {
  "expression": "popSizeMean * popSizeCV"
}
```

This indicates that the value of `sdlog` should be calculated by multiplying the values of `popSizeMean` and `popSizeCV`. 
Expressions are limited to basic arithmetic operations and common mathematical functions to maintain portability across inference engines.

Standard operations and functions that should be supported include:
- Basic arithmetic: `+`, `-`, `*`, `/`, `^` (power)
- Mathematical functions: `log`, `exp`, `sqrt`, `abs`
- Trigonometric functions: `sin`, `cos`, `tan`
- Statistical functions: `min`, `max`, `mean`, `median`

For more complex transformations, deterministic functions should be used.

## Mixture distributions

Mixture models are frequently used in phylogenetics to represent heterogeneity, such as different substitution rates across sites. 
Codephy supports mixture distributions through a dedicated `Mixture` type:

```json
{
  "randomVariables": {
    "gammaShape": {
      "distribution": {
        "type": "Exponential",
        "generates": "Real",
        "parameters": {
          "rate": 1.0
        }
      }
    },
    "siteRates": {
      "distribution": {
        "type": "Mixture",
        "generates": "RealVector",
        "parameters": {
          "components": [
            {
              "distribution": "Gamma",
              "shape": {
                "variable": "gammaShape"
              },
              "rate": {
                "variable": "gammaShape"
              },
              "discretization": {
                "type": "quantile",
                "categories": 4
              }
            }
          ],
          "weights": [1.0]
        }
      }
    }
  }
}
```

The example above represents a discrete gamma distribution of rates across sites, commonly used in phylogenetic models. The `Mixture` distribution has these key components:

- **generates**: The type of values produced by the mixture
- **components**: An array of component distributions that make up the mixture
- **weights**: The relative weights of each component (must sum to 1.0)

### Discretization

For continuous distributions that need to be discretized (like the gamma distribution for site rates), the `discretization` field specifies how to create discrete categories:

```json
"discretization": {
  "type": "quantile",
  "categories": 4
}
```

Different discretization methods are available:
- **quantile**: Equal probability categories based on quantiles of the distribution
- **mean**: Categories based on the mean of equal probability portions
- **custom**: User-specified discrete values

### Multiple components

For more complex mixtures, multiple components with different weights can be specified:

```json
"distribution": {
  "type": "Mixture",
  "generates": "RealVector",
  "parameters": {
    "components": [
      {
        "distribution": "Gamma",
        "shape": 0.5,
        "rate": 0.5,
        "discretization": {
          "type": "quantile",
          "categories": 4
        }
      },
      {
        "distribution": "LogNormal",
        "meanlog": 0.0,
        "sdlog": 1.0,
        "discretization": {
          "type": "quantile",
          "categories": 4
        }
      }
    ],
    "weights": [0.7, 0.3]
  }
}
```

This represents a mixture of a gamma distribution (with 70% weight) and a log-normal distribution (with 30% weight), each discretized into 4 categories.

## Joint priors

In some cases, parameters have joint prior distributions that cannot be factored into independent components. 
Codephy handles these through multivariate distributions and constraints:

```json
{
  "randomVariables": {
    "birthDeathParams": {
      "distribution": {
        "type": "MultivariateNormal",
        "generates": "RealVector",
        "parameters": {
          "mean": [0.1, 0.05],
          "covariance": [
            [0.01, 0.005],
            [0.005, 0.01]
          ]
        }
      }
    }
  },
  "deterministicFunctions": {
    "birthRate": {
      "function": "vectorElement",
      "arguments": {
        "vector": { "variable": "birthDeathParams" },
        "index": 0
      }
    },
    "deathRate": {
      "function": "vectorElement",
      "arguments": {
        "vector": { "variable": "birthDeathParams" },
        "index": 1
      }
    }
  },
  "constraints": [
    {
      "type": "LessThan",
      "left": "deathRate",
      "right": "birthRate"
    }
  ]
}
```

This example introduces several features for handling joint priors:

1. A multivariate normal distribution for correlated parameters
2. Deterministic functions to extract individual parameters
3. A constraint to ensure valid parameter relationships

### Multivariate distributions

Multivariate distributions like `MultivariateNormal` capture correlations between parameters:

```json
"distribution": {
  "type": "MultivariateNormal",
  "generates": "RealVector",
  "parameters": {
    "mean": [0.1, 0.05],
    "covariance": [
      [0.01, 0.005],
      [0.005, 0.01]
    ]
  }
}
```

The parameters are:
- **mean**: Vector of mean values for each dimension
- **covariance**: Covariance matrix specifying correlations between dimensions

Other multivariate distributions that could be supported include:
- **Dirichlet**: For compositional data (values that sum to 1)
- **MultivariateBeta**: For multiple correlated parameters in (0,1)
- **WishartInverse**: For covariance matrices

### Vector extraction

To use individual elements of a multivariate distribution, deterministic functions extract specific components:

```json
"birthRate": {
  "function": "vectorElement",
  "arguments": {
    "vector": { "variable": "birthDeathParams" },
    "index": 0
  }
}
```

This extracts the first element (index 0) from the `birthDeathParams` vector for use elsewhere in the model.

### Constraints section

The `constraints` section specifies relationships that must hold between parameters:

```json
"constraints": [
  {
    "type": "LessThan",
    "left": "deathRate",
    "right": "birthRate"
  }
]
```

This constraint requires that `deathRate` is less than `birthRate`, which is a common requirement in birth-death process models.

Available constraint types include:
- **LessThan**: Left value must be less than right value
- **GreaterThan**: Left value must be greater than right value
- **Equals**: Left value must equal right value
- **Bounded**: Value must be within specified bounds
- **SumTo**: A set of values must sum to a specified total

## Informative priors from previous studies

A common scenario in Bayesian phylogenetics is to use posterior distributions from previous studies as informative priors. Codephy supports this through approximations of posterior distributions:

```json
{
  "randomVariables": {
    "substitutionRate": {
      "distribution": {
        "type": "PosteriorApproximation",
        "generates": "Real",
        "parameters": {
          "source": {
            "doi": "10.1093/sysbio/example",
            "parameter": "clockRate"
          },
          "approximation": {
            "type": "LogNormal",
            "meanlog": -5.2,
            "sdlog": 0.3
          }
        }
      }
    }
  }
}
```

The `PosteriorApproximation` type includes:
- **source**: Metadata about the source of the posterior (e.g., DOI and parameter name)
- **approximation**: A standard distribution that approximates the shape of the posterior

This maintains computational tractability while documenting the informative prior's provenance. 
The approximation can be any standard distribution that best captures the shape of the posterior distribution from the previous study.

## Priors on trees and topology

Beyond numerical parameters, phylogenetic models often require sophisticated priors on tree topology and branch lengths. 
Codephy extends the basic formats to handle these:

```json
{
  "randomVariables": {
    "treeHeightPrior": {
      "distribution": {
        "type": "LogNormal",
        "generates": "Real",
        "parameters": {
          "meanlog": 4.0,
          "sdlog": 0.5
        }
      }
    },
    "tree": {
      "distribution": {
        "type": "ConstrainedYule",
        "generates": "Tree",
        "parameters": {
          "birthRate": { "variable": "birthRateParam" },
          "rootHeight": {
            "variable": "treeHeightPrior"
          },
          "constraints": {
            "topology": {
              "type": "monophyly",
              "taxonSet": ["Homo", "Pan", "Gorilla"],
              "clade": "Hominidae"
            }
          }
        }
      }
    }
  }
}
```

This example demonstrates several advanced features for tree priors:

1. A separate prior on tree height
2. A reference to this prior in the tree distribution
3. Topological constraints on the tree

### Tree height priors

By separating the tree height prior (`treeHeightPrior`) from the tree process itself, we can place informative priors on this important parameter while still using a standard tree prior like Yule or birth-death.

### Constrained tree processes

The `ConstrainedYule` distribution extends the basic Yule process with additional parameters and constraints:

```json
"distribution": {
  "type": "ConstrainedYule",
  "generates": "Tree",
  "parameters": {
    "birthRate": { "variable": "birthRateParam" },
    "rootHeight": {
      "variable": "treeHeightPrior"
    },
    "constraints": {
      "topology": {
        "type": "monophyly",
        "taxonSet": ["Homo", "Pan", "Gorilla"],
        "clade": "Hominidae"
      }
    }
  }
}
```

This specifies a Yule process with:
- A birth rate parameter
- A root height determined by the `treeHeightPrior` variable
- A constraint enforcing monophyly of a specific group of taxa

### Topology constraints

Topology constraints can take several forms:

- **monophyly**: A specified set of taxa must form a monophyletic group
- **fixed**: The topology is fixed to a specified structure (e.g., from a previous analysis)
- **calibrated**: Specific clades have age constraints based on fossil evidence

For fixed topologies, a Newick string representation can be provided:

```json
"constraints": {
  "topology": {
    "type": "fixed",
    "newick": "((Homo,Pan),Gorilla);"
  }
}
```

For calibrated clades, age constraints can be specified:

```json
"constraints": {
  "calibrations": [
    {
      "taxonSet": ["Homo", "Pan"],
      "age": {
        "distribution": {
          "type": "LogNormal",
          "parameters": {
            "meanlog": 2.0,
            "sdlog": 0.5
          }
        }
      }
    }
  ]
}
```

## Implementation considerations

Complex priors introduce several challenges for inference engines implementing the Codephy format:

### Expressions

Expressions should be parsed and evaluated safely, with a well-defined set of allowed operations. 
Inference engines might:

- Use a simple expression parser with defined operators and functions
- Implement a restricted subset of the host language's evaluation capabilities
- Translate expressions to compiled code for efficiency

For example, a simple expression evaluator might work like this:

```javascript
function evaluateExpression(expr, variables) {
  // Replace variable references with their values
  for (const [name, value] of Object.entries(variables)) {
    expr = expr.replaceAll(name, value);
  }
  
  // Use a safe evaluation method (this is simplified)
  // In practice, you'd want a proper expression parser
  return Function('"use strict"; return (' + expr + ')')();
}
```

### Constraints

Constraints require specialized MCMC proposals or optimizers that respect the constrained parameter space. 
Implementations might:

- Use rejection sampling to discard proposals that violate constraints
- Implement constrained proposal distributions that only generate valid values
- Transform the parameter space to implicitly satisfy constraints

### Posterior approximations

Posterior approximations should be documented clearly to ensure reproducibility. 
Inference engines might:

- Include the source information in output files
- Warn when posterior approximations are used
- Provide tools to validate the appropriateness of the approximation

### Mixture components

Mixture components may require special handling depending on the inference method. 
Implementations might:

- Precalculate discretized values for efficiency
- Track mixture component assignments as latent variables
- Implement specialized MCMC moves for mixture models

## IID samples from standard distributions

For independent and identically distributed (IID) samples from standard distributions, Codephy now supports a direct approach using the `dimension` parameter with standard distributions. For example:

```json
"branchRates": {
  "distribution": {
    "type": "LogNormal",
    "generates": "RealVector",
    "parameters": {
      "meanlog": -7.0,
      "sdlog": 1.0,
      "dimension": 10
    }
  }
}
```

This generates a vector of 10 independent samples from the same LogNormal distribution, which is more concise than defining a mixture distribution when no correlation between the values is needed.

## Recommended practices

When using complex priors in Codephy models, consider these best practices:

### Document your choices

Include detailed metadata explaining why particular prior structures were chosen and what they represent. 
This is especially important for hierarchical and informative priors.

### Start simple

Begin with simple priors and add complexity gradually, validating the model at each step. 
This makes it easier to identify issues.

### Check compatibility

Ensure that the inference engine you plan to use supports all the complex prior features you're using. 
Some engines may have limited support for certain features.

### Validate domain knowledge

Complex priors often encode domain knowledge about parameters. 
Validate these assumptions with subject matter experts and relevant literature.

### Consider identifiability

Be cautious with highly parameterized hierarchical models, as they may lead to identifiability issues. 
Ensure that all parameters are identifiable given the data.

## Summary

Codephy's flexible approach to prior specification allows it to represent a wide range of complex prior structures commonly used in Bayesian phylogenetics. 
By incorporating features for hierarchical priors, mixtures, joint distributions, and constraints, Codephy enables sophisticated modeling while maintaining a clean, portable format.

These advanced features extend the basic Codephy model while preserving its fundamental separation between random variables and deterministic functions. 
Inference engines implementing Codephy can choose which complex prior features to support, with a clear path for extending support as needed.