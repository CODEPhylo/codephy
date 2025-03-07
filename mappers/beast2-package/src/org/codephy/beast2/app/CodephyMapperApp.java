package org.codephy.beast2.app;

import org.codephy.mappers.beast2.CodephyToBEAST2Mapper;
import beast.base.core.Description;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.pkgmgmt.PackageManager;

@Description("Application for converting Codephy JSON to BEAST2 XML")
public class CodephyMapperApp {
    
    public CodephyMapperApp() {
    }
    
    public static void main(String[] args) {
        try {
            // Initialize BEAST2
            ProgramStatus.name = "Codephy BEAST2 Mapper";
            
            // Load BEAST packages
            try {
                PackageManager.loadExternalJars();
            } catch (Exception e) {
                // Log but continue - core functionality should still work
                System.err.println("Warning: Could not load some external packages: " + e.getMessage());
            }
            
            if (args.length != 2) {
                System.err.println("Usage: codephyMapper <input-json-file> <output-xml-file>");
                System.exit(1);
            }
            
            String inputFile = args[0];
            String outputFile = args[1];
            
            Log.info.println("Converting " + inputFile + " to BEAST2 XML...");
            
            // Use your existing mapper code
            CodephyToBEAST2Mapper mapper = new CodephyToBEAST2Mapper();
            mapper.convertToBEAST2Objects(inputFile);
            mapper.exportToXML(outputFile);
            
            Log.info.println("Conversion complete. Output written to " + outputFile);
            
        } catch (Exception e) {
            Log.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}