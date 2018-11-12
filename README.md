[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity Build Status](https://teamcity.jetbrains.com/app/rest/builds/buildType(id:TeamCityPluginsByJetBrains_InvestigationsAutoAssigner_BuildAgainstTeamCity81x)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_InvestigationsAutoAssigner_BuildAgainstTeamCity81x&guest=1)

# TeamCity Investigations Auto-Assigner

## General Info
* Vendor: JetBrains
* License: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* Plugin type: free, open-source

## Plugin Description
The plugin adds a [build feature](https://confluence.jetbrains.com/display/TCDL/Adding+Build+Features) which enables automatic assignment of investigations for a build failure. Investigations are assigned on the basis of the following heuristics:

* If a user is the only committer to the build, the user is responsible.
* If a user is the only one who changed the files, whose names appear in the test or build problem error text, the user is responsible.
* If a user was responsible for this problem previous time, the user is responsible.
* If a user is set as the default responsible user, the user is responsible.

## Status
Working prototype is implemented.

## TeamCity Versions Compatibility
Compatible with TeamCity 2017.2 and later.

## Installation and Usage
1. Download the plugin from the [public TeamCity server](http://teamcity.jetbrains.com/viewLog.html?buildId=lastSuccessful&buildTypeId=TeamCityPluginsByJetBrains_InvestigationsAutoAssigner_BuildAgainstTeamCity81x&tab=artifacts)

2. Install the plugin as described in the [TeamCity documentation](http://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins#InstallingAdditionalPlugins-InstallingTeamCityplugins).

When the plugin is installed, the Investigations Auto Assigner [build feature](https://confluence.jetbrains.com/display/TCDL/Adding+Build+Features) appears in the build configuration settings. Add the build feature to your configuration to enable investigations auto-assignment.

See [Wiki](https://github.com/JetBrains/teamcity-investigations-auto-assigner/wiki) for more information.

## Additional configuration
1. **Frequency of processing problems:** 
To avoid TeamCity server performance degradation, 
the extension checks builds for new build problems and failed tests and processes them regularly, 
with a configurable interval. The default is every 120 seconds, which can be changed
via the `teamcity.investigationAutoAssigner.processingDelayInSeconds` 
[TeamCity property](https://confluence.jetbrains.com/display/TCDL/Configuring+TeamCity+Server+Startup+Properties).
*Restart the server for the changes to take effect.*
 
2. **Limitation of processed failed tests and build problems:** 
Currently there is a configurable limit of the number of the processed build problems 
and failed tests. The default is 100, which can be changed via 
the `teamcity.investigationAutoAssigner.maxTestsPerBuildNumber` [build parameter](https://confluence.jetbrains.com/display/TCDL/Configuring+Build+Parameters).
*The changes will be applied to the build run with the parameter.*

## Feedback
Everybody is encouraged to try the plugin and provide feedback in the [forum](http://devnet.jetbrains.net/community/teamcity/teamcity) or post bugs into the [issue tracker](http://youtrack.jetbrains.net/issues/TW).
Please make sure to note the plugin version that you use.
