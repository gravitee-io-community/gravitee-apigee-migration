package com.gravitee.migration.service.migration;

public interface MigrationService {
    /**
     * Starts the migration process for the APIProxy and the shared flows.
     *
     * @param folderLocationString The folder location of the APIProxy that needs to be migrated.
     * @return A string indicating the result of the migration process.
     */
    String start(String folderLocationString);

}