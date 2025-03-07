# Codephy to BEAST2 Mapper

This module provides a mapper from Codephy JSON model specifications to BEAST2 Java objects. It demonstrates how Codephy models can be directly mapped to the internal object model of existing phylogenetic inference frameworks.

## Overview

The mapper creates a complete BEAST2 model structure from a Codephy JSON specification:

1. Creates BEAST2 objects for all random variables and deterministic functions
2. Resolves references between components
3. Builds the full model including posterior, likelihood, and priors
4. Sets up MCMC with appropriate operators and loggers

## Requirements

- BEAST2 v2.7.x
- Jackson for JSON parsing

## Installation

Include this module in your project by adding the following to your pom.xml:

```xml
<dependency>
    <groupId>org.codephy</groupId>
    <artifactId>codephy-mapper-beast2</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Usage

```java
// Create the mapper
CodephyToBEAST2Mapper mapper = new CodephyToBEAST2Mapper();

// Convert the Codephy model to BEAST2 objects
mapper.convertToBEAST2Objects("path/to/codephy-model.json");

// Get the root BEAST2 object (MCMC)
BEASTInterface beast2Model = mapper.getPosterior();

// Optionally export to XML
mapper.exportToXML("path/to/output.xml");
```

See `MapperExample.java` for a complete example.

## Supported Features

### Random Variables / Distributions
- Scalar distributions: LogNormal, Normal, Gamma, Beta, Exponential, Uniform
- Vector distributions: Dirichlet, MultivariateNormal
- Tree distributions: Yule, BirthDeath, Coalescent, ConstrainedYule
- Sequence evolution: PhyloCTMC

### Deterministic Functions
- Substitution models: HKY, JC69, GTR
- Vector operations: normalize, vectorElement

### Vector-valued Parameters
The mapper supports the new schema enhancement for vector outputs with dimension parameters.

## Limitations

- Complex expressions for dimension parameters are simplified
- Limited support for calibrated trees
- Advanced mixture models need additional implementation
- BEAST2-specific features like operator tuning not exposed

## Contributing

Contributions are welcome! Here are some areas that could use improvement:

1. Support for more distribution types
2. Better handling of expressions in dimension parameters
3. More flexible MCMC configuration
4. Support for advanced BEAST2 features like path sampling

## License

This module is released under the same license as the main Codephy project.