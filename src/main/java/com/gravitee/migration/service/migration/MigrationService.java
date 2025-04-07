package com.gravitee.migration.service.migration;

public interface MigrationService {

    /**
     * Starts the migration process for the specified XML file location.
     *
     * @param apiGeeFolderLocation The location of the apiGeeFolder.
     * @return A message indicating the result of the migration process.
     */
    String start(String apiGeeFolderLocation);
}
