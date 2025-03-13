package org.codephy.mappers.beast2;

/**
 * Constants used for BEAST2 API interactions.
 * These constants represent the string literals used when interacting with
 * the BEAST2 API, such as parameter names and model types.
 */
public class Beast2Constants {
    
    // BEAST2 parameter names
    public static final String PARAM_KAPPA = "kappa";
    public static final String PARAM_FREQUENCIES = "frequencies";
    public static final String PARAM_RATE_AC = "rateAC";
    public static final String PARAM_RATE_AG = "rateAG";
    public static final String PARAM_RATE_AT = "rateAT";
    public static final String PARAM_RATE_CG = "rateCG";
    public static final String PARAM_RATE_CT = "rateCT";
    public static final String PARAM_RATE_GT = "rateGT";
    public static final String PARAM_ALPHA = "alpha";  // for Gamma in BEAST2
    public static final String PARAM_BETA = "beta";    // for Gamma in BEAST2
    public static final String PARAM_BIRTH_DIFF_RATE = "birthDiffRate";
    public static final String PARAM_RELATIVE_DEATH_RATE = "relativeDeathRate";
    public static final String PARAM_MEAN_IN_REAL_SPACE = "meanInRealSpace";
    public static final String PARAM_M = "M";  // for LogNormal in BEAST2
    public static final String PARAM_S = "S";  // for LogNormal in BEAST2
    public static final String PARAM_MEAN = "mean";
    public static final String PARAM_SIGMA = "sigma";
    public static final String PARAM_SHAPE = "shape";
    public static final String PARAM_POP_SIZE = "popSize";
    public static final String PARAM_CLOCK_RATE = "clock.rate";
    
    // BEAST2 object IDs
    public static final String ID_TREE_LIKELIHOOD = "treeLikelihood";
    public static final String ID_SITE_MODEL = "siteModel";
    public static final String ID_CLOCK_MODEL = "clockModel";
    public static final String ID_GAMMA_SHAPE = "gammaShape";
    public static final String ID_PRIOR = "prior";
    public static final String ID_LIKELIHOOD = "likelihood";
    public static final String ID_POSTERIOR = "posterior";
    public static final String ID_STATE = "state";
    public static final String ID_MCMC = "mcmc";
    public static final String ID_CONSOLE_LOGGER = "consoleLogger";
    public static final String ID_FILE_LOGGER = "fileLogger";
    public static final String ID_TREE_LOGGER = "treeLogger";
    
    // BEAST2 parameter input names
    public static final String INPUT_ALIGNMENT = "alignment";
    public static final String INPUT_DISTRIBUTION = "distribution";
    public static final String INPUT_DISTR = "distr";
    public static final String INPUT_X = "x";
    public static final String INPUT_DATA = "data";
    public static final String INPUT_TREE = "tree";
    public static final String INPUT_SITE_MODEL = "siteModel";
    public static final String INPUT_SUBST_MODEL = "substModel";
    public static final String INPUT_GAMMA_CATEGORY_COUNT = "gammaCategoryCount";
    public static final String INPUT_PROPORTION_INVARIANT = "proportionInvariant";
    public static final String INPUT_POPULATION_MODEL = "populationModel";
    public static final String INPUT_STATE_NODE = "stateNode";
    public static final String INPUT_PARAMETER = "parameter";
    public static final String INPUT_WEIGHT = "weight";
    public static final String INPUT_CHAIN_LENGTH = "chainLength";
    public static final String INPUT_STATE = "state";
    public static final String INPUT_OPERATOR = "operator";
    public static final String INPUT_LOGGER = "logger";
    public static final String INPUT_LOG_EVERY = "logEvery";
    public static final String INPUT_LOG = "log";
    public static final String INPUT_FILE_NAME = "fileName";
    public static final String INPUT_MODE = "mode";
    public static final String INPUT_VALUE = "value";
    public static final String INPUT_LOWER = "lower";
    public static final String INPUT_UPPER = "upper";
}