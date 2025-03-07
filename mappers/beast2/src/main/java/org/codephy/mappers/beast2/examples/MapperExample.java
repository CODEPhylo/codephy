package org.codephy.mappers.beast2.examples;

import beast.base.core.BEASTInterface;
import beast.base.parser.XMLProducer;
import org.codephy.mappers.beast2.CodephyToBEAST2Mapper;

/**
 * Example of using the Codephy to BEAST2 mapper.
 */
public class MapperExample {

    /**
     * Convert a Codephy model to BEAST2 XML.
     *
     * @param args Command line arguments: [input JSON file] [output XML file]
     * @throws Exception if conversion fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: MapperExample [input JSON file] [output XML file]");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args[1];
        
        // Create mapper and convert the model
        CodephyToBEAST2Mapper mapper = new CodephyToBEAST2Mapper();
        mapper.convertToBEAST2Objects(inputFile);
        
        // Get the posterior distribution and export to XML
        BEASTInterface posterior = mapper.getPosterior();
        mapper.exportToXML(outputFile);
        
        System.out.println("Conversion complete. Output written to " + outputFile);
    }
}