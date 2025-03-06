# Codephy Java Validator

A Java-based validation tool for Codephy phylogenetic model specifications that verifies both structural correctness (against the JSON schema) and semantic validity.

## Features

- **Schema Validation**: Ensures the model adheres to the Codephy JSON schema
- **Reference Checking**: Verifies that all referenced variables and functions exist
- **Circular Dependency Detection**: Identifies circular references in the model
- **Constraint Validation**: Ensures constraints are properly defined
- **Distribution Type Checking**: Verifies that distribution types match their expected output types

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)

## Building

To build the validator, use Maven:

```bash
mvn clean package
```

This creates two JAR files in the `target` directory:
- `codephy-validator-0.1.0.jar`: The validator without dependencies
- `codephy-validator-0.1.0-jar-with-dependencies.jar`: A standalone executable JAR with all dependencies included

## Usage

### Command Line

You can use the validator directly from the command line:

```bash
# Using the standalone JAR
java -jar target/codephy-validator-0.1.0-jar-with-dependencies.jar examples/my_model.json

# Specify a custom schema location
java -jar target/codephy-validator-0.1.0-jar-with-dependencies.jar examples/my_model.json path/to/schema.json
```

### Java API

You can also use the validator in your own Java code:

```java
import org.codephy.validator.CodephyValidator;
import org.codephy.validator.CodephyValidator.ValidationResult;

// Create a validator with the default schema location
CodephyValidator validator = new CodephyValidator();

// Or with a specific schema path
// CodephyValidator validator = new CodephyValidator("path/to/schema.json");

// Validate a model file
ValidationResult result = validator.validateFile("examples/my_model.json");

if (result.isValid()) {
    System.out.println("Model is valid!");
} else {
    System.out.println("Model has validation errors:");
    for (String error : result.getErrors()) {
        System.out.println("- " + error);
    }
}

// You can also validate a JsonNode object directly
ObjectMapper mapper = new ObjectMapper();
JsonNode model = mapper.readTree(new File("examples/my_model.json"));
ValidationResult modelResult = validator.validateModel(model);
```

## Project Structure

```
src/main/java/org/codephy/validator/
├── CodephyValidator.java      # Main validator class with schema and semantic validation
└── ValidationResult.java      # Result class for validation operations (inner class of CodephyValidator)

src/test/java/org/codephy/validator/
└── CodephyValidatorTest.java  # Test cases with various model examples
```

## Validation Checks

The validator performs the following checks:

1. **JSON Schema Validation**
   - Ensures the model structure conforms to the Codephy schema
   - Validates field types, required fields, and allowed values

2. **Semantic Validation**
   - **Name Collisions**: Checks for duplicate names between random variables and deterministic functions
   - **Undefined References**: Ensures all referenced variables exist
   - **Circular Dependencies**: Detects any circular references in the model graph
   - **Constraint Validation**: Verifies that constraints use valid types and reference existing variables
   - **Distribution Type Checking**: Ensures distributions generate the correct type of values

## Testing

Run the tests with Maven:

```bash
mvn test
```

The tests include examples of:
- Valid simple models
- Models with circular dependencies
- Models with undefined references
- Models with invalid constraints
- Models with incorrect distribution types
- Complete PhyloCTMC models

## Integration with CI/CD

You can integrate the validator into continuous integration pipelines:

```yaml
# Example GitHub Actions step
- name: Validate Codephy models
  run: |
    java -jar target/codephy-validator-0.1.0-jar-with-dependencies.jar examples/*.json
```

## Extending the Validator

To add additional validation checks, extend the `validateSemantics` method in the `CodephyValidator` class. For example, to add a check for unused variables:

```java
private List<String> checkUnusedVariables(JsonNode randomVars, JsonNode deterministicFuncs) {
    List<String> errors = new ArrayList<>();
    // Implementation here
    return errors;
}

// Then add it to validateSemantics
private List<String> validateSemantics(JsonNode model) {
    // Existing checks...
    
    // Add new check
    List<String> unusedErrors = checkUnusedVariables(randomVars, deterministicFuncs);
    errors.addAll(unusedErrors);
    
    return errors;
}
```

## Dependencies

- [Jackson](https://github.com/FasterXML/jackson): For JSON processing
- [json-schema-validator](https://github.com/networknt/json-schema-validator): For JSON Schema validation
- [JUnit](https://junit.org/junit5/): For testing (test scope only)