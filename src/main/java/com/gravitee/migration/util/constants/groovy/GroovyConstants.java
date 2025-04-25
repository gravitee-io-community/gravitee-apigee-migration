package com.gravitee.migration.util.constants.groovy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroovyConstants {

    // Script to upload a single shared policy groups to the management API
    // (replace URL and Authorization manually in the generated bat file after migration)
    public static final String CURL_COMMAND_TEMPLATE = """
            @echo off
            set "URL=%%~1"
            set "Authorization=%%~2"
            
            curl -k -X POST %%URL%% ^
            -H "Content-Type: application/json" ^
            -H "Authorization: %%Authorization%%" ^
            --data "@%s" ^
            """;

    // Script to execute all batch scripts in the current directory (used for uploading shared policy groups to the management API)
    // (replace URL and Authorization manually in the generated bat file after migration)
    public static final String MASTER_SCRIPT = """
            @echo off
            set "URL={}"
            set "Authorization={}"
            
            echo Executing all batch scripts...
            
            for /r "%~dp0" %%f in (*.bat) do (
                if /i not "%%~nxf"=="execute_all_scripts.bat" (
                    echo Running "%%f"
                    pushd "%%~dpf"
                    call "%%f" "%URL%" "%Authorization%"
                    popd
                )
            )
            
            echo All scripts executed.
            """;
}
