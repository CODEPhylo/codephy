# Validation mechanisms

A critical aspect of any model specification format is ensuring that models are well-formed and semantically valid. 
For Codephy, we envision a multi-layered validation approach that helps users create correct models and enables inference engines to safely process them.

## The importance of validation

Validation serves several important purposes in the Codephy ecosystem:

1. **Catching errors early**: Identifying problems during model creation rather than during inference
2. **Ensuring portability**: Confirming that models will work across different inference engines
3. **Preventing runtime failures**: Avoiding situations where invalid models cause inference engines to crash
4. **Improving model clarity**: Enforcing best practices that make models more understandable
5. **Supporting reproducibility**: Ensuring models are complete and self-contained

By implementing robust validation, we reduce the friction in developing, sharing, and using phylogenetic models.

## Multi-layered validation approach

Codephy validation operates at several levels, with each level addressing different aspects of model correctness:

1. **Schema validation**: Basic structural and type checking
2. **Graph consistency**: Ensuring valid references and dependency structure
3. **Type checking**: Verifying compatibility of inputs and outputs
4. **Distribution-specific validation**: Checking constraints for specific distributions
5. **Data validation**: Ensuring observed data is properly formatted

This layered approach provides comprehensive validation while allowing for meaningful error messages that pinpoint specific issues.

## Schema validation

The foundation of validation is the JSON Schema presented in the Appendix. This schema provides structural validation that ensures:

- Required fields are present (`distribution` for random variables, `function` and `arguments` for deterministic functions)
- Field values have the correct types (e.g., strings, numbers, arrays)
- Enumerations are restricted to allowed values (e.g., distribution types)
- Object properties adhere to specified patterns (e.g., variable names)

Many programming languages offer established JSON Schema validators that can be used to verify Codephy models against this schema:

```javascript
// Example of schema validation in JavaScript
const Ajv = require('ajv');
const ajv = new Ajv();
const validate = ajv.compile(codephySchema);
const valid = validate(modelInstance);

if (!valid) {
  console.error('Validation errors:', validate.errors);
}
```

Similar libraries exist for other languages:

- **Python**: `jsonschema`
- **Java**: [`org.everit.json.schema`](https://github.com/everit-org/json-schema)
- **C#**: `Newtonsoft.Json.Schema`
- **R**: `jsonvalidate`

Schema validation should be the first step in any validation process, as it ensures the basic structure is correct before attempting more sophisticated validation.

## Graph consistency

Beyond basic schema validation, Codephy models must have consistent dependency graphs. 
We recommend implementations include validation that:

- Verifies all references to variables and functions resolve to existing objects
- Detects cycles in the dependency graph
- Ensures all required parameters for specific distributions or functions are provided

```javascript
// Pseudocode for reference validation
function validateReferences(model) {
  const definedNames = new Set([
    ...Object.keys(model.randomVariables),
    ...Object.keys(model.deterministicFunctions)
  ]);
  
  // Check deterministic function references
  for (const [name, func] of Object.entries(model.deterministicFunctions)) {
    for (const argName of Object.values(func.arguments)) {
      if (typeof argName === 'string' && !definedNames.has(argName)) {
        throw new Error(`Function ${name} references undefined variable: ${argName}`);
      }
    }
  }
  
  // Check random variable references in distributions
  for (const [name, rv] of Object.entries(model.randomVariables)) {
    // Check distribution parameters that are references
    checkDistributionReferences(rv.distribution, definedNames, name);
  }
}

function checkDistributionReferences(distribution, definedNames, varName) {
  // Distribution-specific reference checking
  // For example, for a Yule distribution:
  if (distribution.type === 'Yule') {
    const birthRate = distribution.parameters.birthRate;
    if (typeof birthRate === 'string' && !definedNames.has(birthRate)) {
      throw new Error(`Variable ${varName} references undefined birth rate: ${birthRate}`);
    }
  }
  // Add checks for other distribution types
}
```

Cycle detection is particularly important, as cycles can lead to infinite recursion during inference:

```javascript
function detectCycles(model) {
  const graph = buildDependencyGraph(model);
  const visited = new Set();
  const recursionStack = new Set();
  
  for (const node of graph.keys()) {
    if (hasCycle(node, graph, visited, recursionStack)) {
      throw new Error(`Cycle detected in dependency graph involving: ${[...recursionStack].join(' -> ')}`);
    }
  }
}

function hasCycle(node, graph, visited, recursionStack) {
  if (!visited.has(node)) {
    visited.add(node);
    recursionStack.add(node);
    
    for (const dependent of graph.get(node) || []) {
      if (!visited.has(dependent) && hasCycle(dependent, graph, visited, recursionStack)) {
        return true;
      } else if (recursionStack.has(dependent)) {
        recursionStack.add(dependent); // Add to show the complete cycle
        return true;
      }
    }
  }
  
  recursionStack.delete(node);
  return false;
}
```

## Type checking

A more sophisticated level of validation involves type checking to ensure that the output type of a referenced variable is compatible with the expected input type where it is used. 
For example:

- A `REAL_VECTOR` output from a Dirichlet distribution is appropriate for base frequencies
- A `TREE` output from a Yule process is required for the tree parameter of a PhyloCTMC

Type validation can be integrated into inference engines or provided as separate tools:

```javascript
// Pseudocode for type validation
function validateTypes(model) {
  const typeMap = new Map();
  
  // First, determine output types of all variables
  for (const [name, rv] of Object.entries(model.randomVariables)) {
    typeMap.set(name, getOutputType(rv.distribution));
  }
  
  for (const [name, func] of Object.entries(model.deterministicFunctions)) {
    typeMap.set(name, getDeterministicOutputType(func.function, func.arguments));
  }
  
  // Then, check parameter compatibility
  for (const [name, rv] of Object.entries(model.randomVariables)) {
    validateDistributionParameters(rv.distribution, typeMap);
  }
  
  for (const [name, func] of Object.entries(model.deterministicFunctions)) {
    validateFunctionParameters(func.function, func.arguments, typeMap);
  }
}

function getOutputType(distribution) {
  // Return the "generates" field from the distribution
  return distribution.generates;
}

function validateDistributionParameters(distribution, typeMap) {
  // Distribution-specific type checking
  if (distribution.type === 'PhyloCTMC') {
    const treeRef = distribution.parameters.tree;
    const treeType = typeMap.get(treeRef);
    
    if (treeType !== 'TREE') {
      throw new Error(`PhyloCTMC expects a TREE but got ${treeType} for parameter 'tree'`);
    }
    
    // Check other parameters
  }
  // Add checks for other distribution types
}
```

This level of validation helps catch more subtle errors that wouldn't be detected by simple schema validation or reference checking.

## Distribution-specific validation

Each distribution or function may have specific constraints that go beyond simple type checking. For example:

- Ensuring Dirichlet concentration parameters are positive
- Verifying that birth rates for tree priors are positive
- Checking that matrix parameters are symmetric when required

These validations are often best integrated into the library that implements the specific distribution or function:

```javascript
function validateLogNormalParameters(distribution) {
  // Check that sdlog is positive
  const sdlog = distribution.sdlog;
  if (typeof sdlog === 'number' && sdlog <= 0) {
    throw new Error('LogNormal distribution requires sdlog > 0');
  }
  // Note: For references or expressions, more complex validation would be needed
}

function validateDirichletParameters(distribution) {
  // Check that all alpha values are positive
  const alpha = distribution.alpha;
  if (Array.isArray(alpha)) {
    for (let i = 0; i < alpha.length; i++) {
      if (typeof alpha[i] === 'number' && alpha[i] <= 0) {
        throw new Error(`Dirichlet distribution requires alpha[${i}] > 0`);
      }
    }
  }
}
```

## Data validation

For observed values, additional validation ensures that the data format matches the expected output of the distribution:

- Sequence data in a PhyloCTMC should contain valid characters (A, C, G, T)
- Numeric observed values should be within valid ranges
- Taxon names in trees and alignments should be consistent

```javascript
function validateAlignment(alignment) {
  const validBases = new Set(['A', 'C', 'G', 'T', '-', 'N', 'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V']);
  
  for (const taxon of alignment) {
    // Check taxon name is present
    if (!taxon.taxon || taxon.taxon.trim() === '') {
      throw new Error('Alignment contains a taxon with no name');
    }
    
    // Check sequence is present
    if (!taxon.sequence) {
      throw new Error(`Taxon ${taxon.taxon} has no sequence`);
    }
    
    // Check sequence contains valid characters
    for (let i = 0; i < taxon.sequence.length; i++) {
      const base = taxon.sequence[i].toUpperCase();
      if (!validBases.has(base)) {
        throw new Error(`Invalid base '${base}' at position ${i} in sequence for taxon ${taxon.taxon}`);
      }
    }
  }
  
  // Check all sequences have the same length
  const lengths = new Set(alignment.map(taxon => taxon.sequence.length));
  if (lengths.size > 1) {
    throw new Error('Alignment contains sequences of different lengths');
  }
}
```

## Integration with tooling

To make validation accessible to users, we recommend integration into the modeling workflow:

### Interactive validation

Model-building GUIs should provide immediate feedback on model correctness, highlighting errors as they occur and suggesting fixes. 
This could include:

- Syntax highlighting for JSON structure
- Error indicators for invalid references
- Type checking for function and distribution parameters
- Warning indicators for potential issues (e.g., unusual parameter values)

### Command-line validators

For integration into scripts and pipelines, command-line validation tools are valuable:

```bash
$ codephy-validate model.json
Validating model.json...
ERROR: Reference to undefined variable 'mutationRate' in function 'substitutionModel'
ERROR: Cycle detected in dependency graph: tree -> alignment -> siteModel -> tree
```

These tools can provide different levels of validation (e.g., `--strict` for full validation or `--basic` for schema-only validation) and output formats (text, JSON, HTML reports).

### Pre-inference validation

Inference engines should validate models before beginning the inference process, providing clear error messages for invalid models:

```bash
$ beast model.json
Validating model...
ERROR: PhyloCTMC distribution requires tree parameter to be a TREE, but 'myTree' is a REAL
Inference aborted due to validation errors.
```

This prevents wasted computational resources on models that would fail during inference.

### Repository validation

Model repositories should validate submissions before accepting them, ensuring that all models in the repository are at least structurally valid:

```
Uploading model to repository...
Validating model...
WARNING: Model has no metadata.description field
WARNING: No license specified, defaulting to CC-BY-4.0
Model uploaded successfully with ID: repo:12345
```

## Recommended validation workflow

We recommend a progressive validation approach for Codephy models:

1. Start with basic schema validation to ensure structural correctness
2. Proceed to reference and cycle detection to verify graph integrity
3. Apply type checking to ensure parameter compatibility
4. Perform distribution-specific validations for parameter constraints
5. Finally, validate observed data formats if present

This layered approach allows users to address fundamental issues before moving on to more subtle validation checks.

## Handling validation in different contexts

Validation behavior should be adaptable to different contexts:

### Development time

During model creation, validation should be:
- Immediate, providing real-time feedback
- Detailed, highlighting specific issues
- Assistive, suggesting fixes where possible
- Incremental, validating parts of the model as they are created

### Submission time

When submitting models to repositories or sharing them with others:
- Comprehensive, checking all aspects of the model
- Strict, requiring full compliance with the schema
- Complete, providing a full report of all issues
- Metadata-focused, ensuring proper documentation

### Inference time

Before running inference:
- Focused on issues that would prevent successful inference
- Performance-oriented, avoiding unnecessary validation
- Engine-specific, checking compatibility with the specific inference engine
- Clear, providing actionable error messages

## Implementation considerations

When implementing Codephy validation, consider these best practices:

### Error categorization

Categorize errors into different levels:

- **Fatal errors**: Issues that make the model unusable (e.g., cycles, missing required fields)
- **Inference errors**: Issues that would prevent successful inference (e.g., type mismatches)
- **Warnings**: Potential problems that don't prevent inference but might indicate mistakes (e.g., unusual parameter values)
- **Style issues**: Deviations from best practices (e.g., missing metadata fields)

### Localized error messages

Provide error messages that pinpoint the exact location of issues:

```
Error in randomVariables.kappaParam.distribution.sdlog: Value must be positive
```

This helps users quickly locate and fix problems.

### Batch validation

Allow validation of multiple models at once, which is useful for large-scale analyses:

```bash
$ codephy-validate models/*.json --report=validation-report.html
```

### Extensible validation

Design validation systems to be extensible, allowing users to add custom validation rules for specific use cases:

```javascript
validator.addRule('myCustomRule', (model) => {
  // Custom validation logic
  if (someCondition) {
    return [{ level: 'warning', message: 'Custom warning message' }];
  }
  return [];
});
```

## Summary

By implementing this multi-layered validation approach, the Codephy ecosystem can provide robust guardrails for model specification, reducing errors and improving the efficiency of the modeling process. 
Standardized validation also enables model sharing with confidence that the models will work across different inference engines.

The combination of schema validation, reference checking, type validation, distribution-specific checks, and data validation ensures that Codephy models are well-formed, consistent, and ready for inference. 
Integration of validation into various tools and workflows makes it easier for users to create correct models and diagnose issues when they arise.
