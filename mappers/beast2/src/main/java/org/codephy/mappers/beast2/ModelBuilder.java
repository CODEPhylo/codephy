package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.Logger;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.operator.DeltaExchangeOperator;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.operator.Uniform;
import beast.base.evolution.operator.WilsonBalding;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.util.Randomizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds a full BEAST2 model from individual components.
 * Updated to use separate Codephy and BEAST2 constants.
 */
public class ModelBuilder {

    private final Map<String, BEASTInterface> beastObjects;
    private boolean useStrictClock = false;
    private List<Logger> loggers = new ArrayList<>();
    
    /**
     * Constructor.
     *
     * @param beastObjects Shared map of created BEAST2 objects
     */
    public ModelBuilder(Map<String, BEASTInterface> beastObjects) {
        this.beastObjects = beastObjects;
    }
    
    /**
     * Build a full BEAST2 model with posterior, prior, likelihood, etc.
     */
    public BEASTInterface buildFullModel(JsonNode model) throws Exception {
        // Check if model explicitly uses a clock
        useStrictClock = doesModelUseClock(model);
        
        // Build tree and alignment objects
        TreeLikelihood treeLikelihood = buildTreeLikelihood(model);
        
        // Create site model if not already created
        SiteModel siteModel = setupSiteModel(model);
        
        // Create clock model if needed
        StrictClockModel clockModel = null;
        if (useStrictClock) {
            clockModel = setupClockModel(model);
        }
        
        // Create prior distributions
        CompoundDistribution prior = setupPrior(model, treeLikelihood);
        
        // Create likelihood distribution
        CompoundDistribution likelihood = setupLikelihood(model, treeLikelihood);
        
        // Create posterior distribution
        CompoundDistribution posterior = setupPosterior(model, prior, likelihood);
        
        // Create state object
        State state = setupState(model);
        
        // Create operators
        List<Operator> operators = setupOperators(model, state);
        
        // Create MCMC object
        MCMC mcmc = setupMCMC(model, posterior, state, operators);
        
        return mcmc;
    }
    
    /**
     * Check if the model explicitly uses a molecular clock
     */
    private boolean doesModelUseClock(JsonNode model) {
        // Check if any of the deterministicFunctions references a clock
        if (model.has(CodephyConstants.FIELD_DET_FUNCTIONS)) {
            JsonNode detFunctions = model.path(CodephyConstants.FIELD_DET_FUNCTIONS);
            Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode funcNode = entry.getValue();
                String functionType = funcNode.path(CodephyConstants.FIELD_FUNCTION).asText();
                
                if (functionType.equals(CodephyConstants.FUNCTION_STRICT_CLOCK) || 
                    functionType.equals(CodephyConstants.FUNCTION_RELAXED_CLOCK) || 
                    functionType.equals(CodephyConstants.FUNCTION_UNCORRELATED_CLOCK)) {
                    return true;
                }
            }
        }
        
        // Check if PhyloCTMC has a rate or branchRates parameter
        if (model.has(CodephyConstants.FIELD_RANDOM_VARS)) {
            JsonNode randomVars = model.path(CodephyConstants.FIELD_RANDOM_VARS);
            Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode varNode = entry.getValue();
                
                JsonNode distNode = varNode.path(CodephyConstants.FIELD_DISTRIBUTION);
                String distType = distNode.path(CodephyConstants.FIELD_TYPE).asText();
                
                if (distType.equals(CodephyConstants.DIST_PHYLOCTMC)) {
                    JsonNode paramsNode = distNode.path(CodephyConstants.FIELD_PARAMETERS);
                    if (paramsNode.has(CodephyConstants.PARAM_RATE) || 
                        paramsNode.has(CodephyConstants.PARAM_BRANCH_RATES)) {
                        return true;
                    }
                }
            }
        }
        
        // Default: do not use clock unless explicitly specified
        return false;
    }
    
    /**
     * Build a TreeLikelihood object from the model.
     */
    private TreeLikelihood buildTreeLikelihood(JsonNode model) throws Exception {
        // Find alignment and tree objects
        Alignment alignment = null;
        TreeInterface tree = null;
        
        // Find PhyloCTMC component (the alignment)
        if (model.has(CodephyConstants.FIELD_RANDOM_VARS)) {
            JsonNode randomVars = model.path(CodephyConstants.FIELD_RANDOM_VARS);
            Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode varNode = entry.getValue();
                
                JsonNode distNode = varNode.path(CodephyConstants.FIELD_DISTRIBUTION);
                String distType = distNode.path(CodephyConstants.FIELD_TYPE).asText();
                
                if (distType.equals(CodephyConstants.DIST_PHYLOCTMC)) {
                    alignment = (Alignment) beastObjects.get(name);
                    
                    // Find the tree reference in the PhyloCTMC model
                    JsonNode paramsNode = distNode.path(CodephyConstants.FIELD_PARAMETERS);
                    if (paramsNode.has(CodephyConstants.PARAM_TREE)) {
                        String treeRef = Utils.extractVariableReference(paramsNode, CodephyConstants.PARAM_TREE);
                        tree = (TreeInterface) beastObjects.get(treeRef);
                    }
                    
                    break;
                }
            }
        }
        
        if (alignment == null) {
            throw new IllegalArgumentException("No alignment found in model");
        }
        
        if (tree == null) {
            throw new IllegalArgumentException("No tree found in model");
        }
        
        // Create taxon set for the tree if not already set
        TaxonSet taxonSet = new TaxonSet();
        taxonSet.initByName(Beast2Constants.INPUT_ALIGNMENT, alignment);
        if (tree instanceof Tree) {
            // Set taxon set for the tree using BEAST2 API
            ((Tree) tree).setInputValue("taxonset", taxonSet);
        }
        
        // Find substitution model
        SubstitutionModel substModel = null;
        if (model.has(CodephyConstants.FIELD_DET_FUNCTIONS)) {
            JsonNode detFunctions = model.path(CodephyConstants.FIELD_DET_FUNCTIONS);
            Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode funcNode = entry.getValue();
                
                String functionType = funcNode.path(CodephyConstants.FIELD_FUNCTION).asText();
                if (functionType.equals(CodephyConstants.FUNCTION_HKY) || 
                    functionType.equals(CodephyConstants.FUNCTION_JC69) || 
                    functionType.equals(CodephyConstants.FUNCTION_GTR)) {
                    substModel = (SubstitutionModel) beastObjects.get(name);
                    break;
                }
            }
        }
        
        if (substModel == null) {
            throw new IllegalArgumentException("No substitution model found in model");
        }
        
        // Create site model
        SiteModel siteModel = setupSiteModel(model);
        
        // Create tree likelihood using BEAST2 API
        TreeLikelihood treeLikelihood = new TreeLikelihood();
        treeLikelihood.setID(Beast2Constants.ID_TREE_LIKELIHOOD);
        treeLikelihood.initByName(
            Beast2Constants.INPUT_DATA, alignment,
            Beast2Constants.INPUT_TREE, tree,
            Beast2Constants.INPUT_SITE_MODEL, siteModel
        );
        
        beastObjects.put(Beast2Constants.ID_TREE_LIKELIHOOD, treeLikelihood);
        return treeLikelihood;
    }
    
    /**
     * Set up a site model for the likelihood calculation.
     */
    private SiteModel setupSiteModel(JsonNode model) throws Exception {
        // Check if we already created a site model
        if (beastObjects.containsKey(Beast2Constants.ID_SITE_MODEL)) {
            return (SiteModel) beastObjects.get(Beast2Constants.ID_SITE_MODEL);
        }
        
        // Find substitution model
        SubstitutionModel substModel = null;
        if (model.has(CodephyConstants.FIELD_DET_FUNCTIONS)) {
            JsonNode detFunctions = model.path(CodephyConstants.FIELD_DET_FUNCTIONS);
            Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode funcNode = entry.getValue();
                
                String functionType = funcNode.path(CodephyConstants.FIELD_FUNCTION).asText();
                if (functionType.equals(CodephyConstants.FUNCTION_HKY) || 
                    functionType.equals(CodephyConstants.FUNCTION_JC69) || 
                    functionType.equals(CodephyConstants.FUNCTION_GTR)) {
                    substModel = (SubstitutionModel) beastObjects.get(name);
                    break;
                }
            }
        }
        
        if (substModel == null) {
            throw new IllegalArgumentException("No substitution model found in model");
        }
        
        // Check if model explicitly specifies gamma rate heterogeneity
        boolean useGamma = false;
        
        // Look for siteRates parameter in PhyloCTMC models
        if (model.has(CodephyConstants.FIELD_RANDOM_VARS)) {
            JsonNode randomVars = model.path(CodephyConstants.FIELD_RANDOM_VARS);
            Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode varNode = entry.getValue();
                JsonNode distNode = varNode.path(CodephyConstants.FIELD_DISTRIBUTION);
                String distType = distNode.path(CodephyConstants.FIELD_TYPE).asText();
                
                if (distType.equals(CodephyConstants.DIST_PHYLOCTMC)) {
                    // Check if PhyloCTMC specifies siteRates parameter
                    JsonNode paramsNode = distNode.path(CodephyConstants.FIELD_PARAMETERS);
                    if (paramsNode.has(CodephyConstants.PARAM_SITE_RATES)) {
                        useGamma = true;
                        break;
                    }
                }
            }
        }
        
        // Create site model using BEAST2 API
        SiteModel siteModel = new SiteModel();
        siteModel.setID(Beast2Constants.ID_SITE_MODEL);
        
        if (useGamma) {
            // Create gamma shape parameter for rate heterogeneity
            RealParameter shapeParameter = new RealParameter();
            shapeParameter.setID(Beast2Constants.ID_GAMMA_SHAPE);
            shapeParameter.initByName(
                Beast2Constants.INPUT_VALUE, "0.5", 
                Beast2Constants.INPUT_LOWER, "0.0", 
                Beast2Constants.INPUT_UPPER, "1000.0"
            );
            
            // Set up site model with gamma rate heterogeneity
            siteModel.initByName(
                Beast2Constants.INPUT_SUBST_MODEL, substModel,
                Beast2Constants.INPUT_GAMMA_CATEGORY_COUNT, 4,
                Beast2Constants.PARAM_SHAPE, shapeParameter,
                Beast2Constants.INPUT_PROPORTION_INVARIANT, "0.0"
            );
            
            // Store the shape parameter
            beastObjects.put(Beast2Constants.ID_GAMMA_SHAPE, shapeParameter);
        } else {
            // Set up site model without gamma rate heterogeneity
            siteModel.initByName(
                Beast2Constants.INPUT_SUBST_MODEL, substModel,
                Beast2Constants.INPUT_GAMMA_CATEGORY_COUNT, 1,  // No rate heterogeneity
                Beast2Constants.INPUT_PROPORTION_INVARIANT, "0.0"
            );
        }
        
        beastObjects.put(Beast2Constants.ID_SITE_MODEL, siteModel);
        return siteModel;
    }
    
    /**
     * Set up a clock model only if explicitly needed.
     */
    private StrictClockModel setupClockModel(JsonNode model) throws Exception {
        // Check if we already created a clock model
        if (beastObjects.containsKey(Beast2Constants.ID_CLOCK_MODEL)) {
            return (StrictClockModel) beastObjects.get(Beast2Constants.ID_CLOCK_MODEL);
        }
        
        // Create clock rate parameter using BEAST2 API
        RealParameter clockRate = new RealParameter();
        clockRate.setID("clockRate");
        clockRate.initByName(
            Beast2Constants.INPUT_VALUE, "1.0", 
            Beast2Constants.INPUT_LOWER, "0.0"
        );
        
        // Create strict clock model using BEAST2 API
        StrictClockModel clockModel = new StrictClockModel();
        clockModel.setID(Beast2Constants.ID_CLOCK_MODEL);
        clockModel.initByName(Beast2Constants.PARAM_CLOCK_RATE, clockRate);
        
        beastObjects.put("clockRate", clockRate);
        beastObjects.put(Beast2Constants.ID_CLOCK_MODEL, clockModel);
        
        return clockModel;
    }
    
/**
 * Set up the prior distribution.
 */
private CompoundDistribution setupPrior(JsonNode model, TreeLikelihood treeLikelihood) throws Exception {
    // Create a compound distribution for the prior using BEAST2 API
    CompoundDistribution prior = new CompoundDistribution();
    prior.setID(Beast2Constants.ID_PRIOR);
    
    // Collect all prior distributions
    List<Distribution> priors = new ArrayList<>();
    
    // Create a map of distribution IDs to random variable names that have observed values
    Map<String, Boolean> hasObservationMap = buildObservationMap(model);
    
    // First, add distributions explicitly marked as priors
    for (String key : beastObjects.keySet()) {
        if (key.endsWith("Prior") && beastObjects.get(key) instanceof Distribution) {
            priors.add((Distribution) beastObjects.get(key));
        }
    }
    
    // Then, add remaining distributions without observed values
    for (BEASTInterface obj : beastObjects.values()) {
        if (obj instanceof Distribution) {
            Distribution dist = (Distribution) obj;
            String id = dist.getID();
            
            // Skip if already added, or if it's likelihood/posterior
            if (priors.contains(dist) || 
                id.equals(Beast2Constants.ID_LIKELIHOOD) || 
                id.equals(Beast2Constants.ID_POSTERIOR)) {
                continue;
            }
            
            // Skip if this distribution corresponds to a random variable with observed data
            if (hasObservationMap.containsKey(id) && hasObservationMap.get(id)) {
                continue;
            }
            
            // Skip TreeLikelihood objects as they should only be in the likelihood
            if (dist instanceof GenericTreeLikelihood) {
                continue;
            }
            
            // Otherwise, add it to the prior
            priors.add(dist);
        }
    }
    
    prior.initByName(Beast2Constants.INPUT_DISTRIBUTION, priors);
    beastObjects.put(Beast2Constants.ID_PRIOR, prior);
    
    return prior;
}

/**
 * Build a map of random variable names to whether they have observed values
 */
private Map<String, Boolean> buildObservationMap(JsonNode model) {
    Map<String, Boolean> observationMap = new HashMap<>();
    
    if (model.has(CodephyConstants.FIELD_RANDOM_VARS)) {
        JsonNode randomVars = model.path(CodephyConstants.FIELD_RANDOM_VARS);
        Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String varName = entry.getKey();
            JsonNode varNode = entry.getValue();
            
            // Check if this random variable has an observedValue
            boolean hasObservation = varNode.has(CodephyConstants.FIELD_OBSERVED);
            observationMap.put(varName, hasObservation);
        }
    }
    
    return observationMap;
}    
    /**
     * Set up the likelihood distribution.
     */
    private CompoundDistribution setupLikelihood(JsonNode model, TreeLikelihood treeLikelihood) throws Exception {
        // Create a compound distribution for the likelihood using BEAST2 API
        CompoundDistribution likelihood = new CompoundDistribution();
        likelihood.setID(Beast2Constants.ID_LIKELIHOOD);
        
        // Add tree likelihood
        List<Distribution> likelihoods = new ArrayList<>();
        likelihoods.add(treeLikelihood);
        
        likelihood.initByName(Beast2Constants.INPUT_DISTRIBUTION, likelihoods);
        beastObjects.put(Beast2Constants.ID_LIKELIHOOD, likelihood);
        
        return likelihood;
    }
    
    /**
     * Set up the posterior distribution.
     */
    private CompoundDistribution setupPosterior(JsonNode model, CompoundDistribution prior, 
                                              CompoundDistribution likelihood) throws Exception {
        // Create a compound distribution for the posterior using BEAST2 API
        CompoundDistribution posterior = new CompoundDistribution();
        posterior.setID(Beast2Constants.ID_POSTERIOR);
        
        // Add prior and likelihood
        List<Distribution> distributions = new ArrayList<>();
        distributions.add(prior);
        distributions.add(likelihood);
        
        posterior.initByName(Beast2Constants.INPUT_DISTRIBUTION, distributions);
        beastObjects.put(Beast2Constants.ID_POSTERIOR, posterior);
        
        return posterior;
    }
    
    /**
     * Set up the state object to be sampled.
     */
    private State setupState(JsonNode model) throws Exception {
        // Create a state object using BEAST2 API
        State state = new State();
        state.setID(Beast2Constants.ID_STATE);
        
        // Add all state nodes (parameters and trees)
        List<StateNode> stateNodes = new ArrayList<>();
        
        for (BEASTInterface obj : beastObjects.values()) {
            // Skip clock rate if we're not using a clock
            // Also skip alignment objects - they should not be state nodes
            if (obj instanceof StateNode && 
                !(obj instanceof Alignment) &&   // Add this check
                (!obj.getID().equals("clockRate") || useStrictClock)) {
                stateNodes.add((StateNode) obj);
            }
        }
        
        state.initByName(Beast2Constants.INPUT_STATE_NODE, stateNodes);
        beastObjects.put(Beast2Constants.ID_STATE, state);
        
        return state;
    }
    
    /**
     * Set up MCMC operators.
     */
    private List<Operator> setupOperators(JsonNode model, State state) throws Exception {
        List<Operator> operators = new ArrayList<>();
        
        // Make a defensive copy of the values to avoid ConcurrentModificationException
        List<BEASTInterface> objectsCopy = new ArrayList<>(beastObjects.values());
        
        // Set up operators for parameters using BEAST2 API
        for (BEASTInterface obj : objectsCopy) {
            if (obj instanceof Parameter) {
                Parameter param = (Parameter) obj;
                String paramID = param.getID();
                
                // Skip clock rate if we're not using a clock
                if (paramID.equals("clockRate") && !useStrictClock) {
                    continue;
                }
                
                if (param.getDimension() > 1) {
                    // Use Delta Exchange operator for multidimensional parameters
                    DeltaExchangeOperator deltaOperator = new DeltaExchangeOperator();
                    deltaOperator.setID(paramID + "Operator");
                    deltaOperator.initByName(
                        Beast2Constants.INPUT_PARAMETER, param, 
                        Beast2Constants.INPUT_WEIGHT, 1.0
                    );
                    operators.add(deltaOperator);
                    beastObjects.put(paramID + "Operator", deltaOperator);
                } else {
                    // Use Scale operator for scalar parameters
                    ScaleOperator operator = new ScaleOperator();
                    operator.setID(paramID + "Operator");
                    operator.initByName(
                        Beast2Constants.INPUT_PARAMETER, param, 
                        Beast2Constants.INPUT_WEIGHT, 1.0
                    );
                    operators.add(operator);
                    beastObjects.put(paramID + "Operator", operator);
                }
            }
        }
        
        // Set up operators for trees using BEAST2 API
        for (BEASTInterface obj : objectsCopy) {
            if (obj instanceof Tree) {
                Tree tree = (Tree) obj;
                String treeID = tree.getID();
                
                // SubtreeSlide operator
                SubtreeSlide subtreeSlide = new SubtreeSlide();
                subtreeSlide.setID(treeID + "SubtreeSlide");
                subtreeSlide.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 5.0);
                operators.add(subtreeSlide);
                beastObjects.put(treeID + "SubtreeSlide", subtreeSlide);
                
                // Narrow Exchange operator
                Exchange narrowExchange = new Exchange();
                narrowExchange.setID(treeID + "NarrowExchange");
                narrowExchange.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 5.0, "isNarrow", true);
                operators.add(narrowExchange);
                beastObjects.put(treeID + "NarrowExchange", narrowExchange);
                
                // Wide Exchange operator
                Exchange wideExchange = new Exchange();
                wideExchange.setID(treeID + "WideExchange");
                wideExchange.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 3.0, "isNarrow", false);
                operators.add(wideExchange);
                beastObjects.put(treeID + "WideExchange", wideExchange);
                
                // Wilson-Balding operator
                WilsonBalding wilsonBalding = new WilsonBalding();
                wilsonBalding.setID(treeID + "WilsonBalding");
                wilsonBalding.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 3.0);
                operators.add(wilsonBalding);
                beastObjects.put(treeID + "WilsonBalding", wilsonBalding);
                
                // Tree Scaler operator
                ScaleOperator treeScaler = new ScaleOperator();
                treeScaler.setID(treeID + "TreeScaler");
                treeScaler.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 3.0, "scaleFactor", 0.95);
                operators.add(treeScaler);
                beastObjects.put(treeID + "TreeScaler", treeScaler);
                
                // Root Height Scaler operator
                ScaleOperator rootHeightScaler = new ScaleOperator();
                rootHeightScaler.setID(treeID + "RootHeightScaler");
                rootHeightScaler.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 3.0, "scaleFactor", 0.95, "rootOnly", true);
                operators.add(rootHeightScaler);
                beastObjects.put(treeID + "RootHeightScaler", rootHeightScaler);
                
                // Uniform operator (for internal node heights)
                Uniform uniform = new Uniform();
                uniform.setID(treeID + "Uniform");
                uniform.initByName(Beast2Constants.INPUT_TREE, tree, Beast2Constants.INPUT_WEIGHT, 30.0);
                operators.add(uniform);
                beastObjects.put(treeID + "Uniform", uniform);
            }
        }
        
        return operators;
    }
    
    /**
     * Set up the MCMC object.
     */
    private MCMC setupMCMC(JsonNode model, CompoundDistribution posterior, 
                         State state, List<Operator> operators) throws Exception {
        // Create MCMC object using BEAST2 API
        MCMC mcmc = new MCMC();
        mcmc.setID(Beast2Constants.ID_MCMC);
        
        // Create loggers
        loggers = new ArrayList<>();
        
        // 1. Console logger
        Logger consoleLogger = new Logger();
        consoleLogger.setID(Beast2Constants.ID_CONSOLE_LOGGER);
        // Add items to log for console
        List<BEASTInterface> consoleLogItems = new ArrayList<>();
        consoleLogItems.add(posterior); // Always log the posterior
        consoleLogger.initByName(
            Beast2Constants.INPUT_LOG_EVERY, 1000,
            Beast2Constants.INPUT_LOG, consoleLogItems
        );
        loggers.add(consoleLogger);
        
        // 2. File logger for parameters
        Logger fileLogger = new Logger();
        fileLogger.setID(Beast2Constants.ID_FILE_LOGGER);
        // Add items to log for file
        List<BEASTInterface> fileLogItems = new ArrayList<>();
        fileLogItems.add(posterior); // Always log the posterior
        // Add all parameters to log
        for (BEASTInterface obj : beastObjects.values()) {
            // Skip clock rate if we're not using a clock
            if (obj instanceof Parameter && !(obj instanceof Tree) && 
                (!obj.getID().equals("clockRate") || useStrictClock)) {
                fileLogItems.add(obj);
            }
        }
        fileLogger.initByName(
            Beast2Constants.INPUT_FILE_NAME, "model.log",  // Default name, will be updated by app
            Beast2Constants.INPUT_LOG_EVERY, 1000,
            Beast2Constants.INPUT_LOG, fileLogItems
        );
        loggers.add(fileLogger);
        
        // 3. Tree logger
        Logger treeLogger = new Logger();
        treeLogger.setID(Beast2Constants.ID_TREE_LOGGER);
        // Add trees to log
        List<BEASTInterface> treeLogItems = new ArrayList<>();
        // Find trees to log
        for (BEASTInterface obj : beastObjects.values()) {
            if (obj instanceof Tree) {
                treeLogItems.add(obj);
            }
        }
        treeLogger.initByName(
            Beast2Constants.INPUT_FILE_NAME, "model.trees",  // Default name, will be updated by app
            Beast2Constants.INPUT_LOG_EVERY, 1000,
            Beast2Constants.INPUT_MODE, "tree",
            Beast2Constants.INPUT_LOG, treeLogItems
        );
        loggers.add(treeLogger);
        
        // Set up chainLength, state, operators, and posterior using BEAST2 API
        mcmc.initByName(
            Beast2Constants.INPUT_CHAIN_LENGTH, Long.valueOf(10000000),
            Beast2Constants.INPUT_STATE, state,
            Beast2Constants.INPUT_DISTRIBUTION, posterior,
            Beast2Constants.INPUT_OPERATOR, operators,
            Beast2Constants.INPUT_LOGGER, loggers
        );
        
        beastObjects.put(Beast2Constants.ID_MCMC, mcmc);
        
        return mcmc;
    }
    
    /**
     * Update log file paths to use a specific output directory and base name
     */
    public void updateLogFilePaths(String outputDir, String baseName) {
        for (Logger logger : loggers) {
            String loggerId = logger.getID();
            
            if (loggerId.equals(Beast2Constants.ID_FILE_LOGGER)) {
                String newPath = Paths.get(outputDir, baseName + ".log").toString();
                logger.setInputValue(Beast2Constants.INPUT_FILE_NAME, newPath);
            } else if (loggerId.equals(Beast2Constants.ID_TREE_LOGGER)) {
                String newPath = Paths.get(outputDir, baseName + ".trees").toString();
                logger.setInputValue(Beast2Constants.INPUT_FILE_NAME, newPath);
            }
        }
    }
    
    /**
     * Get the list of loggers
     */
    public List<Logger> getLoggers() {
        return loggers;
    }
}