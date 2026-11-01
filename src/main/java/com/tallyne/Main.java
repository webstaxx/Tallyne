package com.tallyne;

/**
 * Main entry point for the Tallyne V1 CLI application.
 * 
 * Delegates to TallyneInterpreter for all language execution semantics.
 * This separation allows the interpreter to be used independently of the CLI.
 */
public class Main {
    
    public static void main(String[] args) {
        com.tallyne.interpreter.TallyneInterpreter interpreter = 
            new com.tallyne.interpreter.TallyneInterpreter();
        
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "run":
                if (args.length < 2) {
                    System.err.println("Usage: tallyne run <file.tly>");
                    System.exit(2);
                }
                String filePath = args[1];
                boolean ran = interpreter.loadAndRunFile(filePath);
                if (!ran) {
                    System.exit(3);
                }
                break;
            case "list":
                listRoutines();
                break;
            case "unpack":
                if (args.length < 2) {
                    System.err.println("Usage: tallyne unpack <package.zip>");
                    System.exit(2);
                }
                String zipPath = args[1];
                // V1: unpack is a no-op/placeholder
                System.out.println("Unpack: V1 does not support package installation. " +
                    "This is a V2 feature. File: " + zipPath + " was acknowledged but not processed.");
                break;
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Tallyne V1 - Esoteric checklist programming language");
        System.out.println("Usage:");
        System.out.println("  tallyne run <file.tly>   Execute a Tallyne source file");
        System.out.println("  tallyne list           List registered routines");
        System.out.println("  tallyne unpack <zip>   Acknowledge a package (V1 placeholder)");
    }
    
    private static void listRoutines() {
        System.out.println("Tallyne Routines:");
        System.out.println("  (no routines registered in V1 placeholder)");
    }
}