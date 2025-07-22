# AlonsoLevels JAR Dependency

## Required File: `AlonsoLevels.jar`

To compile and run this project, you need to place the **AlonsoLevels.jar** file in this `libs/` folder.

### How to obtain AlonsoLevels.jar:

1. **Purchase/Download AlonsoLevels plugin:**
   - Visit: https://github.com/AlonsoAliaga/AlonsoLevels
   - Or search for it on SpigotMC/BuiltByBit
   - This is likely a premium/paid plugin

2. **Place the JAR file here:**
   ```
   libs/
   └── AlonsoLevels.jar  ← Place the plugin JAR file here
   ```

3. **Verify the file:**
   - File size should be ~1-2MB
   - File name should be exactly: `AlonsoLevels.jar`

### Alternative API-only JAR:

If you have access to an API-only JAR file, you can use that instead:
```
libs/
└── AlonsoLevels-API.jar
```

And update `build.gradle` to reference the API JAR instead.

### Build Status:

✅ **Current Status:** Ready to compile and run!  
✅ **JAR File:** AlonsoLevelsPro-v2.4-BETA-PRO.jar (544KB) - Successfully integrated!

### Version Compatibility:

This project expects AlonsoLevels API version **2.4-BETA-PRO** or compatible.
The following API methods are required:
- `AlonsoLevelsAPI.getLevel(UUID)`  
- `LevelChangeEvent` 