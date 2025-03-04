# Example models

This section provides concrete examples of phylogenetic models specified using the Codephy format. These examples progress from simple to more complex, demonstrating how various aspects of phylogenetic models can be represented.

## Basic model with HKY substitution model

Let's start with a minimal but complete phylogenetic model using the HKY (Hasegawa-Kishino-Yano) substitution model:

```json
{
  "model": "hky_simple",
  "codephyVersion": "0.1",
  
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreqParam": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "birthRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "tree": {
      "distribution": {
        "type": "Yule",
        "generates": "TREE",
        "parameters": {
          "birthRate": "birthRateParam"
        }
      }
    },
    "alignment": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel"
        }
      },
      "observedValue": [
        {"taxon": "TaxonA", "sequence": "ACGT"},
        {"taxon": "TaxonB", "sequence": "ACGA"},
        {"taxon": "TaxonC", "sequence": "ACGT"},
        {"taxon": "TaxonD", "sequence": "ACGA"}
      ]
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

This model includes:
- A transition/transversion rate parameter (`kappaParam`) with a log-normal prior
- Base frequencies (`baseFreqParam`) with a Dirichlet prior
- A birth rate parameter (`birthRateParam`) for the tree prior
- A tree drawn from a Yule process
- An alignment following a PhyloCTMC distribution, observed with four taxa
- A deterministic function creating an HKY substitution model

## Model with rate heterogeneity

Next, let's extend the model to include rate heterogeneity across sites using a discretized gamma distribution:

```json
{
  "model": "hky_gamma",
  "codephyVersion": "0.1",
  
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreqParam": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "gammaShape": {
      "distribution": {
        "type": "Exponential",
        "generates": "REAL",
        "rate": 1.0
      }
    },
    "birthRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "siteRates": {
      "distribution": {
        "type": "Mixture",
        "generates": "REAL_VECTOR",
        "components": [
          {
            "distribution": "Gamma",
            "shape": {
              "variable": "gammaShape"
            },
            "rate": {
              "variable": "gammaShape"
            },
            "discretization": {
              "type": "quantile",
              "categories": 4
            }
          }
        ],
        "weights": [1.0]
      }
    },
    "tree": {
      "distribution": {
        "type": "Yule",
        "generates": "TREE",
        "parameters": {
          "birthRate": "birthRateParam"
        }
      }
    },
    "alignment": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel",
          "siteRates": "siteRates"
        }
      },
      "observedValue": [
        {"taxon": "TaxonA", "sequence": "ACGT"},
        {"taxon": "TaxonB", "sequence": "ACGA"},
        {"taxon": "TaxonC", "sequence": "ACGT"},
        {"taxon": "TaxonD", "sequence": "ACGA"}
      ]
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

This model adds:
- A gamma shape parameter with an exponential prior
- A site rates variable representing a discretized gamma distribution
- The site rates are incorporated into the PhyloCTMC model

## Birth-death model with relaxed clock

Now let's create a more complex model with a birth-death tree prior and a relaxed molecular clock:

```json
{
  "model": "birth_death_relaxed_clock",
  "codephyVersion": "0.1",
  
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreqParam": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "birthRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 0.0,
        "sdlog": 1.0
      }
    },
    "deathRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": -0.5,
        "sdlog": 1.0
      }
    },
    "clockRateMean": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": -7.0,
        "sdlog": 1.0
      }
    },
    "clockRateSD": {
      "distribution": {
        "type": "Exponential",
        "generates": "REAL",
        "rate": 3.0
      }
    },
    "tree": {
      "distribution": {
        "type": "BirthDeath",
        "generates": "TREE",
        "parameters": {
          "birthRate": "birthRateParam",
          "deathRate": "deathRateParam",
          "rootHeight": 10.0
        }
      }
    },
    "branchRates": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL_VECTOR",
        "meanlog": {
          "variable": "clockRateMean"
        },
        "sdlog": {
          "variable": "clockRateSD"
        },
        "dimension": {
          "expression": "getBranchCount(tree)"
        }
      }
    },
    "alignment": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel",
          "branchRates": "branchRates"
        }
      },
      "observedValue": [
        {"taxon": "TaxonA", "sequence": "ACGT"},
        {"taxon": "TaxonB", "sequence": "ACGA"},
        {"taxon": "TaxonC", "sequence": "ACGT"},
        {"taxon": "TaxonD", "sequence": "ACGA"}
      ]
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
  },
  
  "constraints": [
    {
      "type": "lessThan",
      "left": "deathRateParam",
      "right": "birthRateParam"
    }
  ]
}
```

This model includes:
- A birth-death process instead of a Yule process for the tree prior
- A constraint ensuring the death rate is less than the birth rate
- Branch-specific evolutionary rates drawn from a log-normal distribution
- Hyperparameters for the relaxed clock (mean and standard deviation)
- An expression referencing a function to get the number of branches in the tree

## Model with partitioned data

Many analyses involve partitioned data where different regions evolve under different models. Here's an example:

```json
{
  "model": "partitioned_data",
  "codephyVersion": "0.1",
  
  "randomVariables": {
    "kappa1": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "kappa2": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreq1": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "baseFreq2": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "birthRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "relativeRate1": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 0.0,
        "sdlog": 0.5
      }
    },
    "relativeRate2": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 0.0,
        "sdlog": 0.5
      }
    },
    "tree": {
      "distribution": {
        "type": "Yule",
        "generates": "TREE",
        "parameters": {
          "birthRate": "birthRateParam"
        }
      }
    },
    "alignment1": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel1",
          "rate": "relativeRate1"
        }
      },
      "observedValue": [
        {"taxon": "TaxonA", "sequence": "ACGT"},
        {"taxon": "TaxonB", "sequence": "ACGA"},
        {"taxon": "TaxonC", "sequence": "ACGT"},
        {"taxon": "TaxonD", "sequence": "ACGA"}
      ]
    },
    "alignment2": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel2",
          "rate": "relativeRate2"
        }
      },
      "observedValue": [
        {"taxon": "TaxonA", "sequence": "TGCA"},
        {"taxon": "TaxonB", "sequence": "TGCA"},
        {"taxon": "TaxonC", "sequence": "TGCC"},
        {"taxon": "TaxonD", "sequence": "TGCG"}
      ]
    }
  },
  
  "deterministicFunctions": {
    "substitutionModel1": {
      "function": "hky",
      "arguments": {
        "kappa": "kappa1",
        "baseFrequencies": "baseFreq1"
      }
    },
    "substitutionModel2": {
      "function": "hky",
      "arguments": {
        "kappa": "kappa2",
        "baseFrequencies": "baseFreq2"
      }
    },
    "normalizedRates": {
      "function": "normalize",
      "arguments": {
        "values": ["relativeRate1", "relativeRate2"]
      }
    }
  }
}
```

This model features:
- Two separate alignments with their own parameters
- A shared tree between the alignments
- Partition-specific substitution models
- Relative rates for each partition
- A normalization function to ensure rates are properly scaled

## Model with metadata and provenance

Adding metadata and provenance information enhances a model's reproducibility:

```json
{
  "model": "primate_phylogeny",
  "codephyVersion": "0.1",
  
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
  },
  
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
  },
  
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreqParam": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "birthRateParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "tree": {
      "distribution": {
        "type": "Yule",
        "generates": "TREE",
        "parameters": {
          "birthRate": "birthRateParam"
        }
      }
    },
    "alignment": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel"
        }
      },
      "observedValue": [
        {"taxon": "Human", "sequence": "ACGT"},
        {"taxon": "Chimp", "sequence": "ACGA"},
        {"taxon": "Gorilla", "sequence": "ACGT"},
        {"taxon": "Orangutan", "sequence": "ACGA"}
      ]
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

This model includes comprehensive metadata:
- Detailed authorship information with ORCID identifiers
- Creation and modification timestamps
- Version information and licensing
- Citations to relevant literature
- Software used to create the model
- A provenance section tracking the model's history
- Information about the source data

## Model with hierarchical priors

Here's an example demonstrating hierarchical priors:

```json
{
  "model": "hierarchical_population_sizes",
  "codephyVersion": "0.1",
  
  "randomVariables": {
    "kappaParam": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 1.0,
        "sdlog": 0.5
      }
    },
    "baseFreqParam": {
      "distribution": {
        "type": "Dirichlet",
        "generates": "REAL_VECTOR",
        "alpha": [5, 5, 5, 5]
      }
    },
    "popSizeMean": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": 0.0,
        "sdlog": 1.0
      }
    },
    "popSizeCV": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": -1.0,
        "sdlog": 0.5
      }
    },
    "popSize": {
      "distribution": {
        "type": "LogNormal",
        "generates": "REAL",
        "meanlog": {
          "variable": "popSizeMean"
        },
        "sdlog": {
          "expression": "popSizeMean * popSizeCV"
        }
      }
    },
    "tree": {
      "distribution": {
        "type": "Coalescent",
        "generates": "TREE",
        "parameters": {
          "populationSize": "popSize"
        }
      }
    },
    "alignment": {
      "distribution": {
        "type": "PhyloCTMC",
        "generates": "ALIGNMENT",
        "parameters": {
          "tree": "tree",
          "Q": "substitutionModel"
        }
      },
      "observedValue": [
        {"taxon": "Sample1", "sequence": "ACGT"},
        {"taxon": "Sample2", "sequence": "ACGA"},
        {"taxon": "Sample3", "sequence": "ACGT"},
        {"taxon": "Sample4", "sequence": "ACGA"}
      ]
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

This model illustrates:
- Hyperparameters for a population size parameter
- A variable reference using the `variable` field
- An expression combining multiple parameters
- A coalescent tree prior instead of a birth process

## Summary

These examples demonstrate various ways to represent common phylogenetic models in the Codephy format. Key points to note:

1. The separation between random variables and deterministic functions is maintained throughout
2. Models can range from simple to complex while following the same basic structure
3. Different tree priors (Yule, birth-death, coalescent) can be specified
4. Hierarchical relationships between parameters can be expressed
5. Different types of constraints can be applied
6. Metadata and provenance information enhance reproducibility

The flexibility of the Codephy format allows it to represent a wide range of phylogenetic models while maintaining a consistent structure. 
This makes it possible to develop general-purpose tools for model creation, validation, and visualization that work across different types of analyses.