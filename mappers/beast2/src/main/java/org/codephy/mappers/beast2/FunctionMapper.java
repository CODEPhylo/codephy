package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.Parameter;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.evolution.substitutionmodel.GTR;
import beast.base.evolution.substitutionmodel.SubstitutionModel;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Maps Codephy deterministic functions to BEAST2 objects.
 */
public class FunctionMapper {

    private final Map<String, BEASTInterface> beastObjects;
    
    /**
     * Constructor.
     *
     * @param beastObjects Shared map of created BEAST2 objects
     */
    public FunctionMapper(Map<String, BEASTInterface> beastObjects) {
        this.beastObjects = beastObjects;
    }
    
    /**
     * Create a BEAST2 object for a function based on its type.
     */
    public void createFunction(String name, String functionType, JsonNode funcNode) throws Exception {
        switch (functionType) {
            case "hky":
                createHKYModel(name, funcNode);
                break;
                
            case "jc69":
                createJC69Model(name, funcNode);
                break;
                
            case "gtr":
                createGTRModel(name, funcNode);
                break;
                
            case "normalize":
                createNormalizeFunction(name, funcNode);
                break;
                
            case "vectorElement":
                createVectorElementFunction(name, funcNode);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported function type: " + functionType);
        }
    }
    
    /**
     * Connect the parameters of a function based on its type.
     */
    public void connectFunction(String name, String functionType, JsonNode funcNode) throws Exception {
        switch (functionType) {
            case "hky":
                connectHKYModel(name, funcNode);
                break;
                
            case "jc69":
                connectJC69Model(name, funcNode);
                break;
                
            case "gtr":
                connectGTRModel(name, funcNode);
                break;
                
            case "normalize":
                connectNormalizeFunction(name, funcNode);
                break;
                
            case "vectorElement":
                connectVectorElementFunction(name, funcNode);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported function type: " + functionType);
        }
    }
    
    /**
     * Create a HKY substitution model.
     */
    private void createHKYModel(String name, JsonNode funcNode) throws Exception {
        HKY hky = new HKY();
        hky.setID(name);
        beastObjects.put(name, hky);
    }
    
    /**
     * Connect the parameters of a HKY model.
     */
    private void connectHKYModel(String name, JsonNode funcNode) throws Exception {
        JsonNode argsNode = funcNode.path("arguments");
        
        // Get kappa parameter reference
        String kappaRef = Utils.extractVariableReference(argsNode, "kappa");
        Parameter kappa = (Parameter) beastObjects.get(kappaRef);
        
        // Get frequencies parameter reference
        String freqRef = Utils.extractVariableReference(argsNode, "baseFrequencies");
        Parameter freq = (Parameter) beastObjects.get(freqRef);
        
        // Create Frequencies object
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freq);
        
        // Connect to HKY model
        HKY hky = (HKY) beastObjects.get(name);
        hky.initByName("kappa", kappa, "frequencies", frequencies);
    }
    
    /**
     * Create a JC69 substitution model.
     */
    private void createJC69Model(String name, JsonNode funcNode) throws Exception {
        JukesCantor jc = new JukesCantor();
        jc.setID(name);
        beastObjects.put(name, jc);
    }
    
    /**
     * Connect the parameters of a JC69 model.
     */
    private void connectJC69Model(String name, JsonNode funcNode) throws Exception {
        // JC69 doesn't require any parameters, it's already initialized
    }
    
    /**
     * Create a GTR substitution model.
     */
    private void createGTRModel(String name, JsonNode funcNode) throws Exception {
        GTR gtr = new GTR();
        gtr.setID(name);
        beastObjects.put(name, gtr);
    }
    
    /**
     * Connect the parameters of a GTR model.
     */
    private void connectGTRModel(String name, JsonNode funcNode) throws Exception {
        JsonNode argsNode = funcNode.path("arguments");
        
        // Get rate parameters references
        String rateACRef = Utils.extractVariableReference(argsNode, "rateAC");
        String rateAGRef = Utils.extractVariableReference(argsNode, "rateAG");
        String rateATRef = Utils.extractVariableReference(argsNode, "rateAT");
        String rateCGRef = Utils.extractVariableReference(argsNode, "rateCG");
        String rateCTRef = Utils.extractVariableReference(argsNode, "rateCT");
        String rateGTRef = Utils.extractVariableReference(argsNode, "rateGT");
        
        Parameter rateAC = (Parameter) beastObjects.get(rateACRef);
        Parameter rateAG = (Parameter) beastObjects.get(rateAGRef);
        Parameter rateAT = (Parameter) beastObjects.get(rateATRef);
        Parameter rateCG = (Parameter) beastObjects.get(rateCGRef);
        Parameter rateCT = (Parameter) beastObjects.get(rateCTRef);
        Parameter rateGT = (Parameter) beastObjects.get(rateGTRef);
        
        // Get frequencies parameter reference
        String freqRef = Utils.extractVariableReference(argsNode, "baseFrequencies");
        Parameter freq = (Parameter) beastObjects.get(freqRef);
        
        // Create Frequencies object
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freq);
        
        // Connect to GTR model
        GTR gtr = (GTR) beastObjects.get(name);
        gtr.initByName(
            "rateAC", rateAC,
            "rateAG", rateAG,
            "rateAT", rateAT,
            "rateCG", rateCG,
            "rateCT", rateCT,
            "rateGT", rateGT,
            "frequencies", frequencies
        );
    }
    
    /**
     * Create a normalize function.
     * This simulates a function that normalizes a set of values.
     */
    private void createNormalizeFunction(String name, JsonNode funcNode) throws Exception {
        // In BEAST2, normalization is often handled implicitly
        // Here we'd create a placeholder for the normalized values
        // For simplicity, we'll just pass through the original values
        // in this example
        
        // No actual objects to create yet - will be handled in the connection phase
    }
    
    /**
     * Connect the parameters of a normalize function.
     */
    private void connectNormalizeFunction(String name, JsonNode funcNode) throws Exception {
        JsonNode argsNode = funcNode.path("arguments");
        
        // Get the values to normalize
        JsonNode valuesNode = argsNode.path("values");
        
        // In a real implementation, we would create a normalized set of parameters
        // based on the original values. For this simplified example, we'll just
        // create a reference to the original values.
        
        if (valuesNode.isArray()) {
            // If multiple values are provided, we'd normalize them as a group
            // This would involve more sophisticated handling not shown here
        } else {
            // If a single value is provided (e.g., a vector), we'd normalize it
            String valueRef = Utils.extractVariableReference(argsNode, "values");
            BEASTInterface originalValue = beastObjects.get(valueRef);
            
            // In this simplified example, we just create a reference
            beastObjects.put(name, originalValue);
        }
    }
    
    /**
     * Create a vectorElement function to extract an element from a vector.
     */
    private void createVectorElementFunction(String name, JsonNode funcNode) throws Exception {
        // No actual objects to create yet - will be handled in the connection phase
    }
    
    /**
     * Connect the parameters of a vectorElement function.
     */
    private void connectVectorElementFunction(String name, JsonNode funcNode) throws Exception {
        JsonNode argsNode = funcNode.path("arguments");
        
        // Get vector reference
        String vectorRef = Utils.extractVariableReference(argsNode, "vector");
        Parameter vector = (Parameter) beastObjects.get(vectorRef);
        
        // Get index
        int index = argsNode.path("index").asInt();
        
        // In BEAST2, you'd normally create a derived parameter or use indexing
        // For this simplified example, if the vector is a RealParameter, we can
        // create a parameter for the specific index
        
        if (vector instanceof beast.base.inference.parameter.RealParameter) {
            beast.base.inference.parameter.RealParameter realVector = 
                (beast.base.inference.parameter.RealParameter) vector;
            beast.base.inference.parameter.RealParameter element = 
                new beast.base.inference.parameter.RealParameter();
            
            // Extract the value at the specified index
            double value = realVector.getValue(index);
            element.setID(name);
            element.initByName("value", Double.toString(value));
            
            beastObjects.put(name, element);
        }
    }
}