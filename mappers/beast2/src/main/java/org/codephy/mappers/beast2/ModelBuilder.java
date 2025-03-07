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
        if (model.has("deterministicFunctions")) {
            JsonNode detFunctions = model.path("deterministicFunctions");
            Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode funcNode = entry.getValue();
                String functionType = funcNode.path("function").asText();
                
                if (functionType.equals("strictClock") || 
                    functionType.equals("relaxedClock") || 
                    functionType.equals("uncorrelatedClock")) {
                    return true;
                }
            }
        }
        
        // Check if PhyloCTMC has a clockRate parameter
        if (model.has("randomVariables")) {
            JsonNode randomVars = model.path("randomVariables");
            Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode varNode = entry.getValue();
                
                JsonNode distNode = varNode.path("distribution");
                String distType = distNode.path("type").asText();
                
                if (distType.equals("PhyloCTMC")) {
                    JsonNode paramsNode = distNode.path("parameters");
                    if (paramsNode.has("clockRate")) {
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
        if (model.has("randomVariables")) {
            JsonNode randomVars = model.path("randomVariables");
            Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode varNode = entry.getValue();
                
                JsonNode distNode = varNode.path("distribution");
                String distType = distNode.path("type").asText();
                
                if (distType.equals("PhyloCTMC")) {
                    alignment = (Alignment) beastObjects.get(name);
                    
                    // Find the tree reference in the PhyloCTMC model
                    JsonNode paramsNode = distNode.path("parameters");
                    if (paramsNode.has("tree")) {
                        String treeRef = Utils.extractVariableReference(paramsNode, "tree");
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
        taxonSet.initByName("alignment", alignment);
        if (tree instanceof Tree) {
            // Set taxon set for the tree using reflection or other method
            // In BEAST 2.7.5, we need to use proper setters instead of direct input access
            ((Tree) tree).setInputValue("taxonset", taxonSet);
        }
        
        // Find substitution model
        SubstitutionModel substModel = null;
        if (model.has("deterministicFunctions")) {
            JsonNode detFunctions = model.path("deterministicFunctions");
            Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode funcNode = entry.getValue();
                
                String functionType = funcNode.path("function").asText();
                if (functionType.equals("hky") || functionType.equals("jc69") || functionType.equals("gtr")) {
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
        
        // Create tree likelihood
        TreeLikelihood treeLikelihood = new TreeLikelihood();
        treeLikelihood.setID("treeLikelihood");
        treeLikelihood.initByName(
            "data", alignment,
            "tree", tree,
            "siteModel", siteModel
        );
        
        beastObjects.put("treeLikelihood", treeLikelihood);
        return treeLikelihood;
    }
    
/**
 * Set up a site model for the likelihood calculation.
 */
private SiteModel setupSiteModel(JsonNode model) throws Exception {
    // Check if we already created a site model
    if (beastObjects.containsKey("siteModel")) {
        return (SiteModel) beastObjects.get("siteModel");
    }
    
    // Find substitution model
    SubstitutionModel substModel = null;
    if (model.has("deterministicFunctions")) {
        JsonNode detFunctions = model.path("deterministicFunctions");
        Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            JsonNode funcNode = entry.getValue();
            
            String functionType = funcNode.path("function").asText();
            if (functionType.equals("hky") || functionType.equals("jc69") || functionType.equals("gtr")) {
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
    
    // Look for siteRates or similar parameters in PhyloCTMC models
    if (model.has("randomVariables")) {
        JsonNode randomVars = model.path("randomVariables");
        Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode varNode = entry.getValue();
            JsonNode distNode = varNode.path("distribution");
            String distType = distNode.path("type").asText();
            
            if (distType.equals("PhyloCTMC")) {
                // Check if PhyloCTMC specifies siteRates parameter
                JsonNode paramsNode = distNode.path("parameters");
                if (paramsNode.has("siteRates")) {
                    useGamma = true;
                    break;
                }
            }
        }
    }
    
    // Create site model
    SiteModel siteModel = new SiteModel();
    siteModel.setID("siteModel");
    
    if (useGamma) {
        // Create gamma shape parameter for rate heterogeneity
        RealParameter shapeParameter = new RealParameter();
        shapeParameter.setID("gammaShape");
        shapeParameter.initByName("value", "0.5", "lower", "0.0", "upper", "1000.0");
        
        // Set up site model with gamma rate heterogeneity
        siteModel.initByName(
            "substModel", substModel,
            "gammaCategoryCount", 4,
            "shape", shapeParameter,
            "proportionInvariant", "0.0"
        );
        
        // Store the shape parameter
        beastObjects.put("gammaShape", shapeParameter);
    } else {
        // Set up site model without gamma rate heterogeneity
        siteModel.initByName(
            "substModel", substModel,
            "gammaCategoryCount", 1,  // No rate heterogeneity
            "proportionInvariant", "0.0"
        );
    }
    
    beastObjects.put("siteModel", siteModel);
    return siteModel;
}
    
    /**
     * Set up a clock model only if explicitly needed.
     */
    private StrictClockModel setupClockModel(JsonNode model) throws Exception {
        // Check if we already created a clock model
        if (beastObjects.containsKey("clockModel")) {
            return (StrictClockModel) beastObjects.get("clockModel");
        }
        
        // Create clock rate parameter
        RealParameter clockRate = new RealParameter();
        clockRate.setID("clockRate");
        clockRate.initByName("value", "1.0", "lower", "0.0");
        
        // Create strict clock model
        StrictClockModel clockModel = new StrictClockModel();
        clockModel.setID("clockModel");
        clockModel.initByName("clock.rate", clockRate);
        
        beastObjects.put("clockRate", clockRate);
        beastObjects.put("clockModel", clockModel);
        
        return clockModel;
    }
    
    /**
     * Set up the prior distribution.
     */
    private CompoundDistribution setupPrior(JsonNode model, TreeLikelihood treeLikelihood) throws Exception {
        // Create a compound distribution for the prior
        CompoundDistribution prior = new CompoundDistribution();
        prior.setID("prior");
        
        // Collect all prior distributions
        List<Distribution> priors = new ArrayList<>();
        
        // Add all tree priors (from TreeDistributionsMapper)
        for (String key : beastObjects.keySet()) {
            if (key.endsWith("Prior") && beastObjects.get(key) instanceof Distribution) {
                priors.add((Distribution) beastObjects.get(key));
            }
        }
        
        // Add all parameter priors (from StandardDistributionsMapper)
        for (BEASTInterface obj : beastObjects.values()) {
            if (obj instanceof Distribution && !priors.contains(obj) && 
                !obj.getID().equals("likelihood") && !obj.getID().equals("posterior")) {
                priors.add((Distribution) obj);
            }
        }
        
        prior.initByName("distribution", priors);
        beastObjects.put("prior", prior);
        
        return prior;
    }
    
    /**
     * Set up the likelihood distribution.
     */
    private CompoundDistribution setupLikelihood(JsonNode model, TreeLikelihood treeLikelihood) throws Exception {
        // Create a compound distribution for the likelihood
        CompoundDistribution likelihood = new CompoundDistribution();
        likelihood.setID("likelihood");
        
        // Add tree likelihood
        List<Distribution> likelihoods = new ArrayList<>();
        likelihoods.add(treeLikelihood);
        
        likelihood.initByName("distribution", likelihoods);
        beastObjects.put("likelihood", likelihood);
        
        return likelihood;
    }
    
    /**
     * Set up the posterior distribution.
     */
    private CompoundDistribution setupPosterior(JsonNode model, CompoundDistribution prior, 
                                              CompoundDistribution likelihood) throws Exception {
        // Create a compound distribution for the posterior
        CompoundDistribution posterior = new CompoundDistribution();
        posterior.setID("posterior");
        
        // Add prior and likelihood
        List<Distribution> distributions = new ArrayList<>();
        distributions.add(prior);
        distributions.add(likelihood);
        
        posterior.initByName("distribution", distributions);
        beastObjects.put("posterior", posterior);
        
        return posterior;
    }
    
    /**
     * Set up the state object to be sampled.
     */
    private State setupState(JsonNode model) throws Exception {
        // Create a state object
        State state = new State();
        state.setID("state");
        
        // Add all state nodes (parameters and trees)
        List<StateNode> stateNodes = new ArrayList<>();
        
        for (BEASTInterface obj : beastObjects.values()) {
            // Skip clock rate if we're not using a clock
            if (obj instanceof StateNode && 
                (!obj.getID().equals("clockRate") || useStrictClock)) {
                stateNodes.add((StateNode) obj);
            }
        }
        
        state.initByName("stateNode", stateNodes);
        beastObjects.put("state", state);
        
        return state;
    }
    
    /**
     * Set up MCMC operators.
     */
    private List<Operator> setupOperators(JsonNode model, State state) throws Exception {
        List<Operator> operators = new ArrayList<>();
        
        // Make a defensive copy of the values to avoid ConcurrentModificationException
        List<BEASTInterface> objectsCopy = new ArrayList<>(beastObjects.values());
        
        // Set up operators for parameters
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
                    deltaOperator.initByName("parameter", param, "weight", 1.0);
                    operators.add(deltaOperator);
                    beastObjects.put(paramID + "Operator", deltaOperator);
                } else {
                    // Use Scale operator for scalar parameters
                    ScaleOperator operator = new ScaleOperator();
                    operator.setID(paramID + "Operator");
                    operator.initByName("parameter", param, "weight", 1.0);
                    operators.add(operator);
                    beastObjects.put(paramID + "Operator", operator);
                }
            }
        }
        
        // Set up operators for trees
        for (BEASTInterface obj : objectsCopy) {
            if (obj instanceof Tree) {
                Tree tree = (Tree) obj;
                String treeID = tree.getID();
                
                // SubtreeSlide operator
                SubtreeSlide subtreeSlide = new SubtreeSlide();
                subtreeSlide.setID(treeID + "SubtreeSlide");
                subtreeSlide.initByName("tree", tree, "weight", 5.0);
                operators.add(subtreeSlide);
                beastObjects.put(treeID + "SubtreeSlide", subtreeSlide);
                
                // Narrow Exchange operator
                Exchange narrowExchange = new Exchange();
                narrowExchange.setID(treeID + "NarrowExchange");
                narrowExchange.initByName("tree", tree, "weight", 5.0, "isNarrow", true);
                operators.add(narrowExchange);
                beastObjects.put(treeID + "NarrowExchange", narrowExchange);
                
                // Wide Exchange operator
                Exchange wideExchange = new Exchange();
                wideExchange.setID(treeID + "WideExchange");
                wideExchange.initByName("tree", tree, "weight", 3.0, "isNarrow", false);
                operators.add(wideExchange);
                beastObjects.put(treeID + "WideExchange", wideExchange);
                
                // Wilson-Balding operator
                WilsonBalding wilsonBalding = new WilsonBalding();
                wilsonBalding.setID(treeID + "WilsonBalding");
                wilsonBalding.initByName("tree", tree, "weight", 3.0);
                operators.add(wilsonBalding);
                beastObjects.put(treeID + "WilsonBalding", wilsonBalding);
                
                // Tree Scaler operator
                ScaleOperator treeScaler = new ScaleOperator();
                treeScaler.setID(treeID + "TreeScaler");
                treeScaler.initByName("tree", tree, "weight", 3.0, "scaleFactor", 0.95);
                operators.add(treeScaler);
                beastObjects.put(treeID + "TreeScaler", treeScaler);
                
                // Root Height Scaler operator
                ScaleOperator rootHeightScaler = new ScaleOperator();
                rootHeightScaler.setID(treeID + "RootHeightScaler");
                rootHeightScaler.initByName("tree", tree, "weight", 3.0, "scaleFactor", 0.95, "rootOnly", true);
                operators.add(rootHeightScaler);
                beastObjects.put(treeID + "RootHeightScaler", rootHeightScaler);
                
                // Uniform operator (for internal node heights)
                Uniform uniform = new Uniform();
                uniform.setID(treeID + "Uniform");
                uniform.initByName("tree", tree, "weight", 30.0);
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
        // Create MCMC object
        MCMC mcmc = new MCMC();
        mcmc.setID("mcmc");
        
        // Create loggers
        loggers = new ArrayList<>();
        
        // 1. Console logger
        Logger consoleLogger = new Logger();
        consoleLogger.setID("consoleLogger");
        // Add items to log for console
        List<BEASTInterface> consoleLogItems = new ArrayList<>();
        consoleLogItems.add(posterior); // Always log the posterior
        consoleLogger.initByName(
            "logEvery", 1000,
            "log", consoleLogItems
        );
        loggers.add(consoleLogger);
        
        // 2. File logger for parameters
        Logger fileLogger = new Logger();
        fileLogger.setID("fileLogger");
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
            "fileName", "model.log",  // Default name, will be updated by app
            "logEvery", 1000,
            "log", fileLogItems
        );
        loggers.add(fileLogger);
        
        // 3. Tree logger
        Logger treeLogger = new Logger();
        treeLogger.setID("treeLogger");
        // Add trees to log
        List<BEASTInterface> treeLogItems = new ArrayList<>();
        // Find trees to log
        for (BEASTInterface obj : beastObjects.values()) {
            if (obj instanceof Tree) {
                treeLogItems.add(obj);
            }
        }
        treeLogger.initByName(
            "fileName", "model.trees",  // Default name, will be updated by app
            "logEvery", 1000,
            "mode", "tree",
            "log", treeLogItems
        );
        loggers.add(treeLogger);
        
        // Set up chainLength, state, operators, and posterior
        mcmc.initByName(
            "chainLength", Long.valueOf(10000000),
            "state", state,
            "distribution", posterior,
            "operator", operators,
            "logger", loggers
        );
        
        beastObjects.put("mcmc", mcmc);
        
        return mcmc;
    }
    
    /**
     * Update log file paths to use a specific output directory and base name
     */
    public void updateLogFilePaths(String outputDir, String baseName) {
        for (Logger logger : loggers) {
            String loggerId = logger.getID();
            
            if (loggerId.equals("fileLogger")) {
                String newPath = Paths.get(outputDir, baseName + ".log").toString();
                logger.setInputValue("fileName", newPath);
            } else if (loggerId.equals("treeLogger")) {
                String newPath = Paths.get(outputDir, baseName + ".trees").toString();
                logger.setInputValue("fileName", newPath);
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