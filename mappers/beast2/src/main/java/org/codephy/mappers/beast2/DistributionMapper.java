package org.codephy.mappers.beast2;

import beast.base.core.BEASTInterface;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Coordinates the mapping of Codephy distributions to BEAST2 distribution objects.
 * Updated to use separate Codephy and BEAST2 constants.
 */
public class DistributionMapper {

    private final Map<String, BEASTInterface> beastObjects;
    private final StandardDistributionsMapper standardMapper;
    private final TreeDistributionsMapper treeMapper;
    
    /**
     * Constructor.
     *
     * @param beastObjects Shared map of created BEAST2 objects
     */
    public DistributionMapper(Map<String, BEASTInterface> beastObjects) {
        this.beastObjects = beastObjects;
        this.standardMapper = new StandardDistributionsMapper(beastObjects);
        this.treeMapper = new TreeDistributionsMapper(beastObjects);
    }
    
    /**
     * Create a BEAST2 object for a distribution based on its type.
     */
    public void createDistribution(String name, String distType, String generates, JsonNode distNode, JsonNode varNode) throws Exception {
        // Determine if this is a tree distribution or a standard parameter distribution
        if (isTreeDistribution(distType)) {
            treeMapper.createTreeDistribution(name, distType, distNode);
        } else if (distType.equals(CodephyConstants.DIST_PHYLOCTMC)) {
            treeMapper.createPhyloCTMC(name, distNode, varNode.path(CodephyConstants.FIELD_OBSERVED));
        } else {
            standardMapper.createStandardDistribution(name, distType, generates, distNode);
        }
    }
    
    /**
     * Connect the parameters of a distribution based on its type.
     */
    public void connectDistribution(String name, String distType, JsonNode distNode) throws Exception {
        if (isTreeDistribution(distType)) {
            treeMapper.connectTreeDistribution(name, distType, distNode);
        } else if (distType.equals(CodephyConstants.DIST_PHYLOCTMC)) {
            treeMapper.connectPhyloCTMC(name, distNode);
        }
        // Standard distributions don't need additional connection steps
    }
    
    /**
     * Determine if a distribution type is a tree distribution.
     */
    private boolean isTreeDistribution(String distType) {
        return distType.equals(CodephyConstants.DIST_YULE) || 
               distType.equals(CodephyConstants.DIST_BIRTH_DEATH) || 
               distType.equals(CodephyConstants.DIST_COALESCENT) ||
               distType.equals(CodephyConstants.DIST_CONSTRAINED_YULE);
    }
}