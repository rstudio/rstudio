# Visual C++ Runtime Files

The DLLs in this folder will be installed as part of RStudio, and should be updated to match the
ones included with most recent major version of the Microsoft Visual C++ toolset (currently 2022).

## How To Update These Files

Microsoft doesn't make it easy to get the files from vc_redist.exe. There is an /extract argument
that is documented as doing so, but this hasn't worked in many, many years, and never will.

The best way is to install the latest version of Microsoft Visual C++ build tools, and copy the
files from its redist folders.

Download `vs_buildtools.exe` from: https://visualstudio.microsoft.com/visual-cpp-build-tools/.

```powershell
vs_buildtools.exe --passive --add Microsoft.VisualStudio.Workload.VCTools --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64
```

Copy all the files from the following to the x64 and x86 folders.

- x64: `C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Redist\MSVC\14.42.34433\x64\Microsoft.VC143.CRT\*.*`
- x86: `C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Redist\MSVC\14.42.34433\x64\Microsoft.VC143.CRT\*.*`
- ARM: future

Note that "2022", "14.29.30133", and "VC143" will need to be updated to match what has actually
been installed.

## TLDR; More Background

Up to and including version 2024.12.1, RStudio desktop on Windows installed the vc_redist and
ucrt files via CMake's `CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS_SKIP` and `CMAKE_INSTALL_UCRT_LIBRARIES`.

CMake uses the versions of the redist files that came with the Visual C++ build tools installed
on the build machine, installing into RStudio folder, not globally.

Unless we keep the compiler up-to-date, we may end up installing runtimes that
are not compatible with those installed system-wide (C:\Windows\system32, etc.), which can
lead to crashes such as https://github.com/rstudio/rstudio/issues/15674.

This is because "...the version of the Microsoft Visual C++ Redistributable installed on the machine
must be the same or higher than the version of the Visual C++ toolset used to create your application."
