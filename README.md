# Pdx-Unlimiter launcher

## Building and running

You can build the project with `gradle build`.
To create jlink images and installers, use `gradle createDist`.
For running, you can use `gradle run -Dpdxu.installDir=<installation directory>`.
The `-Dpdxu.installDir=<installation directory>` property is needed for a dev build to simulate the program operating
in a real installation directory and not the build directory.
