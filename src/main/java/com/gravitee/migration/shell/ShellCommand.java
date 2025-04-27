package com.gravitee.migration.shell;

import com.gravitee.migration.service.migration.MigrationService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ShellCommand {

    private final MigrationService migrationService;

    public ShellCommand(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Starts the migration process for the APIProxy and the shared flows.
     * ex. input "start path/to/apiproxy"
     *
     * @param apiProxyFolderLocation The folder location of the APIProxy that needs to be migrated.
     */
    @ShellMethod(key = "start", value = "Start the migration")
    public String start(@ShellOption(help = "The APIProxy folder location.") String apiProxyFolderLocation) {
        return migrationService.start(apiProxyFolderLocation);
    }
}
