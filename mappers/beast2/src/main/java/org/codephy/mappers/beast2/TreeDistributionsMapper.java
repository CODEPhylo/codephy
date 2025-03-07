package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.coalescent.RandomTree;
import beast.base.evolution.tree.coalescent.ConstantPopulation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps Codephy tree distributions and phylogenetic models to BEAST2 objects.
 */
public class TreeDistributionsMapper {

    private final Map<String, BEASTInterface> beastObjects;
    
    /**
     * Constructor.
     *
     * @param beastObjects Shared map of created BEAST2 objects
     */
    public TreeDistributionsMapper(Map<String, BEASTInterface> beastObjects) {
        this.beastObjects = beastObjects;
    }
    
    /**
     * Create a BEAST2 object for a tree distribution.
     */
    public void createTreeDistribution(String name, String distType, JsonNode distNode) throws Exception {
        switch (distType) {
            case "Yule":
                createYuleModel(name, distNode);
                break;
                
            case "BirthDeath":
                createBirthDeathModel(name, distNode);
                break;
                
            case "Coalescent":
                createCoalescentModel(name, distNode);
                break;
                
            case "ConstrainedYule":
                createConstrainedYuleModel(name, distNode);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported tree distribution type: " + distType);
        }
    }
    
    /**
     * Connect the parameters of a tree distribution.
     */
    public void connectTreeDistribution(String name, String distType, JsonNode distNode) throws Exception {
        switch (distType) {
            case "Yule":
                connectYuleModel(name, distNode);
                break;
                
            case "BirthDeath":
                connectBirthDeathModel(name, distNode);
                break;
                
            case "Coalescent":
                connectCoalescentModel(name, distNode);
                break;
                
            case "ConstrainedYule":
                connectConstrainedYuleModel(name, distNode);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported tree distribution type: " + distType);
        }
    }
    
    /**
     * Create a PhyloCTMC model for sequence evolution.
     */
    public void createPhyloCTMC(String name, JsonNode distNode, JsonNode observedValue) throws Exception {
        if (observedValue.isMissingNode()) {
            throw new IllegalArgumentException("PhyloCTMC must have an observed value");
        }
        
        // Create alignment from observed sequences
        List<Sequence> sequences = new ArrayList<>();
        List<Taxon> taxonObjects = new ArrayList<>();
        
        for (int i = 0; i < observedValue.size(); i++) {
            JsonNode seqNode = observedValue.get(i);
            String taxonName = seqNode.path("taxon").asText();
            
            // Create Taxon object
            Taxon taxon = new Taxon(taxonName);
            taxonObjects.add(taxon);
            
            // Create Sequence with this taxon
            Sequence sequence = new Sequence();
            sequence.initByName(
                "taxon", taxonName,
                "value", seqNode.path("sequence").asText()
            );
            sequences.add(sequence);
            System.out.println(taxonName + ": " + 
                               seqNode.path("sequence").asText().length() + " " +
                               seqNode.path("sequence").asText().replace("-", "").length());
        }
        
        // Create TaxonSet for later use with trees
        TaxonSet taxonSet = new TaxonSet();
        taxonSet.setID(name + ".taxa");
        taxonSet.initByName("taxon", taxonObjects);
        beastObjects.put(name + ".taxa", taxonSet);
        
        // Create the alignment
        Alignment alignment = new Alignment();
        alignment.setID(name);
        alignment.initByName("sequence", sequences);
        beastObjects.put(name, alignment);
    }
    
    /**
     * Connect the parameters of a PhyloCTMC model.
     */
    public void connectPhyloCTMC(String name, JsonNode distNode) throws Exception {
        // PhyloCTMC connections will be handled by the ModelBuilder
        // when creating the TreeLikelihood
    }
    
/**
 * Create a Yule tree model.
 */
private void createYuleModel(String name, JsonNode distNode) throws Exception {
    // In your model, the PhyloCTMC references the tree, not the other way around
    // We need to check if an alignment exists and use its taxa
    
    System.out.println("Creating tree named: " + name);
    System.out.println("Available objects: " + beastObjects.keySet());
    
    // Get the alignment
    Alignment alignment = null;
    TaxonSet taxonSet = null;
    
    // Check if there's an alignment with taxa that we can use
    if (beastObjects.containsKey("alignment")) {
        alignment = (Alignment) beastObjects.get("alignment");
        System.out.println("Found alignment with " + alignment.getTaxonCount() + " taxa");
        
        // Create a TaxonSet from the alignment if needed
        if (!beastObjects.containsKey("alignment.taxa")) {
            taxonSet = new TaxonSet();
            taxonSet.setID("alignment.taxa");
            taxonSet.initByName("alignment", alignment);
            beastObjects.put("alignment.taxa", taxonSet);
        } else {
            taxonSet = (TaxonSet) beastObjects.get("alignment.taxa");
        }
    }
    
    // Create a properly initialized tree
    if (taxonSet != null) {
        // Create a tree with the taxon set from the alignment
        StringBuilder newick = new StringBuilder();
        newick.append("(");
        for (int i = 0; i < taxonSet.getTaxonCount(); i++) {
            if (i > 0) newick.append(",");
            newick.append(taxonSet.getTaxonId(i));
        }
        newick.append("):1.0;");
        
        System.out.println("Creating tree with Newick: " + newick.toString());
        
        // Parse the tree
        TreeParser parser = new TreeParser();
        parser.setID(name);
        parser.initByName(
            "newick", newick.toString(),
            "IsLabelledNewick", true,
            "adjustTipHeights", false,
            "taxa", taxonSet
        );
        beastObjects.put(name, parser);
        System.out.println("Tree created with " + parser.getLeafNodeCount() + " leaves");
    } else {
        // Create a dummy tree if no alignment is available
        System.out.println("No alignment found, creating dummy tree");
        StringBuilder newick = new StringBuilder();
        newick.append("(taxon1:1.0,taxon2:1.0):1.0;");
        
        TreeParser parser = new TreeParser();
        parser.setID(name);
        parser.initByName(
            "newick", newick.toString(),
            "IsLabelledNewick", true,
            "adjustTipHeights", false
        );
        beastObjects.put(name, parser);
    }
}

public static void updateTreesWithCorrectTaxa(Map<String, BEASTInterface> beastObjects) throws Exception {
    System.out.println("Updating trees with correct taxa");
    
    // For each tree in the beastObjects
    for (String key : new ArrayList<>(beastObjects.keySet())) {
        if (beastObjects.get(key) instanceof Tree) {
            Tree oldTree = (Tree) beastObjects.get(key);
            System.out.println("Checking tree: " + key);
            
            // Find an alignment
            for (String alignmentKey : beastObjects.keySet()) {
                if (beastObjects.get(alignmentKey) instanceof Alignment) {
                    Alignment alignment = (Alignment) beastObjects.get(alignmentKey);
                    System.out.println("Found alignment: " + alignmentKey);
                    
                    // Create a Newick string for a pectinate tree
                    StringBuilder newickBuilder = new StringBuilder();
                    List<String> taxaNames = alignment.getTaxaNames();
                    
                    if (taxaNames.size() > 1) {
                        // Start with the first two taxa
                        newickBuilder.append("(").append(taxaNames.get(0)).append(":0.5,");
                        
                        // Add remaining taxa as a pectinate tree
                        for (int i = 1; i < taxaNames.size() - 1; i++) {
                            newickBuilder.append("(").append(taxaNames.get(i)).append(":0.5,");
                        }
                        
                        // Add the last taxon and close all parentheses
                        newickBuilder.append(taxaNames.get(taxaNames.size() - 1)).append(":0.5");
                        
                        // Close all open parentheses
                        for (int i = 0; i < taxaNames.size() - 1; i++) {
                            newickBuilder.append(")").append(":0.5");
                        }
                    } else if (taxaNames.size() == 1) {
                        // Just one taxon
                        newickBuilder.append(taxaNames.get(0));
                    }
                    
                    // Add the final semicolon
                    newickBuilder.append(";");
                    
                    String newick = newickBuilder.toString();
                    System.out.println("Created newick tree: " + newick);
                    
                    try {
                        // Use a constructor that doesn't require a separate taxon names parameter
                        TreeParser newTree = new TreeParser();
                        newTree.setID(key);
                        newTree.initByName(
                            "newick", newick,
                            "IsLabelledNewick", true,
                            "adjustTipHeights", false,
                            "singlechild", false
                        );
                        
                        // Replace the existing tree
                        beastObjects.put(key, newTree);
                        System.out.println("Tree replaced with " + newTree.getLeafNodeCount() + " leaves");
                    } catch (Exception e) {
                        System.out.println("Failed to create tree: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
    
    /**
     * Connect the Yule tree model.
     */
    private void connectYuleModel(String name, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Get birth rate parameter reference
        String birthRateRef = Utils.extractVariableReference(paramsNode, "birthRate");
        Parameter birthRate = (Parameter) beastObjects.get(birthRateRef);
        
        // Create Yule model
        beast.base.evolution.speciation.YuleModel yule = new beast.base.evolution.speciation.YuleModel();
        yule.setID(name + "Prior");
        yule.initByName("birthDiffRate", birthRate, "tree", beastObjects.get(name));
        beastObjects.put(name + "Prior", yule);
    }
    
    /**
     * Create a Birth-Death tree model.
     */
    private void createBirthDeathModel(String name, JsonNode distNode) throws Exception {
        // Similar to createYuleModel
        String alignmentRef = null;
        JsonNode paramsNode = distNode.path("parameters");
        
        if (paramsNode.has("phyloCTMC")) {
            alignmentRef = Utils.extractVariableReference(paramsNode, "phyloCTMC");
        }
        
        // Create a properly initialized tree
        if (alignmentRef != null && beastObjects.containsKey(alignmentRef + ".taxa")) {
            // Create a tree with the taxon set from the alignment
            TaxonSet taxonSet = (TaxonSet) beastObjects.get(alignmentRef + ".taxa");
            
            // Create a simple star tree using the TaxonSet
            StringBuilder newick = new StringBuilder();
            newick.append("(");
            for (int i = 0; i < taxonSet.getTaxonCount(); i++) {
                if (i > 0) newick.append(",");
                newick.append(taxonSet.getTaxonId(i));
            }
            newick.append("):1.0;");
            
            // Parse the tree
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false,
                "taxa", taxonSet
            );
            beastObjects.put(name, parser);
        } else {
            // Create a dummy tree with minimal taxa
            StringBuilder newick = new StringBuilder();
            newick.append("(taxon1:1.0,taxon2:1.0):1.0;");
            
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false
            );
            beastObjects.put(name, parser);
        }
    }
    
    /**
     * Connect the Birth-Death tree model.
     */
    private void connectBirthDeathModel(String name, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Get parameter references
        String birthRateRef = Utils.extractVariableReference(paramsNode, "birthRate");
        Parameter birthRate = (Parameter) beastObjects.get(birthRateRef);
        
        String deathRateRef = Utils.extractVariableReference(paramsNode, "deathRate");
        Parameter deathRate = (Parameter) beastObjects.get(deathRateRef);
        
        // Create Birth-Death model
        beast.base.evolution.speciation.BirthDeathGernhard08Model birthDeath = 
            new beast.base.evolution.speciation.BirthDeathGernhard08Model();
        birthDeath.setID(name + "Prior");
        
        // Initialize parameters
        double initialBirthRate = 1.0;
        double initialDeathRate = 0.5;
        double relativeDeathRate = initialDeathRate / initialBirthRate;
        
        // In BEAST2, we use birthDiffRate (lambda - mu) and relativeDeathRate (mu/lambda)
        RealParameter relativeDeathRateParam = new RealParameter();
        relativeDeathRateParam.initByName("value", Double.toString(relativeDeathRate));
        
        birthDeath.initByName(
            "birthDiffRate", birthRate,
            "relativeDeathRate", relativeDeathRateParam,
            "tree", beastObjects.get(name)
        );
        
        // Handle optional parameters like root height
        if (paramsNode.has("rootHeight")) {
            // Get root height reference or value
            if (paramsNode.path("rootHeight").isObject() && 
                paramsNode.path("rootHeight").has("variable")) {
                
                String rootHeightRef = paramsNode.path("rootHeight").path("variable").asText();
                Parameter rootHeight = (Parameter) beastObjects.get(rootHeightRef);
                // Set mean in real space parameter
                birthDeath.setInputValue("meanInRealSpace", rootHeight);
            } else if (paramsNode.path("rootHeight").isNumber()) {
                double rootHeight = paramsNode.path("rootHeight").asDouble();
                RealParameter rootHeightParam = new RealParameter();
                rootHeightParam.initByName("value", Double.toString(rootHeight));
                birthDeath.setInputValue("meanInRealSpace", rootHeightParam);
            }
        }
        
        beastObjects.put(name + "Prior", birthDeath);
    }
    
    /**
     * Create a Coalescent tree model.
     */
    private void createCoalescentModel(String name, JsonNode distNode) throws Exception {
        // Similar to createYuleModel
        String alignmentRef = null;
        JsonNode paramsNode = distNode.path("parameters");
        
        if (paramsNode.has("phyloCTMC")) {
            alignmentRef = Utils.extractVariableReference(paramsNode, "phyloCTMC");
        }
        
        // Create a properly initialized tree
        if (alignmentRef != null && beastObjects.containsKey(alignmentRef + ".taxa")) {
            // Create a tree with the taxon set from the alignment
            TaxonSet taxonSet = (TaxonSet) beastObjects.get(alignmentRef + ".taxa");
            
            // Create a simple star tree using the TaxonSet
            StringBuilder newick = new StringBuilder();
            newick.append("(");
            for (int i = 0; i < taxonSet.getTaxonCount(); i++) {
                if (i > 0) newick.append(",");
                newick.append(taxonSet.getTaxonId(i));
            }
            newick.append("):1.0;");
            
            // Parse the tree
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false,
                "taxa", taxonSet
            );
            beastObjects.put(name, parser);
        } else {
            // Create a dummy tree with minimal taxa
            StringBuilder newick = new StringBuilder();
            newick.append("(taxon1:1.0,taxon2:1.0):1.0;");
            
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false
            );
            beastObjects.put(name, parser);
        }
    }
    
    /**
     * Connect the Coalescent tree model.
     */
    private void connectCoalescentModel(String name, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Get population size parameter reference
        String popSizeRef = Utils.extractVariableReference(paramsNode, "populationSize");
        Parameter popSize = (Parameter) beastObjects.get(popSizeRef);
        
        // Create constant population size model
        beast.base.evolution.tree.coalescent.ConstantPopulation popModel = 
            new beast.base.evolution.tree.coalescent.ConstantPopulation();
        popModel.initByName("popSize", popSize);
        
        // Create coalescent model
        beast.base.evolution.tree.coalescent.Coalescent coalescent = 
            new beast.base.evolution.tree.coalescent.Coalescent();
        coalescent.setID(name + "Prior");
        coalescent.initByName("tree", beastObjects.get(name), "populationModel", popModel);
        
        beastObjects.put(name + "Prior", coalescent);
    }
    
    /**
     * Create a Constrained Yule tree model.
     */
    private void createConstrainedYuleModel(String name, JsonNode distNode) throws Exception {
        // Similar to createYuleModel
        String alignmentRef = null;
        JsonNode paramsNode = distNode.path("parameters");
        
        if (paramsNode.has("phyloCTMC")) {
            alignmentRef = Utils.extractVariableReference(paramsNode, "phyloCTMC");
        }
        
        // Create a properly initialized tree
        if (alignmentRef != null && beastObjects.containsKey(alignmentRef + ".taxa")) {
            // Create a tree with the taxon set from the alignment
            TaxonSet taxonSet = (TaxonSet) beastObjects.get(alignmentRef + ".taxa");
            
            // Create a simple star tree using the TaxonSet
            StringBuilder newick = new StringBuilder();
            newick.append("(");
            for (int i = 0; i < taxonSet.getTaxonCount(); i++) {
                if (i > 0) newick.append(",");
                newick.append(taxonSet.getTaxonId(i));
            }
            newick.append("):1.0;");
            
            // Parse the tree
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false,
                "taxa", taxonSet
            );
            beastObjects.put(name, parser);
        } else {
            // Create a dummy tree with minimal taxa
            StringBuilder newick = new StringBuilder();
            newick.append("(taxon1:1.0,taxon2:1.0):1.0;");
            
            TreeParser parser = new TreeParser();
            parser.setID(name);
            parser.initByName(
                "newick", newick.toString(),
                "IsLabelledNewick", true,
                "adjustTipHeights", false
            );
            beastObjects.put(name, parser);
        }
    }
    
    /**
     * Connect the Constrained Yule tree model.
     */
    private void connectConstrainedYuleModel(String name, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Get birth rate parameter reference
        String birthRateRef = Utils.extractVariableReference(paramsNode, "birthRate");
        Parameter birthRate = (Parameter) beastObjects.get(birthRateRef);
        
        // Create Yule model
        beast.base.evolution.speciation.YuleModel yule = new beast.base.evolution.speciation.YuleModel();
        yule.setID(name + "Prior");
        yule.initByName("birthDiffRate", birthRate, "tree", beastObjects.get(name));
        
        // Apply constraints if provided
        if (paramsNode.has("constraints")) {
            JsonNode constraintsNode = paramsNode.path("constraints");
            
            // Handle topology constraints
            if (constraintsNode.has("topology")) {
                JsonNode topologyNode = constraintsNode.path("topology");
                String constraintType = topologyNode.path("type").asText();
                
                if (constraintType.equals("monophyly")) {
                    // Implement monophyly constraint
                    JsonNode taxonSetNode = topologyNode.path("taxonSet");
                    List<String> taxonNames = new ArrayList<>();
                    for (int i = 0; i < taxonSetNode.size(); i++) {
                        taxonNames.add(taxonSetNode.get(i).asText());
                    }
                    
                    // In BEAST2, monophyly constraints are handled during initialization
                    // or through special operators. This is simplified.
                } else if (constraintType.equals("fixed")) {
                    // Handle fixed topology
                    if (topologyNode.has("newick")) {
                        String newick = topologyNode.path("newick").asText();
                        
                        // Replace the tree with a fixed one
                        TreeParser fixedTree = new TreeParser();
                        fixedTree.setID(name);
                        fixedTree.initByName("newick", newick, "IsLabelledNewick", true);
                        
                        // Replace the existing tree
                        beastObjects.put(name, fixedTree);
                        
                        // Update the Yule model to use the fixed tree
                        yule.setInputValue("tree", fixedTree);
                    }
                }
            }
            
            // Handle calibrations
            if (constraintsNode.has("calibrations")) {
                // Not implemented in this simplified version
                // In BEAST2, calibrations are typically handled through special
                // tree priors or distribution objects
            }
        }
        
        beastObjects.put(name + "Prior", yule);
    }
}