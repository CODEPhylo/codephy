# Tooling and interfaces

While the Codephy format provides a clear representation for phylogenetic models, manually writing JSON specifications can be error-prone and cumbersome for complex models. 
This section explores various approaches to creating user-friendly tools that allow researchers to construct, visualize, and modify these models without directly editing raw JSON.

## The importance of user-friendly interfaces

The success of any model specification format depends not just on its technical merits, but also on the quality of the tools available for working with it. 
Well-designed interfaces can:

- Lower the barrier to entry for new users
- Reduce errors in model specification
- Accelerate the modeling process
- Facilitate exploration of different model structures
- Enable collaboration between researchers with different levels of technical expertise
- Promote adoption of standardized formats like Codephy

By investing in high-quality tools alongside the format itself, we can significantly increase the impact and utility of Codephy in the phylogenetics community.

## Graphical model builders

Graphical interfaces offer an intuitive way to construct phylogenetic models by representing the dependency structure visually.

### Node-based editors

Node-based editors allow users to create random variables and deterministic functions as nodes in a directed graph, with edges representing dependencies. 
Users can select from a palette of distribution types and functions, configure their parameters through forms, and connect them by drawing edges.

![Example of a node-based editor](https://example.org/node-editor.png)

Key features of effective node-based editors include:
- Drag-and-drop interface for creating and connecting nodes
- Property panels for configuring distribution parameters
- Visual distinction between random variables and deterministic functions
- Real-time validation with visual feedback for errors
- Zooming and panning for navigating complex models
- Hierarchical grouping for organizing model components

For example, a researcher could create a tree node with a Yule distribution, connect it to a birth rate parameter node, and then connect both to a PhyloCTMC node to represent the sequence evolution model.

### Template-based builders

Template-based builders provide pre-configured model templates (e.g., HKY+Gamma, birth-death with relaxed clock) that users can customize by adjusting parameters and adding or removing components.

These builders might offer:
- A library of standard models for common analyses
- Wizards that guide users through model construction
- Options to customize key aspects of the template
- The ability to save customized templates for reuse

For instance, a user might select an "HKY+Γ" template, customize the prior distributions on the kappa and gamma shape parameters, and then export the resulting Codephy model.

### Interactive visualization

Interactive visualization enhances the understanding of model structure by presenting different views, such as a hierarchical tree view showing parent-child relationships or a dependency graph highlighting parameter interactions.

Useful visualization features include:
- Multiple view types (graph, tree, list) for different perspectives
- Filtering options to focus on specific model components
- Highlighting of paths between related variables
- Expandable/collapsible sections for managing complexity
- Search functionality for finding specific parameters

These graphical tools can generate Codephy-compliant JSON behind the scenes, allowing users to focus on the conceptual model rather than syntax details.

## Domain-specific languages

Domain-specific languages (DSLs) provide a more concise and readable alternative to raw JSON while maintaining programmatic capabilities.

```
// Example of a potential DSL for Codephy models
model PrimatePhylogeny {
  // Random variables
  kappaParam ~ LogNormal(meanlog=1.0, sdlog=0.5);
  baseFreqParam ~ Dirichlet(alpha=[5, 5, 5, 5]);
  birthRateParam ~ LogNormal(meanlog=1.0, sdlog=0.5);
  
  // Tree prior
  tree ~ Yule(birthRate=birthRateParam);
  
  // Substitution model
  substitutionModel = hky(kappa=kappaParam, baseFrequencies=baseFreqParam);
  
  // Observed data
  alignment ~ PhyloCTMC(tree=tree, Q=substitutionModel) {
    observe = [
      {taxon: "TaxonA", sequence: "ACGT"},
      {taxon: "TaxonB", sequence: "ACGA"}
    ]
  }
}
```

Such a DSL can be transpiled to Codephy JSON for interoperability while offering several advantages:

- More natural syntax for specifying distributions and functions
- Concise representation of complex relationships
- Ability to include inline comments explaining model choices
- Support for code organization features like modules or imports

A DSL can be implemented with tools like ANTLR, which generates parsers from grammar specifications, or using existing language infrastructure like the TypeScript compiler API.

## Interactive notebooks and scripting interfaces

Notebook environments like Jupyter provide an interactive way to construct models programmatically while exploring data and visualizing results.

```python
# Python interface example
import codephy

# Create random variables
kappa = codephy.random_variable("kappaParam", 
                               codephy.LogNormal(1.0, 0.5))
base_freq = codephy.random_variable("baseFreqParam", 
                                   codephy.Dirichlet([5, 5, 5, 5]))
birth_rate = codephy.random_variable("birthRateParam",
                                    codephy.LogNormal(1.0, 0.5))

# Create tree prior
tree = codephy.random_variable("tree",
                              codephy.Yule(birthRate=birth_rate))

# Create substitution model
sub_model = codephy.deterministic_function(
    "substitutionModel",
    "hky",
    {"kappa": kappa, "baseFrequencies": base_freq}
)

# Load observed alignment from file
alignment_data = codephy.load_alignment("primates.fasta")

# Create the PhyloCTMC with observed data
alignment = codephy.random_variable(
    "alignment",
    codephy.PhyloCTMC(tree=tree, Q=sub_model),
    observed_value=alignment_data
)

# Build complete model and export to JSON
model = codephy.model([kappa, base_freq, birth_rate, tree, alignment],
                     [sub_model])
model.to_json("primate_model.json")
```

Scripting interfaces provide the flexibility of programmatic model construction while abstracting away JSON details, enabling:

- Integration with data preprocessing workflows
- Parameterized model generation (e.g., for simulation studies)
- Batch creation of related models
- Integration with analysis and visualization tools

These approaches are particularly valuable for researchers who are comfortable with programming and want to automate model creation or integrate it into larger analysis pipelines.

## Web-based collaborative platforms

Web platforms can enhance collaboration and model sharing through features such as:

### Model repositories

Repositories allow researchers to:
- Search for models based on metadata (tags, authors, etc.)
- Browse models by type, application, or popularity
- View model details and dependencies
- Download models in Codephy format
- Track citation and usage statistics

### Version control

Version control systems for models enable:
- Tracking changes to models over time
- Comparing different versions of a model
- Rolling back to previous versions
- Branching models for experimental variations
- Merging improvements from different researchers

### Collaborative editing

Collaborative platforms might support:
- Real-time collaboration on model construction
- Comments and discussions attached to model components
- Role-based access control (viewer, editor, admin)
- Notifications for model changes
- Review processes for model validation

### Model documentation

Documentation features could include:
- Rich text descriptions with mathematical notation
- Interactive visualizations of model structure
- Citations and references to related literature
- Usage examples and tutorials
- Performance and convergence reports

### Integration with data repositories

Integration with data sources allows:
- Direct access to sequence data repositories
- Automatic metadata extraction from data sources
- Dataset versioning linked to model versions
- Standardized data preprocessing workflows
- Data provenance tracking

These platforms could implement various interface approaches (graphical, DSL, or scripting) while handling the underlying Codephy representation, metadata management, and interoperability with inference engines.

## API approaches for programmatic access

Programming libraries in languages commonly used for scientific computing provide APIs for creating and manipulating Codephy models:

### Builder patterns

Builder patterns provide a fluent interface for model construction:

```java
// Java example with builder pattern
CodephyModel model = new CodephyModelBuilder()
    .name("myModel")
    .version("0.1")
    .addRandomVariable(
        new RandomVariableBuilder("kappaParam")
            .distribution(
                new LogNormalDistribution(1.0, 0.5)
            )
            .build()
    )
    // Add more components
    .build();

// Export to JSON
String json = model.toJson();
```

### Object-relational mapping

ORM-like approaches map between Codephy JSON and native language objects:

```python
# Python example with ORM-like mapping
from codephy.orm import CodephyModel, RandomVariable, LogNormal

# Create a model
model = CodephyModel(name="myModel")

# Add random variables
kappa = RandomVariable(
    name="kappaParam",
    distribution=LogNormal(meanlog=1.0, sdlog=0.5)
)
model.add_random_variable(kappa)

# Save to file
model.save("model.json")

# Load from file
loaded_model = CodephyModel.load("model.json")
```

### Validation middleware

Validation can be integrated directly into the API:

```javascript
// JavaScript example with validation middleware
const model = new CodephyModel();

// Add a random variable with validation
try {
  model.addRandomVariable({
    name: "kappaParam",
    distribution: {
      type: "LogNormal",
      meanlog: -5,  // Invalid negative value
      sdlog: 0.5
    }
  });
} catch (error) {
  console.error(`Validation error: ${error.message}`);
}
```

### Conversion utilities

Libraries can provide tools for converting between Codephy and engine-specific formats:

```python
# Python example of format conversion
import codephy.converters as converters

# Load a Codephy model
model = CodephyModel.load("model.json")

# Convert to BEAST XML
beast_xml = converters.to_beast(model)
with open("model.xml", "w") as f:
    f.write(beast_xml)

# Convert to RevBayes script
revbayes_script = converters.to_revbayes(model)
with open("model.Rev", "w") as f:
    f.write(revbayes_script)
```

These programmatic approaches are especially valuable for advanced users, integration with existing software, and automated model generation.

## Model visualizers and explorers

Beyond model construction, specialized tools can help researchers understand and explore existing models:

### Interactive dependency graphs

Interactive graphs can show the relationships between variables:

- Nodes represent random variables and deterministic functions
- Edges represent dependencies between components
- Color coding indicates variable types or states
- Hovering shows detailed information about components
- Clicking expands nested structures or shows properties

### Distribution visualizers

Distribution visualizers can display prior distributions and their parameters:

- Interactive plots of probability density functions
- Adjustable parameters with real-time updates
- Comparison of different distribution types
- Visualization of hierarchical relationships
- Export of high-quality figures for publications

### Parameter sensitivity analysis

Sensitivity analysis tools help explore model behavior:

- Interactive sliders for parameter values
- Real-time updates of dependent calculations
- Correlation plots between parameters
- Impact analysis of parameter changes
- Flagging of sensitive or unstable parameters

### Comparison tools

Comparison tools highlight differences between model versions:

- Side-by-side visual comparison
- Highlighting of added, removed, or changed components
- Diff views of the underlying JSON
- Parameter value comparisons
- Metrics for model complexity and structure

These exploration tools help researchers gain insight into their models, explain them to others, and make informed decisions about model refinement.

## Design principles for Codephy tooling

Effective tools for the Codephy ecosystem should adhere to the following principles:

### Separation of concerns

Maintain the conceptual separation of random variables and deterministic functions. 
Tools should reinforce this distinction through visual design, organization, and terminology.

### Progressive disclosure

Allow users to start with simple models and progressively add complexity as needed. 
Interfaces should hide advanced features until they're relevant, while making them accessible when required.

### Consistent abstractions

Ensure that interface elements directly map to Codephy concepts. 
The relationship between the interface and the underlying model should be clear and consistent.

### Structural validation

Provide immediate feedback on model correctness during construction. 
Validation should be integrated into the interface, highlighting errors and suggesting fixes.

### Transparent representation

Allow users to view and optionally edit the underlying JSON for advanced use cases. 
Tools should never hide the model structure from users who want to understand it.

### Interoperability focus

Emphasize connections to inference engines and data sources. 
Tools should facilitate the movement of models between different systems in the phylogenetic workflow.

### Accessibility

Support users with varying levels of programming expertise. 
Interfaces should be approachable for beginners while remaining powerful for advanced users.

## Implementation strategies

When developing tools for Codephy, consider these implementation strategies:

### Web technologies

Modern web technologies provide a powerful platform for interactive applications:

- **Frontend frameworks** like React, Vue, or Angular for UI components
- **D3.js** or **CytoscapeJS** for interactive graph visualization
- **Monaco Editor** or **CodeMirror** for code editing
- **PlotlyJS** or **ChartJS** for interactive plots
- **WebAssembly** for performance-critical computations

Web-based tools have the advantage of being accessible across different platforms without installation.

### Desktop applications

Desktop applications can offer more performance and integration with local resources:

- **Electron** for cross-platform desktop applications
- **Qt** or **GTK** for native performance
- **Python** with **PyQt** or **Tkinter** for scientific applications
- Integration with local inference engines

Desktop tools are well-suited for computationally intensive tasks or integration with existing scientific software.

### Language bindings

Libraries in scientific programming languages provide programmatic access:

- **Python** libraries for data science integration
- **R** packages for statistical modeling
- **Java** libraries for performance and portability
- **JavaScript/TypeScript** for web integration

These libraries can form the foundation for both programmatic and graphical interfaces.

### Open standards and extensibility

To foster a healthy ecosystem, tools should:

- Use open formats and protocols
- Provide extension points for customization
- Document APIs for interoperability
- Support community contributions
- Follow consistent design patterns

By developing a rich ecosystem of tools around the Codephy format, we can significantly lower the barrier to entry for sophisticated phylogenetic modeling while preserving the benefits of a standardized, portable representation. 
The combination of intuitive interfaces with a well-structured underlying format enables both user-friendly model creation and powerful computational inference.

## Summary

The Codephy format provides a solid foundation for representing phylogenetic models, but user-friendly tools are essential for its adoption and impact. 
By developing a variety of interfaces—from graphical editors to programming libraries—we can make Codephy accessible to a wide range of researchers with different needs and expertise levels.

Key approaches include:
- Graphical model builders with node-based or template-based interfaces
- Domain-specific languages for concise, readable model specification
- Interactive notebooks and scripting for programmatic model creation
- Web-based platforms for collaboration and sharing
- APIs for programmatic access and integration
- Visualization tools for exploring and understanding models

By adhering to clear design principles and leveraging modern implementation strategies, we can create a rich ecosystem of tools that make sophisticated phylogenetic modeling more accessible, efficient, and collaborative.