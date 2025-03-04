# Introduction

## Overview

We outline a simple approach to specifying probabilistic phylogenetic models using JSON. This approach aims to:

1. Separate *random variables* from *deterministic functions*.
2. Allow random variables to be *observed* if we have data for them, or left latent if we do not.

A key goal is to develop a *portable format* that multiple inference engines can ingest. This fosters interoperability and facilitates more efficient development of a rich set of upstream tools for constructing and manipulating phylogenetic models. The method we present is sufficiently general to capture typical elements of a Bayesian phylogenetic model, such as parameters (with specified prior distributions), tree priors, and molecular substitution models. It also naturally accommodates an observed alignment (e.g., sequence data) upon which we wish to perform inference.

## Motivation

Phylogenetic analysis software has evolved significantly over the past two decades, with numerous specialized tools available for different types of analyses. However, each tool typically has its own model specification format, leading to several challenges:

- Researchers must learn multiple formats to use different software
- Models cannot be easily shared between different inference engines
- Comparison of results across tools is complicated by differences in model implementation
- Tools for model construction, validation, and visualization must be rebuilt for each format

Codephy addresses these issues by providing a common, portable format that can be used across different inference engines. By standardizing how phylogenetic models are specified, we enable:

- Easier comparison of inference methods using identical models
- Development of shared tooling for model creation and validation
- Better documentation and sharing of models between researchers
- Clearer separation between model specification and inference methodology

## Design principles

Codephy was designed with several core principles in mind:

- **Clarity**: Models should be human-readable and clearly express their intent
- **Completeness**: The format should be capable of expressing all common phylogenetic models
- **Portability**: Models should be consumable by different inference engines
- **Extensibility**: The format should be able to evolve to accommodate new types of models
- **Separation of concerns**: The format should distinguish between model specification and inference details

The resulting JSON-based format balances these principles by focusing on the fundamental components of probabilistic models: random variables and their distributions, deterministic functions that relate these variables, and observed data. 
This approach allows complex models to be specified in a way that is both machine-readable and human-understandable.

## The benefits of JSON

We choose JSON (JavaScript Object Notation) as the underlying format for several reasons:

- **Ubiquity**: JSON is supported in virtually all programming languages
- **Readability**: JSON has a relatively simple syntax that is (if necessary) human-readable
- **Schema support**: JSON Schema provides a way to validate documents
- **Extensibility**: JSON can represent complex nested structures
- **Tooling**: Numerous tools exist for editing, validating, and transforming JSON

While JSON may be more verbose than some domain-specific languages, its widespread adoption and rich ecosystem of tools make it an ideal choice for a format that aims to be accessible across different software environments.

## Applications

The Codephy format can be used in several contexts:

- **Model specification** for phylogenetic inference
- **Model sharing** between researchers or in publications
- **Simulation studies** to compare inference methods
- **Educational purposes** to teach probabilistic modeling
- **Model repositories** for standard analyses
- **Automated analysis pipelines** for high-throughput studies

In each of these contexts, the portable nature of Codephy allows for greater reproducibility and more efficient workflows.

## Document organization

The remainder of this documentation covers:

- The core concepts of random variables and deterministic functions
- Examples of complete model specifications
- How inference engines should interpret Codephy models
- Implementation strategies for mapping to internal class hierarchies
- Metadata and provenance for model sharing
- Validation mechanisms to ensure correctness
- Advanced techniques for specifying complex priors
- Recommendations for user-friendly tooling
- Future directions for the format

Each section is designed to be self-contained while building on previous concepts, allowing readers to focus on the aspects most relevant to their needs.