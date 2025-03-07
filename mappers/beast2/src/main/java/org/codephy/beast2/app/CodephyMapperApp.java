package org.codephy.beast2.app;

import org.codephy.mappers.beast2.CodephyToBEAST2Mapper;
import org.codephy.mappers.beast2.ModelBuilder;
import beast.base.core.Description;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.base.inference.MCMC;
import beast.base.core.BEASTInterface;
import beast.pkgmgmt.PackageManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Description("Application for converting Codephy JSON to BEAST2 XML and optionally running MCMC")
public class CodephyMapperApp {
    
    public CodephyMapperApp() {
    }
    
    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  codephyMapper convert <input-json-file> <output-xml-file>");
        System.err.println("  codephyMapper run <input-json-file> [<output-xml-file>] [<output-dir>]");
        System.err.println();
        System.err.println("Commands:");
        System.err.println("  convert    Convert Codephy JSON to BEAST2 XML without running");
        System.err.println("  run        Convert and run the MCMC analysis directly");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  <input-json-file>    Path to the Codephy JSON model file");
        System.err.println("  <output-xml-file>    Path for output XML (required for 'convert')");
        System.err.println("  <output-dir>         Optional directory for MCMC output files (default: same as input)");
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
            
            // Check command line arguments
            if (args.length < 2) {
                printUsage();
                System.exit(1);
            }
            
            String command = args[0];
            String inputFile = args[1];
            String outputFile = null;
            String outputDir = null;
            
            // Validate input file existence
            File inputFileObj = new File(inputFile);
            if (!inputFileObj.exists() || !inputFileObj.isFile()) {
                System.err.println("Error: Input file not found: " + inputFile);
                System.exit(1);
            }
            
            if ("convert".equals(command)) {
                // Convert mode requires output file
                if (args.length != 3) {
                    System.err.println("Error: 'convert' command requires an output XML file");
                    printUsage();
                    System.exit(1);
                }
                outputFile = args[2];
                convertOnly(inputFile, outputFile);
            } 
            else if ("run".equals(command)) {
                // Run mode has optional output file and directory
                if (args.length >= 3) {
                    outputFile = args[2];
                }
                if (args.length >= 4) {
                    outputDir = args[3];
                } else {
                    // Default output directory is in the same location as the input file
                    outputDir = new File(inputFile).getParent();
                    if (outputDir == null) {
                        outputDir = "."; // Current directory if no parent path
                    }
                }
                convertAndRun(inputFile, outputFile, outputDir);
            }
            else {
                System.err.println("Error: Unknown command '" + command + "'");
                printUsage();
                System.exit(1);
            }
            
        } catch (Exception e) {
            Log.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Convert Codephy JSON to BEAST2 XML without running MCMC.
     */
    private static void convertOnly(String inputFile, String outputFile) throws Exception {
        Log.info.println("Converting " + inputFile + " to BEAST2 XML...");
        
        CodephyToBEAST2Mapper mapper = new CodephyToBEAST2Mapper();
        mapper.convertToBEAST2Objects(inputFile);
        mapper.exportToXML(outputFile);
        
        Log.info.println("Conversion complete. Output written to " + outputFile);
    }
    
    /**
     * Convert Codephy JSON to BEAST2 objects and run the MCMC.
     * Optionally exports the XML file if outputFile is provided.
     */
    private static void convertAndRun(String inputFile, String outputFile, String outputDir) throws Exception {
        Log.info.println("Converting " + inputFile + " to BEAST2 objects...");
        
        // Convert the JSON to BEAST2 objects
        CodephyToBEAST2Mapper mapper = new CodephyToBEAST2Mapper();
        mapper.convertToBEAST2Objects(inputFile);
        
        // Optionally export to XML if requested
        if (outputFile != null) {
            mapper.exportToXML(outputFile);
            Log.info.println("Model XML written to " + outputFile);
        }
        
        // Get the MCMC object
        BEASTInterface posterior = mapper.getPosterior();
        if (!(posterior instanceof MCMC)) {
            throw new RuntimeException("Expected MCMC object, but got " + 
                                     (posterior == null ? "null" : posterior.getClass().getSimpleName()));
        }
        
        MCMC mcmc = (MCMC) posterior;
        
        // Create output directory if it doesn't exist
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            boolean success = outputDirFile.mkdirs();
            if (!success) {
                Log.err.println("Warning: Could not create output directory: " + outputDir);
                Log.err.println("Using current directory instead.");
                outputDir = ".";
            }
        }
        
        // Get the base filename for logs
        String baseName = new File(inputFile).getName();
        if (baseName.toLowerCase().endsWith(".json")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        
        // Update log file paths using ModelBuilder's method (if available)
        try {
            // Try to access the ModelBuilder through reflection - this assumes 
            // the ModelBuilder instance is stored in CodephyToBEAST2Mapper
            java.lang.reflect.Field field = mapper.getClass().getDeclaredField("modelBuilder");
            field.setAccessible(true);
            ModelBuilder modelBuilder = (ModelBuilder) field.get(mapper);
            
            if (modelBuilder != null) {
                modelBuilder.updateLogFilePaths(outputDir, baseName);
                Log.info.println("Set log file paths to use directory: " + outputDir);
            }
        } catch (Exception e) {
            // If ModelBuilder isn't directly accessible, update log files manually
            updateLogFiles(mcmc, outputDir, baseName);
        }
        
        // Run the MCMC analysis
        Log.info.println("Running MCMC analysis...");
        Log.info.println("Output logs will be written to directory: " + outputDir);
        
        long startTime = System.currentTimeMillis();
        mcmc.run();
        long endTime = System.currentTimeMillis();
        
        Log.info.println("MCMC analysis complete!");
        Log.info.println("Analysis took " + ((endTime - startTime) / 1000.0) + " seconds");
    }
    
    /**
     * Updates log file paths manually if the ModelBuilder method isn't accessible.
     * This is a fallback mechanism.
     */
    private static void updateLogFiles(MCMC mcmc, String outputDir, String baseName) {
        try {
            // Look for all loggers in the MCMC object
            for (Object logger : mcmc.loggersInput.get()) {
                if (logger instanceof beast.base.inference.Logger) {
                    beast.base.inference.Logger log = (beast.base.inference.Logger) logger;
                    
                    // Check logger type by ID or content
                    String loggerId = log.getID();
                    if (loggerId != null) {
                        if (loggerId.equals("fileLogger")) {
                            String newPath = new File(outputDir, baseName + ".log").getPath();
                            log.fileNameInput.setValue(newPath, log);
                        } else if (loggerId.equals("treeLogger")) {
                            String newPath = new File(outputDir, baseName + ".trees").getPath();
                            log.fileNameInput.setValue(newPath, log);
                        }
                    }
                }
            }
            Log.info.println("Set log file paths to use directory: " + outputDir);
        } catch (Exception e) {
            Log.err.println("Warning: Could not update log file paths. Using default paths.");
        }
    }
}