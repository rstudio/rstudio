# Building RStudio Installers

The scripts in this directory build RStudio and wrap the result in a
redistributable installer: a `.deb` or `.rpm` on Linux, a `.dmg` on macOS, and
an NSIS setup `.exe` on Windows.

This is a different goal from the one [`INSTALL`](../INSTALL) describes.
`INSTALL` covers configuring CMake by hand and running `make install`, which
puts RStudio directly onto the machine that built it. Use the scripts here when
you want an installer file you can copy to another machine, or when you want to
test the install experience end to end.

Both paths need the same build dependencies, so install those first by
following the README for your platform:

- [`dependencies/linux/README`](../dependencies/linux/README)
- [`dependencies/osx/README.md`](../dependencies/osx/README.md)
- [`dependencies/windows/README.md`](../dependencies/windows/README.md)

A package build compiles the C++ backend, the GWT front end, and the Electron
desktop application, and then runs CPack over the result. Expect it to take
substantially longer than an ordinary incremental build.

## What you can build

| Platform | Desktop (Electron)         | Server           |
| -------- | -------------------------- | ---------------- |
| Linux    | `.deb`, `.rpm`, `.tar.gz`  | `.deb`, `.rpm`   |
| macOS    | `.dmg`                     | not supported    |
| Windows  | NSIS `.exe`, `.zip`        | not supported    |

RStudio Server runs only on Linux, so there is no server package to build on
macOS or Windows. On those platforms the packaging scripts always build the
Electron desktop application.

## Set the version before you build

Every packaging script takes the build version from environment variables:

| Variable                | Example | Default if unset |
| ----------------------- | ------- | ---------------- |
| `RSTUDIO_VERSION_MAJOR` | `2026`  | current year     |
| `RSTUDIO_VERSION_MINOR` | `8`     | current month    |
| `RSTUDIO_VERSION_PATCH` | `0`     | `999`            |
| `RSTUDIO_VERSION_SUFFIX`| `+1`    | `-dev+999`       |

Those defaults come from [`cmake/globals.cmake`](../cmake/globals.cmake), and
they are what determine the installer's file name and the version your package
manager records. The scripts' own `--help` output mentions a `99.9.9` default,
which is a separate fallback that only reaches the Electron application
metadata. Setting all four variables keeps the two in sync and is the
recommended practice:

```bash
export RSTUDIO_VERSION_MAJOR=2026
export RSTUDIO_VERSION_MINOR=8
export RSTUDIO_VERSION_PATCH=0
export RSTUDIO_VERSION_SUFFIX=+1
```

Two details affect the resulting file names. A `+` in the suffix becomes a `-`,
because not every package format accepts `+`. And unless you set
`CMAKE_BUILD_TYPE=Release`, the build type is appended to the file name; the
default build type for a package build is `RelWithDebInfo`.

## Linux

Run the scripts from `package/linux`. The first argument is the build target
and the second is the package format:

```bash
cd package/linux

./make-package Server DEB        # RStudio Server .deb
./make-package Server RPM        # RStudio Server .rpm
./make-package Electron DEB      # RStudio Desktop .deb
./make-package Electron RPM      # RStudio Desktop .rpm
```

Pass `clean` as a third argument to force a full rebuild
(`./make-package Server DEB clean`). Without it the build is incremental and
reuses the existing build directory.

Two wrappers are available for convenience:

- `./make-server-package DEB` is equivalent to `./make-package Server DEB`.
- `./make-electron-package DEB` runs `./make-package Electron DEB` and then
  additionally produces a `.tar.gz` of the installed tree, for installing
  without a package manager.

### Finding the output

Each build gets its own directory named `build-<target>-<format>`, or
`build-<target>-<format>-<build type>` when `CMAKE_BUILD_TYPE` is set
explicitly. The package is written there:

```bash
$ ls build-Server-DEB/*.deb
build-Server-DEB/rstudio-server-2026.8.0-1-amd64-relwithdebinfo.deb
```

The `make-electron-package` tarball is written deeper in the build tree, under
`build-Electron-<format>/_CPack_Packages/Linux/<format>/`.

### Installing the result

Install through your package manager rather than `dpkg`/`rpm` directly, so that
RStudio's dependencies get resolved:

```bash
sudo apt install ./build-Server-DEB/rstudio-server-*.deb        # Debian, Ubuntu
sudo dnf install ./build-Server-RPM/rstudio-server-*.rpm        # Fedora, RHEL
sudo zypper install ./build-Server-RPM/rstudio-server-*.rpm     # openSUSE
```

The server package's post-install script performs the configuration that
section 4 of `INSTALL` describes doing by hand: it creates the `rstudio-server`
user, links the admin script into `/usr/sbin`, creates `/etc/rstudio` and the
required `/var` directories, installs the systemd unit, and starts the service.
Confirm it is running with:

```bash
sudo rstudio-server status
```

The desktop package installs to `/usr/lib/rstudio`, symlinks `rstudio` into
`/usr/bin`, and installs a `.desktop` file so RStudio appears in the
Applications menu.

## Linux packages without a local toolchain

`docker/docker-compile.sh` runs the build inside one of the container images
used by CI. This is the least invasive way to produce a package, and the only
practical way to build for a distribution you are not running.

```bash
# from the repository root
./docker/docker-compile.sh jammy server 2026.8.0+1
./docker/docker-compile.sh rhel9 electron 2026.8.0+1
```

The arguments are the image name, the flavor (`server` or `electron`), and the
version. Run the script with no arguments to print the valid image names, which
are derived from the `Dockerfile.*` files in `docker/jenkins`. The images that
build packages are `bionic`, `focal`, `jammy`, `opensuse15`, `rhel8`, and
`rhel9`; `snyk`, `versioning`, and `windows` appear in that list but serve other
purposes.

The script builds the container image if it is not already present, runs a
clean package build inside it, and copies the finished package into `package/`
in your current working directory.

Two environment variables are worth knowing:

- `CONTAINER_ARCH=amd64|arm64` builds for an architecture other than the host's.
- `CMAKE_BUILD_TYPE=Debug` produces a debug build.

To build Windows packages in a container, use `docker\win-docker-compile.cmd`
from a Windows Command Prompt instead.

## macOS

Run the script from `package/osx`:

```bash
cd package/osx
./make-package clean
```

On an Apple Silicon Mac the default is to build both `x86_64` and `arm64` and
produce a universal application; on an Intel Mac only `x86_64` is built.
Restrict this with `--arch`:

```bash
./make-package --arch=arm64
```

Other options, from `./make-package --help`:

- `clean` performs a full rebuild instead of an incremental one.
- `--install` copies the built application to `/Applications/RStudio-Devel.app`,
  where it can coexist with a released RStudio.
- `--build-dmg=0` produces the application bundle but skips the disk image.
- `--build-gwt=0` reuses the previous GWT build, which saves several minutes
  when you are only iterating on C++ or Electron code.

The build produces two artifacts:

- the application bundle at `package/osx/install/RStudio.app`
- the disk image at `package/osx/build/RStudio-<version>.dmg`, or
  `package/osx/build-arm64/` when `arm64` is the only architecture built

Outside of Posit's CI the build is ad-hoc signed. The result runs on the machine
that built it, but distributing it to other Macs requires signing and
notarizing it with your own Apple Developer credentials.

## Windows

Install the build dependencies first by following
[`dependencies/windows/README.md`](../dependencies/windows/README.md), which
covers the PowerShell bootstrap script and `install-dependencies.cmd`.
`make-package.bat` invokes `vcvarsall.bat` itself, but `ant`, `cmake`, and
`vcvarsall.bat` all need to be resolvable on `PATH`.

From a non-administrator Command Prompt:

```
cd package\win32
make-package.bat
```

The recognized options, from `make-package.bat --help`:

- `clean` performs a full rebuild.
- `debug` produces a debug build.
- `multiarch` builds both 32-bit and 64-bit `rsession` executables.
- `nogwt` reuses the previous GWT build.
- `nozip` skips the ZIP file.
- `quick` skips the NSIS setup package.

Output lands in `package\win32\build`, or `package\win32\build-debug` for a
debug build:

- `RStudio-<version>-RelWithDebInfo.exe`, the NSIS installer
- `RStudio-<version>-RelWithDebInfo.zip`, the same payload without an installer

CPack stages its work in `C:\rsbuild` to stay under the Windows 260-character
path limit, then the finished files are moved into the build directory.

If you already have a completed build and only want to regenerate the
installers, run `make-dist-packages.bat` from `package\win32`.

## Gotchas

**Set the version variables.** Leaving them unset does not fail the build, but
it produces packages named after the current date, and
`make-electron-package`'s tarball ends up with empty version fields in its
name. See [Set the version before you build](#set-the-version-before-you-build).

**`LIBR_HOME` is hardcoded on Windows.** `make-package.bat` passes
`-DLIBR_HOME=C:\R\R-3.6.3`. `Install-RStudio-Prereqs.ps1` installs R to exactly
that path, but it skips the installation entirely if `C:\R` already exists, so
a machine with a different R there will fail to configure. Either place R at
`C:\R\R-3.6.3` or edit the path in the script.

**The `Desktop` target is gone.** `package/linux/make-package Desktop DEB` still
works, but it prints a notice and builds `Electron` instead. The Qt-based
desktop it referred to no longer exists.

**Incremental builds are the default.** Every script rebuilds in place unless
you pass `clean`. That is usually what you want, but after switching branches
or changing CMake options, a clean build avoids stale-artifact problems.

**Build type leaks into file names.** A default package build is
`RelWithDebInfo`, so the installers are named accordingly. Set
`CMAKE_BUILD_TYPE=Release` if you want names without the suffix.

## Script reference

| Script                              | Platform | Produces                                     |
| ----------------------------------- | -------- | -------------------------------------------- |
| `linux/make-package`                | Linux    | `.deb` or `.rpm`, desktop or server           |
| `linux/make-server-package`         | Linux    | Server `.deb` or `.rpm`                       |
| `linux/make-electron-package`       | Linux    | Desktop `.deb` or `.rpm`, plus a `.tar.gz`    |
| `osx/make-package`                  | macOS    | `RStudio.app` and a `.dmg`                    |
| `win32/make-package.bat`            | Windows  | NSIS `.exe` and `.zip`                        |
| `win32/make-dist-packages.bat`      | Windows  | NSIS `.exe` and `.zip` from an existing build |
| `../docker/docker-compile.sh`       | Linux    | `.deb` or `.rpm`, built in a container        |
| `../docker/win-docker-compile.cmd`  | Windows  | Windows packages, built in a container        |
