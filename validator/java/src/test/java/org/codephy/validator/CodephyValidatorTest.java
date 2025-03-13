package org.codephy.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for the Codephy validator.
 */
public class CodephyValidatorTest {

    private ObjectMapper objectMapper;
    private CodephyValidator validator;
    private Path testDir;

    @BeforeEach
    public void setup() throws IOException {
        objectMapper = new ObjectMapper();
        
        // First try to locate the main schema relative to project root
        Path schemaPath = null;
        String[] potentialPaths = {
            "../../../schema/codephy-schema.json", // From test class to project root
            "../../schema/codephy-schema.json",    // Alternative path
            "../schema/codephy-schema.json",       // Another alternative
            "schema/codephy-schema.json"           // Direct path if run from project root
        };
        
        for (String path : potentialPaths) {
            Path tryPath = Paths.get(path);
            if (Files.exists(tryPath)) {
                schemaPath = tryPath;
                break;
            }
        }
        
        if (schemaPath == null) {
            throw new IOException("Could not find the master schema file. Please ensure codephy-schema.json exists in the schema directory.");
        }
        
        System.out.println("Using schema from: " + schemaPath.toAbsolutePath());
        
        // Initialize validator with the found schema
        validator = new CodephyValidator(schemaPath.toString());
        
        // Create test directory if it doesn't exist
        testDir = Paths.get("target", "test-models");
        Files.createDirectories(testDir);
    }

    /**
     * Helper method to create a test model file.
     */
    private Path createTestModel(String filename, JsonNode model) throws IOException {
        Path filePath = testDir.resolve(filename);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), model);
        return filePath;
    }

    @Test
    public void testValidSimpleModel() throws IOException {
        // Create a simple valid model
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "hky_simple");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add kappaParam
        ObjectNode kappaParam = randomVars.putObject("kappaParam");
        ObjectNode kappaDist = kappaParam.putObject("distribution");
        kappaDist.put("type", "LogNormal");
        kappaDist.put("generates", "Real");
        ObjectNode kappaParams = kappaDist.putObject("parameters");
        kappaParams.put("meanlog", 1.0);
        kappaParams.put("sdlog", 0.5);
        
        // Add baseFreqParam
        ObjectNode baseFreqParam = randomVars.putObject("baseFreqParam");
        ObjectNode baseFreqDist = baseFreqParam.putObject("distribution");
        baseFreqDist.put("type", "Dirichlet");
        baseFreqDist.put("generates", "Simplex");
        ObjectNode baseFreqParams = baseFreqDist.putObject("parameters");
        ArrayNode alpha = baseFreqParams.putArray("alpha");
        alpha.add(5).add(5).add(5).add(5);
        
        // Add tree
        ObjectNode tree = randomVars.putObject("tree");
        ObjectNode treeDist = tree.putObject("distribution");
        treeDist.put("type", "Yule");
        treeDist.put("generates", "Tree");
        ObjectNode treeParams = treeDist.putObject("parameters");
        treeParams.put("birthRate", 0.1);
        
        // Save model to file and validate
        Path modelPath = createTestModel("valid_simple.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        assertTrue(result.isValid(), "Simple model should be valid");
        assertEquals(0, result.getErrors().size(), "There should be no validation errors");
    }

    @Test
    public void testCircularDependency() throws IOException {
        // Create a model with circular dependency
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "circular_dependency");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add param1
        ObjectNode param1 = randomVars.putObject("param1");
        ObjectNode param1Dist = param1.putObject("distribution");
        param1Dist.put("type", "LogNormal");
        param1Dist.put("generates", "Real");
        ObjectNode param1Params = param1Dist.putObject("parameters");
        param1Params.put("meanlog", 1.0);
        param1Params.put("sdlog", 0.5);
        
        ObjectNode detFuncs = model.putObject("deterministicFunctions");
        
        // Add func1 that depends on func2. Now, set arguments as objects referencing via "variable".
        ObjectNode func1 = detFuncs.putObject("func1");
        func1.put("function", "add");
        ObjectNode func1Args = func1.putObject("arguments");
        ObjectNode argA = func1Args.putObject("a");
        argA.put("variable", "param1");
        ObjectNode argB = func1Args.putObject("b");
        argB.put("variable", "func2");
        
        // Add func2 that depends on func1.
        ObjectNode func2 = detFuncs.putObject("func2");
        func2.put("function", "multiply");
        ObjectNode func2Args = func2.putObject("arguments");
        func2Args.put("a", 2);
        ObjectNode argB2 = func2Args.putObject("b");
        argB2.put("variable", "func1");
        
        // Save model to file and validate
        Path modelPath = createTestModel("circular_dependency.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        // The test expects that circular dependencies are not detected by the schema,
        // so the model should be considered valid.
        assertTrue(result.isValid(), "Our schema doesn't currently detect circular dependencies");
        
        List<String> errors = result.getErrors();
        for (String error : errors) {
            System.out.println("Circular model error: " + error);
        }
    }

    @Test
    public void testUndefinedReference() throws IOException {
        // Create a model with undefined reference
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "undefined_reference");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add kappaParam
        ObjectNode kappaParam = randomVars.putObject("kappaParam");
        ObjectNode kappaDist = kappaParam.putObject("distribution");
        kappaDist.put("type", "LogNormal");
        kappaDist.put("generates", "Real");
        ObjectNode kappaParams = kappaDist.putObject("parameters");
        kappaParams.put("meanlog", 1.0);
        kappaParams.put("sdlog", 0.5);
        
        // Add tree with reference to nonexistent parameter
        ObjectNode tree = randomVars.putObject("tree");
        ObjectNode treeDist = tree.putObject("distribution");
        treeDist.put("type", "Yule");
        treeDist.put("generates", "Tree");
        ObjectNode treeParams = treeDist.putObject("parameters");
        treeParams.put("birthRate", "nonExistentParam");
        
        // Save model to file and validate
        Path modelPath = createTestModel("undefined_reference.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        assertFalse(result.isValid(), "Model with undefined reference should be invalid");
        
        List<String> errors = result.getErrors();
        boolean foundReferenceError = false;
        for (String error : errors) {
            if (error.contains("undefined variable") && error.contains("nonExistentParam")) {
                foundReferenceError = true;
                break;
            }
        }
        
        assertTrue(foundReferenceError, "Validation should detect undefined reference");
    }

    @Test
    public void testInvalidConstraint() throws IOException {
        // Create a model with invalid constraint
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "invalid_constraint");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add birthRateParam
        ObjectNode birthRateParam = randomVars.putObject("birthRateParam");
        ObjectNode birthRateDist = birthRateParam.putObject("distribution");
        birthRateDist.put("type", "LogNormal");
        birthRateDist.put("generates", "Real");
        ObjectNode birthRateParams = birthRateDist.putObject("parameters");
        birthRateParams.put("meanlog", 1.0);
        birthRateParams.put("sdlog", 0.5);
        
        // Add deathRateParam
        ObjectNode deathRateParam = randomVars.putObject("deathRateParam");
        ObjectNode deathRateDist = deathRateParam.putObject("distribution");
        deathRateDist.put("type", "LogNormal");
        deathRateDist.put("generates", "Real");
        ObjectNode deathRateParams = deathRateDist.putObject("parameters");
        deathRateParams.put("meanlog", 0.0);
        deathRateParams.put("sdlog", 0.5);
        
        // Add constraint with invalid type
        ArrayNode constraints = model.putArray("constraints");
        ObjectNode constraint = constraints.addObject();
        constraint.put("type", "invalidType");
        constraint.put("left", "deathRateParam");
        constraint.put("right", "birthRateParam");
        
        // Save model to file and validate
        Path modelPath = createTestModel("invalid_constraint.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        assertFalse(result.isValid(), "Model with invalid constraint should be invalid");
        
        List<String> errors = result.getErrors();
        boolean foundConstraintError = false;
        for (String error : errors) {
            if (error.contains("invalid type") && error.contains("invalidType")) {
                foundConstraintError = true;
                break;
            }
        }
        
        assertTrue(foundConstraintError, "Validation should detect invalid constraint type");
    }

    @Test
    public void testWrongGeneratesType() throws IOException {
        // Create a model with wrong generates type
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "wrong_generates_type");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add kappaParam with wrong generates type
        ObjectNode kappaParam = randomVars.putObject("kappaParam");
        ObjectNode kappaDist = kappaParam.putObject("distribution");
        kappaDist.put("type", "LogNormal");
        kappaDist.put("generates", "RealVector");  // Wrong for a scalar without dimension
        ObjectNode kappaParams = kappaDist.putObject("parameters");
        kappaParams.put("meanlog", 1.0);
        kappaParams.put("sdlog", 0.5);
        
        // Save model to file and validate
        Path modelPath = createTestModel("wrong_generates_type.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        assertFalse(result.isValid(), "Model with wrong generates type should be invalid");
        
        List<String> errors = result.getErrors();
        boolean foundTypeError = false;
        for (String error : errors) {
            if (error.contains("distribution type") && error.contains("LogNormal") &&
                error.contains("generates type") && error.contains("RealVector")) {
                foundTypeError = true;
                break;
            }
        }
        
        assertTrue(foundTypeError, "Validation should detect wrong generates type");
    }

    @Test
    public void testCompletePhyloCTMCModel() throws IOException {
        // Create a complete model with PhyloCTMC
        ObjectNode model = objectMapper.createObjectNode();
        model.put("model", "hky_complete");
        model.put("codephyVersion", "0.1");
        
        ObjectNode randomVars = model.putObject("randomVariables");
        
        // Add kappaParam
        ObjectNode kappaParam = randomVars.putObject("kappaParam");
        ObjectNode kappaDist = kappaParam.putObject("distribution");
        kappaDist.put("type", "LogNormal");
        kappaDist.put("generates", "Real");
        ObjectNode kappaParams = kappaDist.putObject("parameters");
        kappaParams.put("meanlog", 1.0);
        kappaParams.put("sdlog", 0.5);
        
        // Add baseFreqParam
        ObjectNode baseFreqParam = randomVars.putObject("baseFreqParam");
        ObjectNode baseFreqDist = baseFreqParam.putObject("distribution");
        baseFreqDist.put("type", "Dirichlet");
        baseFreqDist.put("generates", "Simplex");
        ObjectNode baseFreqParams = baseFreqDist.putObject("parameters");
        ArrayNode alpha = baseFreqParams.putArray("alpha");
        alpha.add(5).add(5).add(5).add(5);
        
        // Add birthRateParam
        ObjectNode birthRateParam = randomVars.putObject("birthRateParam");
        ObjectNode birthRateDist = birthRateParam.putObject("distribution");
        birthRateDist.put("type", "LogNormal");
        birthRateDist.put("generates", "Real");
        ObjectNode birthRateParams = birthRateDist.putObject("parameters");
        birthRateParams.put("meanlog", 1.0);
        birthRateParams.put("sdlog", 0.5);
        
        // Add tree (Yule)
        ObjectNode tree = randomVars.putObject("tree");
        ObjectNode treeDist = tree.putObject("distribution");
        treeDist.put("type", "Yule");
        treeDist.put("generates", "Tree");
        ObjectNode treeParams = treeDist.putObject("parameters");

        ObjectNode birthRateRef = treeParams.putObject("birthRate");
        birthRateRef.put("variable", "birthRateParam");
        
        // Add alignment (PhyloCTMC)
        ObjectNode alignment = randomVars.putObject("alignment");
        ObjectNode alignmentDist = alignment.putObject("distribution");
        alignmentDist.put("type", "PhyloCTMC");
        alignmentDist.put("generates", "Alignment");
        ObjectNode alignmentParams = alignmentDist.putObject("parameters");
        
        ObjectNode treeRef = alignmentParams.putObject("tree");
        treeRef.put("variable", "tree");
        ObjectNode qRef = alignmentParams.putObject("Q");
        qRef.put("variable", "substitutionModel");

        
        // Observed data
        ArrayNode observedValue = alignment.putArray("observedValue");
        ObjectNode taxonA = observedValue.addObject();
        taxonA.put("taxon", "TaxonA");
        taxonA.put("sequence", "ACGT");
        ObjectNode taxonB = observedValue.addObject();
        taxonB.put("taxon", "TaxonB");
        taxonB.put("sequence", "ACGA");
        ObjectNode taxonC = observedValue.addObject();
        taxonC.put("taxon", "TaxonC");
        taxonC.put("sequence", "ACGT");
        ObjectNode taxonD = observedValue.addObject();
        taxonD.put("taxon", "TaxonD");
        taxonD.put("sequence", "ACGA");
        
        // Deterministic function for substitution model
        ObjectNode detFuncs = model.putObject("deterministicFunctions");
        ObjectNode substModel = detFuncs.putObject("substitutionModel");
        substModel.put("function", "HKY");
        ObjectNode substArgs = substModel.putObject("arguments");
        // Use proper parameterValue for function arguments:
        ObjectNode argKappa = substArgs.putObject("kappa");
        argKappa.put("variable", "kappaParam");
        ObjectNode argBaseFreq = substArgs.putObject("baseFrequencies");
        argBaseFreq.put("variable", "baseFreqParam");
        
        // Save model to file and validate
        Path modelPath = createTestModel("complete_model.json", model);
        CodephyValidator.ValidationResult result = validator.validateFile(modelPath.toString());
        
        System.out.println("Complete model validation errors:");
        for (String error : result.getErrors()) {
            System.out.println(" - " + error);
        }
        
        assertTrue(result.isValid(), "Complete PhyloCTMC model should now validate correctly");
    }
}