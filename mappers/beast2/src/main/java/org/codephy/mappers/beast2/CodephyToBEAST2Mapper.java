package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.CompoundDistribution;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.TreeInterface;
import beast.base.parser.XMLProducer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Maps Codephy JSON format directly to BEAST2 in-memory objects.
 * This class serves as the central coordinator for the mapping process.
 */
public class CodephyToBEAST2Mapper {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BEASTInterface> beastObjects = new HashMap<>();
    private BEASTInterface posterior;
    
    // Component mappers
    private final DistributionMapper distributionMapper;
    private final FunctionMapper functionMapper;
    private final ModelBuilder modelBuilder;
    
    /**
     * Constructor initializes the component mappers.
     */
    public CodephyToBEAST2Mapper() {
        distributionMapper = new DistributionMapper(beastObjects);
        functionMapper = new FunctionMapper(beastObjects);
        modelBuilder = new ModelBuilder(beastObjects);
    }
    
    /**
     * Convert a Codephy JSON file to BEAST2 objects.
     *
     * @param codephyFile Path to the Codephy JSON file
     * @throws Exception if conversion fails
     */
    public void convertToBEAST2Objects(String codephyFile) throws Exception {
        // Parse JSON model
        JsonNode model = objectMapper.readTree(new File(codephyFile));
        
        // Phase 1: Create all random variables
        createRandomVariables(model);
        
        // Phase 2: Create all deterministic functions
        createDeterministicFunctions(model);
        
        // Phase 3: Connect components and resolve references
        connectComponents(model);
        
        // Phase 4: Build the full model with posterior, likelihood, etc.
        posterior = modelBuilder.buildFullModel(model);
    }
    
    /**
     * Get the constructed posterior object, which contains the full model.
     */
    public BEASTInterface getPosterior() {
        return posterior;
    }
    
    /**
     * Export the model to BEAST2 XML format for verification.
     * 
     * @param outputFile Path to write the XML file
     * @throws Exception if export fails
     */
    public void exportToXML(String outputFile) throws Exception {
        XMLProducer producer = new XMLProducer();
        String xml = producer.toXML(posterior);
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(xml);
        }
    }

    /**
     * Create BEAST2 objects for all random variables.
     */
    private void createRandomVariables(JsonNode model) throws Exception {
        JsonNode randomVars = model.path("randomVariables");
        Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            JsonNode varNode = entry.getValue();
            
            JsonNode distNode = varNode.path("distribution");
            String distType = distNode.path("type").asText();
            String generates = distNode.path("generates").asText();
            
            // Let the distribution mapper handle the specific distribution type
            distributionMapper.createDistribution(name, distType, generates, distNode, varNode);
        }
    }
    
    /**
     * Create BEAST2 objects for all deterministic functions.
     */
    private void createDeterministicFunctions(JsonNode model) throws Exception {
        JsonNode detFunctions = model.path("deterministicFunctions");
        Iterator<Map.Entry<String, JsonNode>> fields = detFunctions.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            JsonNode funcNode = entry.getValue();
            
            String functionType = funcNode.path("function").asText();
            
            // Let the function mapper handle the specific function type
            functionMapper.createFunction(name, functionType, funcNode);
        }
    }
    
    /**
     * Connect components and resolve references.
     */
    private void connectComponents(JsonNode model) throws Exception {
        // Connect random variables
        JsonNode randomVars = model.path("randomVariables");
        Iterator<Map.Entry<String, JsonNode>> fields = randomVars.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            JsonNode varNode = entry.getValue();
            
            JsonNode distNode = varNode.path("distribution");
            String distType = distNode.path("type").asText();
            
            // Let the distribution mapper connect the specific distribution type
            distributionMapper.connectDistribution(name, distType, distNode);
        }
        
        // Connect deterministic functions
        JsonNode detFunctions = model.path("deterministicFunctions");
        fields = detFunctions.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            JsonNode funcNode = entry.getValue();
            
            String functionType = funcNode.path("function").asText();
            
            // Let the function mapper connect the specific function type
            functionMapper.connectFunction(name, functionType, funcNode);
        }
    }
}