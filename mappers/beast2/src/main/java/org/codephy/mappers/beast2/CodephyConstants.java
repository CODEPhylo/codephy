package org.codephy.mappers.beast2;

/**
 * Constants used for the Codephy JSON schema.
 * These constants represent the string literals in the Codephy JSON format
 * that should be used when parsing the schema.
 */
public class CodephyConstants {
    
    // Substitution model function types in Codephy schema
    public static final String FUNCTION_HKY = "HKY";
    public static final String FUNCTION_JC69 = "JC69";
    public static final String FUNCTION_GTR = "GTR";
    
    // Clock model function types in Codephy schema
    public static final String FUNCTION_STRICT_CLOCK = "strictClock";
    public static final String FUNCTION_RELAXED_CLOCK = "relaxedClock";
    public static final String FUNCTION_UNCORRELATED_CLOCK = "uncorrelatedClock";
    
    // Other function types in Codephy schema
    public static final String FUNCTION_NORMALIZE = "normalize";
    public static final String FUNCTION_VECTOR_ELEMENT = "vectorElement";
    
    // Distribution types in Codephy schema
    public static final String DIST_PHYLOCTMC = "PhyloCTMC";
    public static final String DIST_YULE = "Yule";
    public static final String DIST_BIRTH_DEATH = "BirthDeath";
    public static final String DIST_COALESCENT = "Coalescent";
    public static final String DIST_CONSTRAINED_YULE = "ConstrainedYule";
    
    // Parameter names in PhyloCTMC (Codephy schema)
    public static final String PARAM_TREE = "tree";
    public static final String PARAM_SITE_RATES = "siteRates";
    public static final String PARAM_BRANCH_RATES = "branchRates";
    public static final String PARAM_RATE = "rate";
    
    // Parameter names in substitution models (Codephy schema)
    public static final String PARAM_KAPPA = "kappa";
    public static final String PARAM_BASE_FREQUENCIES = "baseFrequencies";
    public static final String PARAM_RATE_AC = "rateAC";
    public static final String PARAM_RATE_AG = "rateAG";
    public static final String PARAM_RATE_AT = "rateAT";
    public static final String PARAM_RATE_CG = "rateCG";
    public static final String PARAM_RATE_CT = "rateCT";
    public static final String PARAM_RATE_GT = "rateGT";
    
    // Distribution parameter names (Codephy schema)
    public static final String PARAM_SHAPE = "shape";
    public static final String PARAM_RATE_GAMMA = "rate";
    public static final String PARAM_MEAN = "mean";
    public static final String PARAM_SD = "sd";
    public static final String PARAM_MEANLOG = "meanlog";
    public static final String PARAM_SDLOG = "sdlog";
    public static final String PARAM_LOWER = "lower";
    public static final String PARAM_UPPER = "upper";
    public static final String PARAM_BIRTH_RATE = "birthRate";
    public static final String PARAM_DEATH_RATE = "deathRate";
    public static final String PARAM_POPULATION_SIZE = "populationSize";
    public static final String PARAM_ROOT_HEIGHT = "rootHeight";
    
    // JSON structure fields in Codephy schema
    public static final String FIELD_RANDOM_VARS = "randomVariables";
    public static final String FIELD_DET_FUNCTIONS = "deterministicFunctions";
    public static final String FIELD_DISTRIBUTION = "distribution";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_GENERATES = "generates";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_ARGUMENTS = "arguments";
    public static final String FIELD_FUNCTION = "function";
    public static final String FIELD_OBSERVED = "observedValue";
}