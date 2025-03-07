package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;

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
        for (int i = 0; i < observedValue.size(); i++) {
            JsonNode seqNode = observedValue.get(i);
            Sequence sequence = new Sequence();
            sequence.initByName(
                "taxon", seqNode.path("taxon").asText(),
                "value", seqNode.path("sequence").asText()
            );
            sequences.add(sequence);
        }
        
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
        // Create a simple tree to start with, will be connected later
        Tree tree = new Tree();
        tree.setID(name);
        beastObjects.put(name, tree);
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
        // Create a simple tree to start with, will be connected later
        Tree tree = new Tree();
        tree.setID(name);
        beastObjects.put(name, tree);
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
        // Create a simple tree to start with, will be connected later
        Tree tree = new Tree();
        tree.setID(name);
        beastObjects.put(name, tree);
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
        // Create a simple tree to start with, will be connected later
        Tree tree = new Tree();
        tree.setID(name);
        beastObjects.put(name, tree);
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