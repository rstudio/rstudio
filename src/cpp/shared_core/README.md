## Purpose

This library exists to be shared with external projects from the IDE. Files should be added to this library only when they are directly needed by the IDE and an external project which uses this library. If the file/class will only be used by the IDE, it should be added to the `core` library, or another applicable library. If the file/class will be used only by an external project, it should be placed in the relevant location within that project. 

This library should be kept as small as possible to avoid adding unnecessary dependencies to external projects. Be careful when moving files or functionality from `core` to `shared_core` to avoid moving more than is absolutely necessary. If possible, try refactoring to reduce dependencies on the other classes in `core`. At the same time, the goal is to eliminate or greatly reduce duplicate code (and therefore duplicate maintenance), so if you feel like you're writing the same thing over again, it may be a good idea to put it here.

## Coding Standards

Currently this library is shared with the [Launcher Plugin SDK](https://github.com/rstudio/rstudio-launcher-plugin-sdk). Because the Launcher Plugin SDK has a stricter coding standard (because it is an SDK), files in this library must follow the coding standards that can be found on the [Launcher Plugin SDK Wiki](https://github.com/rstudio/rstudio-launcher-plugin-sdk/wiki/Coding-Standards)

## Licensing

Because the Launcher Plugin SDK uses an MIT license and the source will be distributed, files added to this library must also use an MIT license.

As an exception to this, Windows only files will not be shared with the Launcher Plugin SDK and may be licensed as usual (e.g. Win32StringUtils.cpp/hpp).
