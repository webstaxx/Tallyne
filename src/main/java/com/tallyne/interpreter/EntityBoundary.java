package com.tallyne.interpreter;

/**
 * Minimal V1 Entity integration boundary.
 * 
 * The interpreter implements this interface to communicate with Entities.
 * V1 only supports the pre-registered `IO` Entity.
 * 
 * Future V2 will support third-party Java Entities distributed as ZIP packages,
 * loaded via CLI and referenced in Tallyne programs.
 * 
 * This interface is deliberately minimal - only radio and query operations.
 * No plugin SDK, permission system, or package manager is defined in V1.
 */
public interface EntityBoundary {
    
    /**
     * Send information to an Entity or Routine.
     * 
     * @param target The name of the Entity or Routine (e.g., "IO", "WebBrowser")
     * @param message The message/content to send
     * @return true if the radio operation was successful
     */
    boolean radio(String target, String message);
    
    /**
     * Request information from an Entity and store the response in `it`.
     * 
     * @param entityName The name of the Entity to query
     * @param question The question/query to send to the Entity
     * @return The Entity's response string, or null if Entity not found
     */
    String query(String entityName, String question);
}