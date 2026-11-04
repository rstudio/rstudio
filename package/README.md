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

By default the build targets the machine's own architecture: `arm64` on an
Apple Silicon Mac, `x86_64` on an Intel Mac. Jenkins builds both on Apple
Silicon to produce a universal application. Request that locally with
`--arch`, after installing both sets of dependencies as described in
`dependencies/osx/README.md`:

```bash
./make-package --arch=x86_64,arm64
```

Other options, from `./make-package --help`:

- `clean` performs a full rebuild instead of an incremental one.
- `--install` copies the built application to `/Applications/RStudio-Devel.app`,
  where it can coexist with a released RStudio.
- `--build-dmg=0` produces the application bundle but skips the disk image.
- `--build-gwt=0` reuses the previous GWT build, which saves several minutes
  when you are only iterating on C++ or Electron code.

The build produces three artifacts:

- the application bundle at `package/osx/install/RStudio.app`
- the disk image at `package/osx/build/RStudio-<version>.dmg`, or
  `package/osx/build-arm64/` when `arm64` is the only architecture built
- a zip of the bundle next to the disk image, `RStudio-<version>.zip`, which
  is what the in-app updater installs from (see
  [In-app update feed](#in-app-update-feed))

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

## In-app update feed

Open-source RStudio Desktop on macOS and Windows can download and install
updates itself (`src/node/desktop/src/main/updater.ts`); Pro desktop builds
(a `.pro` version suffix) are left to Workbench's own distribution. It reads
the JSON API of the builds
site, <https://dailies.rstudio.com/json-api/>, which already lists every
published build with its download link and SHA-256:

- release builds: `https://dailies.rstudio.com/api/v1/release/index.json`,
  whose `released[0].published[0].builds_url` is the current release's newest
  published build (a released branch's own index also lists unpublished patch
  candidates, so the updater does not use it);
- daily and preview builds: `https://dailies.rstudio.com/rstudio/<branch>/index.json`;
- hourly builds: `https://dailies.rstudio.com/rstudio-hourly/<branch>/index.json`.

The channel comes from the version's pre-release tag (`release` when there is
none) and the entry from
`products.electron.platforms`: `macos` and `macos-zip` on macOS, `windows` on
Windows. On macOS the zipped `.app` is handed to Electron's `autoUpdater`
(Squirrel.Mac) through a loopback listener that serves the feed shape Squirrel
expects; on Windows the desktop downloads the NSIS installer, checks its
SHA-256, and runs it with `/S /update` once RStudio has quit. When the API has
no entry for a build (Linux, developer builds, a branch not yet on the site),
the desktop falls back to the session's own check and the "Quit and Download"
dialog; when only the DMG is listed, that dialog links to it directly.

The branch is `version/RELEASE` for preview builds. Daily and hourly builds
instead follow the branch the release index's `in_development` list says is
receiving daily builds, since `version/new-version.sh` moves them to a new
branch at every release cut and a build that kept polling its own branch would
report itself up to date forever. A branch that the release index lists as
released is read through the release API rather than its own index (which
goes on listing unpublished patch candidates), and a branch index whose newest
build is not on the build's own channel -- release candidates, say -- offers
nothing.

For release builds the desktop's answer is marked `confirm-with-session`, and
GWT runs the session's check (rstudio.org's `check_for_update`) as well: that
is where Posit withdraws a release or marks one urgent, which the builds site
does not know, so its answer decides whether the installable update is
offered and how urgently. Daily and preview builds are offered from the builds
site alone.

On Windows the installer is staged under `%TEMP%\rstudio-updates\<key>`,
where the key is derived from the install directory, so every window of one
installation (each is its own process) shares it while another copy of RStudio
on the machine does not: a verified installer already there is reused,
downloads land under a private name until verified, and installers no newer
than the running build are pruned at startup. A download that delivers nothing
for a minute is abandoned. The checksum only proves the file is what the API
named, and the installer runs silently (with no UAC prompt for a per-user
install), so a staged installer must also carry a valid Authenticode signature
from Posit's certificate (checked with PowerShell's
`Get-AuthenticodeSignature`, as Squirrel.Mac checks the new bundle's signature
on macOS); one that does not is deleted and the download reported as failed.
"Restart Now" first checks that the staged installer still verifies
(downloading it again otherwise); a file unchanged in size and modification
time since it last hashed correctly is trusted without being hashed again. An
all-users install elevates, so it is only updated in-app for a user in the
Administrators group; anyone else is sent to the download page. Should the
installer still fail (a declined UAC prompt, an install error), it reopens the
RStudio it was updating and says so. On both
platforms it also checks that no other window of this installation is running
(`tasklist` on Windows, `ps` on macOS), because the NSIS installer refuses to
run beside a live `rstudio.exe` and Squirrel.Mac waits for every process of
the bundle to exit; the check runs again after the save prompts, and a check
that cannot be made blocks the restart rather than letting the installer give
up silently. On both platforms the active project
is written to `relaunch-project/<install key>.json` in the Electron user-data
directory before quitting and reopened, once, by the relaunched RStudio -- the
marker names the build and installation it is for (and the build the update
was attempted from, which the Windows installer brings back when the update
fails), so an unrelated launch in between leaves it alone, and deleting it is
what claims it, so two launches racing for it reopen the project once. A quit the session refuses cancels the
pending install (the download stays).

The macOS zip is produced for open-source builds only and published to the
builds site by the Jenkins pipeline as the `electron/macos-zip` platform, next
to the DMG. The site itself names each platform in a per-branch `_index.md`
(in the `rstudio/latest-builds` repository), so a new platform needs one there
before it is displayed.

Administrators can keep the update notification but send users to the download
page instead with the `RSTUDIO_DISABLE_IN_APP_UPDATES` environment variable
(packaged Desktop builds do not read an `rsession.conf`, so there is no option
for it); the existing `RSTUDIO_DISABLE_CHECK_FOR_UPDATES` still turns update
checks off entirely.

For testing a build against another channel or copy of the API, the desktop
honours `RSTUDIO_UPDATE_API_URL`, `RSTUDIO_UPDATE_CHANNEL` (also enables
updates for `-dev` builds), `RSTUDIO_UPDATE_BRANCH`, and
`RSTUDIO_UPDATE_VERSION` (impersonate a version, as for the session's check).

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
