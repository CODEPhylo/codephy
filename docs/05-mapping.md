# Mapping to class hierarchies

Because the Codephy format is intended to be portable, it's important that inference engines and modeling tools can efficiently map these JSON specifications to their internal representation. 
This section discusses strategies for implementing Codephy in various languages and frameworks.

## General mapping concepts

At a high level, mapping a Codephy specification to an inference engine's internal representation involves:

1. Parsing the JSON document
2. Creating appropriate objects for each random variable and deterministic function
3. Establishing references between these objects
4. Setting up the inference procedure based on observed vs. latent variables

There are several approaches to this mapping, each with different trade-offs.

## Schema-based code generation

One powerful approach is to use JSON Schema to automatically generate classes that represent the Codephy format.

### Tools for code generation

Several tools can generate class definitions from JSON Schema:

- **Java**: [jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo)
- **C++**: [quicktype](https://quicktype.io/)
- **Python**: [datamodel-code-generator](https://github.com/koxudaxi/datamodel-code-generator)
- **C#**: [NJsonSchema](https://github.com/RicoSuter/NJsonSchema)
- **TypeScript**: [quicktype](https://quicktype.io/), [json-schema-to-typescript](https://github.com/bcherny/json-schema-to-typescript)

Here's an example of how this approach might look in practice for Java:

```bash
# Generate Java classes from the Codephy schema
jsonschema2pojo \
  --source codephy-schema.json \
  --target src/main/java \
  --package org.codephy.model \
  --use-title-as-classname
```

This would generate classes like:

```java
package org.codephy.model;

public class CodephyModel {
    private String model;
    private String codephyVersion;
    private Map<String, RandomVariable> randomVariables;
    private Map<String, DeterministicFunction> deterministicFunctions;
    private Metadata metadata;
    private Provenance provenance;
    private List<Constraint> constraints;
    
    // Getters and setters
}

public class RandomVariable {
    private Distribution distribution;
    private Object observedValue;
    
    // Getters and setters
}

public class DeterministicFunction {
    private String function;
    private Map<String, Object> arguments;
    
    // Getters and setters
}

// Additional classes for Distribution, Constraint, etc.
```

### Advantages of schema-based generation

- Ensures the class structure matches the schema definition
- Reduces manual coding errors
- Automatically adapts to schema changes
- Often includes validation logic

### Limitations

- Generated code may not be optimal for inference purposes
- May require additional mapping to engine-specific classes
- Limited control over the generated class structure

## Manual or reflection-based parsing

An alternative approach is to parse the JSON directly into more specialized classes designed for inference.

### Reflection-based approach

Languages with strong reflection capabilities (e.g., Java, Python, C#) can map JSON to objects dynamically:

```python
# Python example using pydantic
from pydantic import BaseModel, Field
from typing import Dict, List, Optional, Union, Any

class Distribution(BaseModel):
    type: str
    generates: str
    # Additional fields depend on the distribution type

class RandomVariable(BaseModel):
    distribution: Distribution
    observedValue: Optional[Any] = None

class DeterministicFunction(BaseModel):
    function: str
    arguments: Dict[str, Union[str, int, float, bool, Dict]]

class CodephyModel(BaseModel):
    model: str
    codephyVersion: str
    randomVariables: Dict[str, RandomVariable]
    deterministicFunctions: Dict[str, DeterministicFunction]
    # Additional fields

# Parse JSON
with open('model.json', 'r') as f:
    model_data = json.load(f)
    
model = CodephyModel(**model_data)
```

### Custom parsing approach

For more control, you can implement custom parsing logic:

```java
// Java example with custom parsing
public class CodephyParser {
    public InferenceModel parseModel(String json) {
        JsonNode root = objectMapper.readTree(json);
        
        // Create model container
        InferenceModel model = new InferenceModel(
            root.get("model").asText(),
            root.get("codephyVersion").asText()
        );
        
        // Parse random variables
        JsonNode variables = root.get("randomVariables");
        for (Iterator<String> it = variables.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            JsonNode varNode = variables.get(name);
            
            // Parse distribution
            Distribution dist = parseDistribution(varNode.get("distribution"));
            
            // Create variable
            RandomVariable var = new RandomVariable(name, dist);
            
            // Add observed value if present
            if (varNode.has("observedValue")) {
                var.setObservedValue(parseObservedValue(
                    varNode.get("observedValue"),
                    dist.getGeneratesType()
                ));
            }
            
            model.addRandomVariable(var);
        }
        
        // Parse deterministic functions
        // ...
        
        return model;
    }
    
    private Distribution parseDistribution(JsonNode node) {
        // Custom logic to instantiate the right distribution type
        // ...
    }
    
    private Object parseObservedValue(JsonNode node, String type) {
        // Custom logic to parse observed values based on their type
        // ...
    }
}
```

### Advantages of custom parsing

- More control over the mapping process
- Can directly create engine-specific objects
- Can include specialized validation
- May be more performant for large models

### Limitations

- Requires more implementation effort
- Must be updated manually when the schema changes
- More opportunity for bugs if schema validation is not applied

## Factory patterns for distribution and function creation

Regardless of the initial parsing approach, a factory pattern is often useful for creating the appropriate distribution or function objects:

```java
// Distribution factory example
public class DistributionFactory {
    public Distribution createDistribution(String type, JsonNode params) {
        switch (type) {
            case "LogNormal":
                return new LogNormalDistribution(
                    getParameterValue(params, "meanlog"),
                    getParameterValue(params, "sdlog")
                );
            case "Gamma":
                return new GammaDistribution(
                    getParameterValue(params, "shape"),
                    getParameterValue(params, "rate")
                );
            case "Dirichlet":
                return new DirichletDistribution(
                    getParameterArray(params, "alpha")
                );
            // Additional distribution types
            default:
                throw new UnknownDistributionException(type);
        }
    }
    
    private ParameterValue getParameterValue(JsonNode params, String name) {
        // Logic to handle simple values or references to variables/expressions
    }
    
    private ParameterValue[] getParameterArray(JsonNode params, String name) {
        // Logic to handle array parameters
    }
}

// Similar factory for deterministic functions
```

This approach allows the inference engine to create the appropriate internal objects for each distribution and function type specified in the Codephy model.

## Handling references between variables

A key challenge in mapping Codephy to an internal representation is resolving references between variables. This typically requires a two-pass approach:

1. First pass: Create all variable and function objects
2. Second pass: Resolve references between them

```java
// First pass
Map<String, Variable> variables = new HashMap<>();
for (Entry<String, JsonNode> entry : json.get("randomVariables").fields()) {
    String name = entry.getKey();
    variables.put(name, createVariable(name, entry.getValue()));
}

// Second pass
for (Entry<String, Variable> entry : variables.entrySet()) {
    resolveReferences(entry.getValue(), variables);
}
```

This approach ensures that all objects are created before attempting to establish their relationships.

## Mapping complex structures

Some aspects of Codephy require special handling:

### Expressions

For variables that use mathematical expressions, you might need an expression parser or evaluator:

```java
// Handle expression type parameters
if (paramNode.has("expression")) {
    String expr = paramNode.get("expression").asText();
    return new ExpressionParameter(expr, expressionEvaluator);
}
```

### Constraints

Constraints should be mapped to appropriate validation or enforcement mechanisms:

```java
// Create constraint objects
for (JsonNode constraintNode : json.get("constraints")) {
    String type = constraintNode.get("type").asText();
    String left = constraintNode.get("left").asText();
    String right = constraintNode.get("right").asText();
    
    switch (type) {
        case "lessThan":
            constraints.add(new LessThanConstraint(
                variables.get(left),
                variables.get(right)
            ));
            break;
        // Other constraint types
    }
}
```

### Trees and alignments

Special data types like trees and alignments often require custom parsing logic:

```java
// Parse observed alignment
if (varNode.has("observedValue") && dist.getGeneratesType().equals("ALIGNMENT")) {
    JsonNode alignmentData = varNode.get("observedValue");
    Alignment alignment = new Alignment();
    
    for (JsonNode taxonData : alignmentData) {
        String taxon = taxonData.get("taxon").asText();
        String sequence = taxonData.get("sequence").asText();
        alignment.addSequence(taxon, sequence);
    }
    
    var.setObservedValue(alignment);
}
```

## Integration with existing inference engines

When integrating Codephy with an existing inference engine, you'll need to map Codephy objects to the engine's native objects:

```java
// Convert Codephy model to BEAST model
public BEASTModel convertToBEAST(CodephyModel codephyModel) {
    BEASTModel beastModel = new BEASTModel();
    
    // Convert parameters
    for (RandomVariable var : codephyModel.getRandomVariables().values()) {
        if (!var.hasObservedValue() && var.getDistribution().getGeneratesType().equals("REAL")) {
            // Create BEAST parameter
            RealParameter param = new RealParameter();
            param.setID(var.getName());
            
            // Set prior based on distribution
            if (var.getDistribution().getType().equals("LogNormal")) {
                LogNormalPrior prior = new LogNormalPrior();
                prior.meanInput.setValue(
                    ((LogNormalDistribution)var.getDistribution()).getMeanlog(),
                    prior
                );
                prior.sigmaInput.setValue(
                    ((LogNormalDistribution)var.getDistribution()).getSdlog(),
                    prior
                );
                param.priorInput.setValue(prior, param);
            }
            
            beastModel.addParameter(param);
        }
    }
    
    // Convert other model components
    // ...
    
    return beastModel;
}
```

This mapping logic will be specific to each inference engine and will need to handle the various distribution types, functions, and data structures used in Codephy.

## Code organization patterns

Regardless of the mapping approach, several code organization patterns are useful:

### Visitor pattern

The visitor pattern can help when applying operations to the model structure:

```java
public interface ModelVisitor {
    void visit(RandomVariable var);
    void visit(DeterministicFunction func);
    void visit(Distribution dist);
    // Additional visit methods
}

// Implementation example
public class ModelValidator implements ModelVisitor {
    private List<String> errors = new ArrayList<>();
    
    public void visit(RandomVariable var) {
        // Validation logic for random variables
    }
    
    // Other visit methods
    
    public List<String> getErrors() {
        return errors;
    }
}
```

### Builder pattern

The builder pattern can help with constructing complex model objects:

```java
// Building a Codephy model programmatically
CodephyModel model = new CodephyModelBuilder()
    .setName("mymodel")
    .setVersion("0.1")
    .addRandomVariable(
        new RandomVariableBuilder("kappaParam")
            .setDistribution(
                new LogNormalDistribution(1.0, 0.5)
            )
            .build()
    )
    // Additional components
    .build();
```

## Summary

Mapping Codephy specifications to internal class hierarchies can be approached in several ways, each with its own trade-offs. 
The most appropriate approach depends on:

- The language and framework of the inference engine
- The need for validation during parsing
- Performance requirements
- The complexity of the target class hierarchy

Regardless of the approach, the key is to create a clear mapping from Codephy concepts (random variables, deterministic functions, distributions, etc.) to the engine's internal representation, ensuring that the semantics of the model are preserved.

By separating the model specification (Codephy) from its implementation in specific inference engines, we enable greater interoperability and reuse of models across different software tools.