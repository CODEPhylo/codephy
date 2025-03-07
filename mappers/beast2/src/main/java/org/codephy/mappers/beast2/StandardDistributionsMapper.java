package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.distribution.Beta;
import beast.base.inference.distribution.Dirichlet;
import beast.base.inference.distribution.Exponential;
import beast.base.inference.distribution.Gamma;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Normal;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps Codephy standard parameter distributions to BEAST2 objects.
 */
public class StandardDistributionsMapper {

    private final Map<String, BEASTInterface> beastObjects;
    
    /**
     * Constructor.
     *
     * @param beastObjects Shared map of created BEAST2 objects
     */
    public StandardDistributionsMapper(Map<String, BEASTInterface> beastObjects) {
        this.beastObjects = beastObjects;
    }
    
    /**
     * Create a BEAST2 object for a standard parameter distribution.
     */
    public void createStandardDistribution(String name, String distType, String generates, JsonNode distNode) throws Exception {
        switch (distType) {
            case "Normal":
                createNormalDistribution(name, generates, distNode);
                break;
                
            case "LogNormal":
                createLogNormalDistribution(name, generates, distNode);
                break;
                
            case "Gamma":
                createGammaDistribution(name, generates, distNode);
                break;
                
            case "Exponential":
                createExponentialDistribution(name, generates, distNode);
                break;
                
            case "Beta":
                createBetaDistribution(name, generates, distNode);
                break;
                
            case "Uniform":
                createUniformDistribution(name, generates, distNode);
                break;
                
            case "Dirichlet":
                createDirichletDistribution(name, generates, distNode);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported distribution type: " + distType);
        }
    }
    
    /**
     * Create a Normal distribution.
     */
    private void createNormalDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double mean = Utils.extractNumericValue(paramsNode, "mean");
        double sigma = Utils.extractNumericValue(paramsNode, "sigma");
        
        // Create parameter
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < dimension; i++) {
                values.add(mean);
            }
            parameter.initByName("value", values);
        } else {
            parameter.initByName("value", Double.toString(mean));
        }
        
        // Create Normal distribution prior
        RealParameter meanParam = new RealParameter();
        meanParam.initByName("value", Double.toString(mean));
        
        RealParameter sigmaParam = new RealParameter();
        sigmaParam.initByName("value", Double.toString(sigma));
        
        Normal normal = new Normal();
        normal.initByName("mean", meanParam, "sigma", sigmaParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", normal);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create a LogNormal distribution.
     */
    private void createLogNormalDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double m = Utils.extractNumericValue(paramsNode, "m");
        double s = Utils.extractNumericValue(paramsNode, "s");
        
        // Create parameter (start with median value)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            double medianValue = Math.exp(m);  // Median of log-normal is exp(m)
            for (int i = 0; i < dimension; i++) {
                values.add(medianValue);
            }
            parameter.initByName("value", values);
        } else {
            double medianValue = Math.exp(m);  // Median of log-normal is exp(m)
            parameter.initByName("value", Double.toString(medianValue));
        }
        
        // Create LogNormal distribution prior
        RealParameter mParam = new RealParameter();
        mParam.initByName("value", Double.toString(m));
        
        RealParameter sParam = new RealParameter();
        sParam.initByName("value", Double.toString(s));
        
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.initByName("M", mParam, "S", sParam, "meanInRealSpace", false);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", logNormal);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create a Gamma distribution.
     */
    private void createGammaDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double alpha = Utils.extractNumericValue(paramsNode, "alpha");
        double beta = Utils.extractNumericValue(paramsNode, "beta");
        
        // Create parameter (start with mean value)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            double meanValue = alpha / beta;  // Mean of gamma is alpha/beta
            for (int i = 0; i < dimension; i++) {
                values.add(meanValue);
            }
            parameter.initByName("value", values);
        } else {
            double meanValue = alpha / beta;  // Mean of gamma is alpha/beta
            parameter.initByName("value", Double.toString(meanValue));
        }
        
        // Create Gamma distribution prior
        RealParameter alphaParam = new RealParameter();
        alphaParam.initByName("value", Double.toString(alpha));
        
        RealParameter betaParam = new RealParameter();
        betaParam.initByName("value", Double.toString(beta));
        
        Gamma gamma = new Gamma();
        gamma.initByName("alpha", alphaParam, "beta", betaParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", gamma);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create an Exponential distribution.
     */
    private void createExponentialDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double lambda = Utils.extractNumericValue(paramsNode, "lambda");
        
        // Create parameter (start with mean value)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            double meanValue = 1.0 / lambda;  // Mean of exponential is 1/lambda
            for (int i = 0; i < dimension; i++) {
                values.add(meanValue);
            }
            parameter.initByName("value", values);
        } else {
            double meanValue = 1.0 / lambda;  // Mean of exponential is 1/lambda
            parameter.initByName("value", Double.toString(meanValue));
        }
        
        // Create Exponential distribution prior
        RealParameter lambdaParam = new RealParameter();
        lambdaParam.initByName("value", Double.toString(lambda));
        
        Exponential exponential = new Exponential();
        exponential.initByName("mean", lambdaParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", exponential);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create a Beta distribution.
     */
    private void createBetaDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double alpha = Utils.extractNumericValue(paramsNode, "alpha");
        double beta = Utils.extractNumericValue(paramsNode, "beta");
        
        // Create parameter (start with mean value)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            double meanValue = alpha / (alpha + beta);  // Mean of beta is alpha/(alpha+beta)
            for (int i = 0; i < dimension; i++) {
                values.add(meanValue);
            }
            parameter.initByName("value", values, "lower", "0.0", "upper", "1.0");
        } else {
            double meanValue = alpha / (alpha + beta);  // Mean of beta is alpha/(alpha+beta)
            parameter.initByName("value", Double.toString(meanValue), "lower", "0.0", "upper", "1.0");
        }
        
        // Create Beta distribution prior
        RealParameter alphaParam = new RealParameter();
        alphaParam.initByName("value", Double.toString(alpha));
        
        RealParameter betaParam = new RealParameter();
        betaParam.initByName("value", Double.toString(beta));
        
        Beta betaDist = new Beta();
        betaDist.initByName("alpha", alphaParam, "beta", betaParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", betaDist);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create a Uniform distribution.
     */
    private void createUniformDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract parameters
        double lower = Utils.extractNumericValue(paramsNode, "lower");
        double upper = Utils.extractNumericValue(paramsNode, "upper");
        
        // Create parameter (start with middle value)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Determine if we need a vector parameter
        int dimension = Utils.extractDimension(paramsNode);
        if (dimension > 1) {
            // Create a vector parameter with the same value repeated
            List<Double> values = new ArrayList<>();
            double middleValue = (lower + upper) / 2.0;  // Middle of uniform range
            for (int i = 0; i < dimension; i++) {
                values.add(middleValue);
            }
            parameter.initByName("value", values, "lower", Double.toString(lower), "upper", Double.toString(upper));
        } else {
            double middleValue = (lower + upper) / 2.0;  // Middle of uniform range
            parameter.initByName("value", Double.toString(middleValue), "lower", Double.toString(lower), "upper", Double.toString(upper));
        }
        
        // Create Uniform distribution prior
        RealParameter lowerParam = new RealParameter();
        lowerParam.initByName("value", Double.toString(lower));
        
        RealParameter upperParam = new RealParameter();
        upperParam.initByName("value", Double.toString(upper));
        
        Uniform uniform = new Uniform();
        uniform.initByName("lower", lowerParam, "upper", upperParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", uniform);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
    
    /**
     * Create a Dirichlet distribution.
     */
    private void createDirichletDistribution(String name, String generates, JsonNode distNode) throws Exception {
        JsonNode paramsNode = distNode.path("parameters");
        
        // Extract alpha parameter (vector of concentrations)
        List<Double> alphaValues = new ArrayList<>();
        JsonNode alphaNode = paramsNode.path("alpha");
        
        if (alphaNode.isArray()) {
            for (int i = 0; i < alphaNode.size(); i++) {
                alphaValues.add(alphaNode.get(i).asDouble());
            }
        } else {
            // If alpha is not an array, assume it's a scalar to be repeated
            double alpha = alphaNode.asDouble();
            
            // Determine the dimension
            int dimension = Utils.extractDimension(paramsNode);
            if (dimension <= 1) {
                dimension = 4;  // Default to 4 dimensions if not specified
            }
            
            for (int i = 0; i < dimension; i++) {
                alphaValues.add(alpha);
            }
        }
        
        // Create parameter (start with normalized alpha values)
        RealParameter parameter = new RealParameter();
        parameter.setID(name);
        
        // Normalize the alpha values to create a probability vector
        double sum = 0.0;
        for (Double a : alphaValues) {
            sum += a;
        }
        
        List<Double> normalizedValues = new ArrayList<>();
        for (Double a : alphaValues) {
            normalizedValues.add(a / sum);
        }
        
        parameter.initByName("value", normalizedValues, "lower", "0.0", "upper", "1.0");
        
        // Create Dirichlet distribution prior
        RealParameter alphaParam = new RealParameter();
        alphaParam.initByName("value", alphaValues);
        
        Dirichlet dirichlet = new Dirichlet();
        dirichlet.initByName("alpha", alphaParam);
        
        Prior prior = new Prior();
        prior.setID(name + ".prior");
        prior.initByName("x", parameter, "distr", dirichlet);
        
        beastObjects.put(name, parameter);
        beastObjects.put(name + ".prior", prior);
    }
}