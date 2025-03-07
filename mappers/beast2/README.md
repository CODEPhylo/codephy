# Codephy to BEAST2 Mapper

This module provides a mapper from Codephy JSON model specifications to BEAST2 Java objects. It demonstrates how Codephy models can be directly mapped to the internal object model of existing phylogenetic inference frameworks.

## Overview

The mapper creates a complete BEAST2 model structure from a Codephy JSON specification:

1. Creates BEAST2 objects for all random variables and deterministic functions
2. Resolves references between components
3. Builds the full model including posterior, likelihood, and priors
4. Sets up MCMC with appropriate operators and loggers
5. Provides a command-line tool for easy integration into workflows

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- BEAST2 v2.7.x (2.7.5 recommended)
- Jackson for JSON parsing

## Setup and Installation

### 1. Install BEAST2 JAR Dependencies

This project requires BEAST2 libraries that may not be available in public Maven repositories. You need to install them manually to your local Maven repository.

#### Option A: Using the Provided Script

If you have BEAST2 installed, you can use the provided script to install the necessary JAR files:

```bash
# Make the script executable
chmod +x install-beast-jars.sh

# Run the script, providing the path to your BEAST2 installation
./install-beast-jars.sh /path/to/beast2
```

#### Option B: Manual Installation

If you prefer to install the JARs manually:

```bash
# First, install the base BEAST2 JAR
mvn install:install-file \
  -Dfile=/path/to/beast2/lib/packages/BEAST.base.jar \
  -DgroupId=beast2 \
  -DartifactId=beast-base \
  -Dversion=2.7.5 \
  -Dpackaging=jar

# Then, install the BEAST2 app JAR
mvn install:install-file \
  -Dfile=/path/to/beast2/lib/packages/BEAST.app.jar \
  -DgroupId=beast2 \
  -DartifactId=beast-app \
  -Dversion=2.7.5 \
  -Dpackaging=jar

# Finally, install the launcher JAR
mvn install:install-file \
  -Dfile=/path/to/beast2/lib/launcher.jar \
  -DgroupId=beast2 \
  -DartifactId=beast-launcher \
  -Dversion=2.7.5 \
  -Dpackaging=jar
```

### 2. Build the Project

Once the dependencies are installed, build the project:

```bash
mvn clean package
```

This will generate:
- `target/codephy-mapper-beast2-0.1.0-jar-with-dependencies.jar` - Core library with dependencies
- `target/codephy-app-0.1.0.jar` - Executable application JAR
- `target/codephyMapper` - Shell script for command-line usage

### 3. Install Locally (Optional)

To make the mapper available from any location:

```bash
# Create the bin directory if it doesn't exist
mkdir -p ~/bin

# Copy the necessary files
cp target/codephy-app-0.1.0.jar ~/bin/
cp target/codephyMapper ~/bin/
chmod +x ~/bin/codephyMapper

# Add ~/bin to your PATH if not already there
echo 'export PATH="$HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

Alternatively, use the Maven profile:

```bash
mvn install -P install-local
```

## Usage

### Command Line Interface

Convert a Codephy JSON model to BEAST2 XML:

```bash
codephyMapper input.json output.xml
```

If you haven't installed the script to your PATH:

```bash
cd target
./codephyMapper input.json output.xml
```

### Programmatic Usage

To use the mapper programmatically in your Java code:

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

### Maven Dependency

Include this module in your project by adding the following to your pom.xml:

```xml
<dependency>
    <groupId>org.codephy</groupId>
    <artifactId>codephy-mapper-beast2</artifactId>
    <version>${project.version}</version>
</dependency>
```

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

## Project Structure

```
.
└── beast2
    ├── README.md                       # This file
    ├── install-beast-jars.sh           # Script to install BEAST2 dependencies
    ├── pom.xml                         # Maven project configuration
    ├── src
    │   └── main
    │       └── java
    │           └── org
    │               └── codephy
    │                   ├── beast2
    │                   │   └── app     # Command-line application
    │                   └── mappers
    │                       └── beast2  # Core mapping functionality
    └── target                          # Build outputs
```

### Key Components

- `CodephyToBEAST2Mapper.java` - Main mapper class
- `CodephyMapperApp.java` - Command-line application
- `DistributionMapper.java` - Maps Codephy distributions to BEAST2
- `ModelBuilder.java` - Constructs the BEAST2 model structure
- `StandardDistributionsMapper.java` - Maps standard probability distributions
- `TreeDistributionsMapper.java` - Maps phylogenetic tree distributions
- `Utils.java` - Utility functions

## Troubleshooting

### Missing BEAST2 Dependencies

If you encounter errors like `java.lang.ClassNotFoundException: beast.base...`, ensure you've properly installed the BEAST2 JAR files to your local Maven repository as described in the Setup section.

### Script Cannot Find BEAST2

The script attempts to locate your BEAST2 installation in common locations. If it fails:

1. Set the `BEAST` environment variable to your BEAST2 installation path:
   ```bash
   export BEAST="/path/to/your/beast2"
   ```
2. Then run the mapper:
   ```bash
   codephyMapper input.json output.xml
   ```

### Java Version Issues

This project requires Java 11 or higher. Verify your Java version:

```bash
java -version
```

### Memory Issues

If you encounter memory errors with large models, modify the `-Xmx` parameter in the `codephyMapper` script:

```bash
# Change -Xmx4g to a larger value, e.g. -Xmx8g for 8GB
```

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