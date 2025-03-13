# Inference interpretation

## Conditioning on observed data

A core aspect of Bayesian phylogenetic inference is the distinction between parameters (to be inferred) and data (observed). Codephy makes this distinction explicit through the use of the `observedValue` field on random variables.

When a random variable has an `observedValue`, it indicates that this variable is considered "data" and is conditioned upon in the inference process. Variables without `observedValue` are considered parameters that need to be inferred.

For example, in this model fragment:

```json
"alignment": {
  "distribution": {
    "type": "PhyloCTMC",
    "generates": "Alignment",
    "parameters": {
      "tree": { "variable": "tree" },
      "Q": { "variable": "substitutionModel" }
    }
  },
  "observedValue": [
    {"taxon": "TaxonA", "sequence": "ACGT"},
    {"taxon": "TaxonB", "sequence": "ACGA"}
  ]
},
"tree": {
  "distribution": {
    "type": "Yule",
    "generates": "Tree",
    "parameters": {
      "birthRate": { "variable": "birthRateParam" }
    }
  }
}
```

The `alignment` variable has an `observedValue` field containing sequence data, so it's treated as observed data. The `tree` variable doesn't have an `observedValue`, so it's a parameter to be inferred.

## Latent parameters

Random variables without an `observedValue` are considered latent parameters. These are the quantities that will be estimated during the inference process. Depending on the inference method used, we might be interested in:

- Point estimates (e.g., maximum likelihood values)
- Posterior distributions (for Bayesian inference)
- Confidence or credible intervals

Latent parameters include:
- Numerical parameters like substitution model parameters
- Topological parameters like the phylogenetic tree
- Branch lengths
- Population size parameters
- Rate variation parameters

## Deterministic transformations

Deterministic functions in Codephy represent fixed transformations of other variables. From an inference perspective, they are not directly inferred but are calculated deterministically from their inputs.

For example, if we have:

```json
"substitutionModel": {
  "function": "HKY",
  "arguments": {
    "kappa": { "variable": "kappaParam" },
    "baseFrequencies": { "variable": "baseFreqParam" }
  }
}
```

The `substitutionModel` itself is not a parameter to be estimated. Instead, the parameters `kappaParam` and `baseFreqParam` are estimated, and the substitution model is constructed from them according to the HKY formula.

This distinction is important for inference engines, as it affects how the likelihood is calculated and what parameters are included in the posterior distribution.

## Inference targets

When interpreting a Codephy model for inference, the engine should:

1. Identify all random variables without `observedValue` as parameters to be inferred
2. Use random variables with `observedValue` as fixed data in the likelihood calculation
3. Apply deterministic functions to transform parameters as needed
4. Apply any constraints specified in the model
5. Compute the posterior distribution or maximum likelihood estimates for the parameters

The primary target of inference is typically the tree topology and branch lengths, along with any associated parameters like substitution model parameters or population sizes.

## Likelihood calculation

The likelihood function for a phylogenetic model represents the probability of observing the data given the parameters. In Codephy, this corresponds to:

P(observed variables | latent variables, deterministic functions)

For a typical phylogenetic analysis with sequence data, this is:

P(alignment | tree, substitution model, other parameters)

Inference engines implementing Codephy should use the model specification to construct the appropriate likelihood function based on the distributions and relationships specified in the model.

## Prior distributions

Random variables in Codephy specify their prior distributions through the `distribution` field. These priors are an essential part of Bayesian inference and encode our beliefs about the parameters before observing the data.

For example:

```json
"kappaParam": {
  "distribution": {
    "type": "LogNormal",
    "generates": "Real",
    "parameters": {
      "meanlog": 1.0,
      "sdlog": 0.5
    }
  }
}
```

This specifies that the `kappaParam` has a log-normal prior with mean log of 1.0 and standard deviation log of 0.5. Inference engines should use these prior specifications in their Bayesian inference procedures.

## Constraints

Constraints in Codephy represent hard restrictions on the parameter space. From an inference perspective, these modify the prior distribution by assigning zero probability to parameter combinations that violate the constraints.

For example:

```json
"constraints": [
  {
    "type": "LessThan",
    "left": "deathRateParam",
    "right": "birthRateParam"
  }
]
```

This constraint requires that `deathRateParam` is less than `birthRateParam`. In inference, this means that parameter combinations where `deathRateParam` ≥ `birthRateParam` should be assigned zero prior probability.

## MCMC implementation considerations

For engines implementing Markov Chain Monte Carlo (MCMC) inference, several aspects of the Codephy model are particularly relevant:

### Proposal distributions

While Codephy doesn't specify how parameters should be sampled during MCMC, inference engines need to implement appropriate proposal distributions for each parameter type. These might include:

- Scale proposals for positive real parameters
- Sliding window proposals for parameters with bounds
- Tree proposals for topological changes
- Branch length proposals

### Parameter dependencies

The dependency structure in a Codephy model (determined by references between variables) informs which parts of the likelihood need to be recalculated when a parameter changes. 
This is crucial for efficient MCMC implementation.

For example, if `kappaParam` changes, only the substitution model and its downstream effects need to be recalculated, not the entire model.

### Initialization

Inference engines should initialize parameters based on their prior distributions unless specific initial values are provided. 
For some parameters (like trees), heuristic initialization methods might be more appropriate.

## Maximum likelihood considerations

For maximum likelihood inference, the prior distributions in Codephy can be ignored (or treated as defining the parameter bounds). 
The focus is solely on finding the parameter values that maximize the likelihood of the observed data.

However, constraints should still be respected, as they define the valid parameter space for the optimization.

## Model selection

Codephy models can be used for model selection through methods like:

- Bayes factors (for Bayesian inference)
- Likelihood ratio tests (for nested models)
- Information criteria like AIC, BIC, or DIC

The consistent structure of Codephy models makes it easier to compare different models and implement model selection procedures.

## Output formats

While Codephy itself doesn't specify output formats for inference results, engines implementing Codephy should consider standardized outputs that include:

1. Parameter estimates (point estimates and/or posterior distributions)
2. Tree estimates (consensus trees, maximum likelihood trees, etc.)
3. Uncertainty measures (credible intervals, bootstrap values, etc.)
4. Model fit statistics
5. References back to the original Codephy model

## Inference engine responsibilities

Inference engines that implement Codephy should:

1. Correctly interpret the model structure according to the rules above
2. Validate the model before inference (e.g., check for cycles, missing references)
3. Implement efficient inference algorithms appropriate for the model
4. Provide clear error messages for invalid models or inference failures
5. Document which model features they support
6. Return results in a format that allows for further analysis

## Summary

The inference interpretation of a Codephy model is centered around the distinction between observed data and latent parameters, with deterministic functions providing transformations of parameters. 
The prior distributions, constraints, and dependency structure in the model guide how inference should be performed.

By separating model specification from inference details, Codephy allows researchers to focus on model design while enabling inference engines to implement efficient algorithms appropriate for each model type.