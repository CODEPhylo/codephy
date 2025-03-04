# Metadata and provenance

To enhance reproducibility and facilitate model sharing, the Codephy schema includes comprehensive metadata and provenance features. 
This section describes how to use these features to document your models, track their history, and enable proper citation and attribution.

## Why metadata matters

Metadata plays a crucial role in scientific workflows by:

- Providing context about how a model was created and for what purpose
- Enabling proper attribution to model authors
- Facilitating discovery through search and categorization
- Supporting reproducibility through explicit documentation
- Enabling citation in academic publications
- Tracking the evolution of models over time

Models without adequate metadata become less valuable over time as their origin, purpose, and context become unclear.

## Metadata structure

The Codephy schema includes a top-level `metadata` object with fields that support the FAIR (Findable, Accessible, Interoperable, Reusable) principles:

```json
{
  "metadata": {
    "title": "HKY model of primate evolution",
    "description": "A phylogenetic model using HKY substitution model to analyze primate mtDNA",
    "authors": [
      {
        "name": "Jane Smith",
        "email": "jsmith@example.edu",
        "orcid": "0000-0002-1825-0097",
        "affiliation": "University of Phylogenetics"
      }
    ],
    "created": "2024-02-15T14:30:00Z",
    "modified": "2024-03-01T09:15:00Z",
    "version": "1.2",
    "license": "CC-BY-4.0",
    "doi": "10.1234/zenodo.1234567",
    "citations": [
      {
        "doi": "10.1093/sysbio/syy032",
        "text": "Drummond & Bouckaert (2018). Bayesian evolutionary analysis with BEAST."
      }
    ],
    "software": {
      "name": "ModelBuilder",
      "version": "2.1.3",
      "url": "https://example.com/modelbuilder"
    },
    "tags": ["primates", "mtDNA", "HKY"]
  }
}
```

### Core metadata fields

- **title**: A concise name for the model
- **description**: A detailed explanation of the model's purpose, assumptions, and scope
- **authors**: List of contributors with contact information and ORCID identifiers
- **created**: ISO 8601 timestamp indicating when the model was first created
- **modified**: ISO 8601 timestamp indicating when the model was last modified
- **version**: A semantic versioning string for the model itself
- **license**: The license under which the model is shared
- **doi**: Digital Object Identifier for the model, if deposited in a repository
- **citations**: References to publications that should be cited when using this model
- **software**: Information about the software used to generate the model specification
- **tags**: Keywords for classification and discovery

### Author information

The `authors` field is an array of objects, each representing a contributor to the model:

```json
"authors": [
  {
    "name": "Jane Smith",
    "email": "jsmith@example.edu",
    "orcid": "0000-0002-1825-0097",
    "affiliation": "University of Phylogenetics"
  },
  {
    "name": "John Doe",
    "email": "jdoe@example.edu",
    "orcid": "0000-0001-5429-7668",
    "affiliation": "Evolutionary Biology Institute"
  }
]
```

Including ORCID identifiers is highly recommended as they provide a persistent link to the researcher's other works and help resolve name ambiguity.

### Citation information

The `citations` field specifies publications that should be cited when using the model:

```json
"citations": [
  {
    "doi": "10.1093/sysbio/syy032",
    "text": "Drummond & Bouckaert (2018). Bayesian evolutionary analysis with BEAST.",
    "url": "https://academic.oup.com/sysbio/article/67/5/901/4827616"
  },
  {
    "doi": "10.1093/molbev/msy096",
    "text": "Suchard et al. (2018). Bayesian phylogenetic and phylodynamic data integration using BEAST 1.10."
  }
]
```

This helps ensure that original method developers and data providers receive appropriate credit.

## Provenance tracking

Beyond basic metadata, Codephy includes a `provenance` object specifically designed to track the history and derivation of a model:

```json
"provenance": {
  "parentModels": [
    {
      "id": "10.1234/zenodo.1234566",
      "relationship": "derived-from"
    }
  ],
  "changeLog": [
    {
      "version": "1.0",
      "date": "2024-02-15T14:30:00Z",
      "author": "Jane Smith",
      "description": "Initial model creation"
    },
    {
      "version": "1.1",
      "date": "2024-02-28T10:45:00Z",
      "author": "John Doe",
      "description": "Changed prior on kappa parameter from Exponential to LogNormal"
    },
    {
      "version": "1.2",
      "date": "2024-03-01T09:15:00Z",
      "author": "Jane Smith",
      "description": "Added constraint on clock rate"
    }
  ],
  "dataSource": {
    "id": "10.5061/dryad.abc123",
    "description": "Primate mtDNA sequences from Jones et al. (2023)",
    "processingSteps": [
      "Removed sequences with >10% missing data",
      "Aligned using MAFFT v7.490"
    ]
  }
}
```

### Parent models

The `parentModels` field identifies models from which this model is derived:

```json
"parentModels": [
  {
    "id": "10.1234/zenodo.1234566",
    "relationship": "derived-from"
  },
  {
    "id": "10.1234/zenodo.7654321",
    "relationship": "inspired-by"
  }
]
```

The `relationship` field uses controlled vocabulary terms like:
- `derived-from`: Direct modification of the parent model
- `inspired-by`: Created with reference to the parent model
- `extends`: Adds new components to the parent model
- `replicates`: Independently implements the same conceptual model

### Change log

The `changeLog` is a chronological record of modifications to the model:

```json
"changeLog": [
  {
    "version": "1.0",
    "date": "2024-02-15T14:30:00Z",
    "author": "Jane Smith",
    "description": "Initial model creation"
  },
  {
    "version": "1.1",
    "date": "2024-02-28T10:45:00Z",
    "author": "John Doe",
    "description": "Changed prior on kappa parameter from Exponential to LogNormal"
  }
]
```

Each entry includes:
- The version associated with the change
- When the change was made
- Who made the change
- What was changed

This helps track the evolution of the model over time and understand the rationale behind specific modeling choices.

### Data source information

The `dataSource` field documents information about the data used in the model:

```json
"dataSource": {
  "id": "10.5061/dryad.abc123",
  "description": "Primate mtDNA sequences from Jones et al. (2023)",
  "processingSteps": [
    "Removed sequences with >10% missing data",
    "Aligned using MAFFT v7.490"
  ]
}
```

This ensures that the model is linked to its data source and documents any preprocessing steps that were applied to the raw data.

## Benefits for the phylogenetic community

These metadata and provenance features offer several advantages to researchers:

### Reproducibility

Complete information enables precise replication of analyses, addressing a core challenge in computational phylogenetics. By documenting not just the model structure but also its history, data sources, and tools used to create it, we make it possible for others to understand and recreate the entire analysis workflow.

### Model discoverability

Structured metadata facilitates the creation of searchable repositories where researchers can find models relevant to their work. Tags, titles, descriptions, and other metadata fields provide rich information for search and categorization.

### Attribution

Clear authorship and citation information ensures proper academic credit for model developers. By including ORCID identifiers and explicit citation instructions, we make it easier for users to acknowledge the intellectual contributions that went into a model.

### Versioning

The change log provides transparent documentation of model evolution and parameter modifications over time. This helps users understand how and why a model changed, and select the appropriate version for their needs.

### Data lineage

By documenting data sources and preprocessing steps, the provenance section ensures that the relationship between raw data and model inputs is clear. This is crucial for understanding the scope and limitations of a model's applicability.

## Integration with existing standards

The metadata schema is compatible with common standards in scientific data management:

### Dublin Core and DataCite

The core metadata fields align with Dublin Core elements and DataCite schema, making it easy to convert Codephy metadata to these widely used formats for publication and citation.

### ORCID

The inclusion of ORCID identifiers for authors leverages this established system for researcher identification, helping to create persistent links between researchers and their contributions.

### ISO 8601

All timestamps use ISO 8601 format (`YYYY-MM-DDThh:mm:ssZ`), ensuring consistent and unambiguous representation of dates and times across different systems.

### PROV-O

The provenance elements of Codephy are informed by the PROV Ontology (PROV-O), which provides a standard way to represent provenance information. This alignment facilitates integration with broader provenance tracking systems.

## Best practices for metadata

To maximize the benefits of Codephy's metadata capabilities, consider these best practices:

### Be comprehensive

Include all relevant information in your metadata. A common mistake is to provide only minimal information, which reduces the long-term value of the model.

### Keep it current

Update the metadata, especially the `modified` timestamp and change log, whenever you modify your model. This creates a reliable record of the model's evolution.

### Use persistent identifiers

Whenever possible, use DOIs or other persistent identifiers for your model, cited works, and data sources. This ensures that these references remain valid over time.

### Be specific about licensing

Clearly specify the license under which your model is shared. This helps others understand how they can use and build upon your work.

### Document data sources thoroughly

The `dataSource` section is particularly important for reproducibility. Include not just the source, but also any processing steps that were applied.

### Use standardized formats

Follow standard formats for identifiers like DOIs and ORCIDs, and use ISO 8601 for all timestamps. This ensures that your metadata can be correctly parsed and interpreted by different systems.

## Example use case

Consider a research group developing a series of phylogenetic models for a study of primate evolution. Using the metadata and provenance features, they can:

1. Create an initial model with complete authorship information and citations
2. Document the source and preprocessing of their sequence data
3. Track version changes as they refine their model and priors
4. Derive new models from the original, maintaining clear relationships
5. Deposit the final models in a repository with DOIs for citation

This structured approach not only enhances the group's internal workflow but also makes their models more valuable to the broader scientific community, supporting both reuse and methodological transparency.

## Summary

Comprehensive metadata and provenance tracking are essential components of the Codephy format. 
By documenting the who, what, when, why, and how of model creation and evolution, we enhance the scientific value of phylogenetic models and support the principles of open, reproducible research.

The structured approach to metadata in Codephy aligns with established standards in scientific data management while addressing the specific needs of phylogenetic modeling. 
By adopting these practices, researchers can ensure that their models remain valuable and usable well beyond their initial creation and application.