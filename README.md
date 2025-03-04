# Codephy: A Portable format for specifying phylogenetic models

Codephy is a proposal for a JSON-based format for specifying probabilistic phylogenetic models in a way that's portable across multiple inference engines. 
It's aim is to foster interoperability and facilitate more efficient development of tools for constructing and manipulating phylogenetic models.

## Key features

- **Separation of concerns**: Clear distinction between random variables and deterministic functions
- **Explicit conditionality**: Variables can be observed (data) or latent (to be inferred)
- **Portable format**: Designed to be consumed by different inference engines
- **Comprehensive metadata**: Support for provenance, authorship, and citation information
- **Validation mechanisms**: Multi-layered approach to ensuring model correctness
- **Complex prior support**: Hierarchical models, mixtures, and joint distributions
- **Tooling Ecosystem**: Guidelines for interfaces that don't require writing raw JSON

## Documentation

| Section | Description |
|---------|-------------|
| [Introduction](docs/01-introduction.md) | Overview of Codephy and its goals |
| [Random variables and deterministic functions](docs/02-random-variables.md) | Core concepts of the Codephy format |
| [Example models](docs/03-example-models.md) | Concrete examples of phylogenetic models in Codephy |
| [Inference interpretation](docs/04-inference.md) | How to interpret Codephy models from an inference perspective |
| [Mapping to class hierarchies](docs/05-mapping.md) | Strategies for implementing Codephy in inference engines |
| [Metadata and provenance](docs/06-metadata.md) | Enhancing models with metadata for reproducibility |
| [Validation mechanisms](docs/07-validation.md) | Multi-layered approach to ensuring model correctness |
| [Complex priors](docs/08-complex-priors.md) | Advanced techniques for sophisticated prior distributions |
| [Tooling and interfaces](docs/09-tooling.md) | Guidelines for creating user-friendly model construction tools |
| [Conclusion](docs/10-conclusion.md) | Summary and future directions |

## Schema

The [full JSON schema](schema/codephy-schema.json) provides a formal specification of the Codephy format.

## Examples

- [Simple model](examples/simple-model.json): A basic phylogenetic model with HKY substitution model
- [With metadata](examples/with-metadata.json): Model with comprehensive metadata and provenance
- [Complex priors](examples/complex-priors.json): Model demonstrating hierarchical priors and constraints

## Getting Started

To create a Codephy model:

1. Define your random variables (parameters, trees, etc.)
2. Define deterministic functions (e.g., substitution models)
3. Specify observed data if available
4. Add metadata for reproducibility
5. Validate the model against the schema

```json
{
  "model": "mymodel",
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "tree": {
      "distribution": {
        "type": "Yule",
        "parameters": {
          "birthRate": "birthRateParam"
        }
      }
    }
  },
  "deterministicFunctions": {
    "substitutionModel": {
      "function": "hky",
      "arguments": {
        "kappa": "kappaParam",
        "baseFrequencies": "baseFreqParam"
      }
    }
  }
}
```

## Contributing

We welcome contributions to the Codephy specification and tooling ecosystem. 
Please see our [contribution guidelines](CONTRIBUTING.md) for more information.

## Citation

If you use Codephy in your research, please cite:

```
Codephy core group (2025). Codephy: A portable format for specifying phylogenetic models. 
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.