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

    @ShellMethod(key = "start", value = "Start the migration")
    public String startMigration(@ShellOption(help = "The APIGee XML file location.") String xmlFileLocation) {
        return migrationService.start(xmlFileLocation);
    }

}
