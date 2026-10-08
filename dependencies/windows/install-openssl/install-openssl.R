
# in case we're invoked from the root of the project
if (file.exists("dependencies/windows/install-openssl"))
   setwd("dependencies/windows/install-openssl")

OWD <- getwd()
URL <- "https://www.openssl.org/source/openssl-3.5.9.tar.gz"
NAME <- sub(".tar.gz$", "", basename(URL))

source("../tools.R")
dir.create("logs", showWarnings = FALSE)
options(log.dir = normalizePath("logs", winslash = "/"))

PATH$prepend("../tools")

# download and extract
if (!file.exists(basename(URL))) {
   section("Downloading OpenSSL")
   download.file(URL, destfile = basename(URL))
}

section("Extracting OpenSSL")
unlink(NAME, recursive = TRUE)
untar(basename(URL))

xcopy <- function(src, dst) {
   fmt <- "xcopy %s %s /E /I /Y /S"
   cmd <- sprintf(fmt, src, dst)
   exec("cmd.exe", "/C", shQuote(cmd))
}

# OpenSSL compiles its install paths into libcrypto, and at runtime reads
# openssl.cnf and loads engines and modules from them. Keep its admin-protected
# Program Files defaults (no --prefix/--openssldir) and stage the install
# under each build tree with DESTDIR instead. RStudio ships no openssl.cnf,
# so no-autoload-config also stops OpenSSL from loading one implicitly; only
# an explicit config-load call in code would still read one.
OPTS <- "no-asm no-shared no-autoload-config -DUNICODE -D_UNICODE"

section("Building OpenSSL 32bit (Debug)")
TARGET <- sprintf("build-%s-debug-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
destdir <- normalizePath(file.path(getwd(), "build"), winslash = "\\", mustWork = FALSE)
exec("vcvarsall.bat", "x86 && perl Configure debug-VC-WIN32 -d", OPTS)
exec("vcvarsall.bat", "x86 && nmake")
exec("vcvarsall.bat", "x86 && nmake test")
exec("vcvarsall.bat", paste0("x86 && nmake install_sw DESTDIR=", destdir))
setwd("..")

section("Building OpenSSL 64bit (Debug)")
TARGET <- sprintf("build-%s-debug-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
destdir <- normalizePath(file.path(getwd(), "build"), winslash = "\\", mustWork = FALSE)
exec("vcvarsall.bat", "amd64 && perl Configure debug-VC-WIN64A -d", OPTS)
exec("vcvarsall.bat", "amd64 && nmake")
exec("vcvarsall.bat", "amd64 && nmake test")
exec("vcvarsall.bat", paste0("amd64 && nmake install_sw DESTDIR=", destdir))
setwd("..")

section("Building OpenSSL 32bit (Release)")
TARGET <- sprintf("build-%s-release-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
destdir <- normalizePath(file.path(getwd(), "build"), winslash = "\\", mustWork = FALSE)
exec("vcvarsall.bat", "x86 && perl Configure VC-WIN32", OPTS)
exec("vcvarsall.bat", "x86 && nmake")
exec("vcvarsall.bat", "x86 && nmake test")
exec("vcvarsall.bat", paste0("x86 && nmake install_sw DESTDIR=", destdir))
setwd("..")

section("Building OpenSSL 64bit (Release)")
TARGET <- sprintf("build-%s-release-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
destdir <- normalizePath(file.path(getwd(), "build"), winslash = "\\", mustWork = FALSE)
exec("vcvarsall.bat", "amd64 && perl Configure VC-WIN64A", OPTS)
exec("vcvarsall.bat", "amd64 && nmake")
exec("vcvarsall.bat", "amd64 && nmake test")
exec("vcvarsall.bat", paste0("amd64 && nmake install_sw DESTDIR=", destdir))
setwd("..")

section("Building redistributible")
unlink("dist", recursive = TRUE)
dir.create(file.path("dist", NAME), recursive = TRUE)
dirs <- list.files(pattern = sprintf("^build-%s-", NAME))
lapply(dirs, function(dir) {
   # DESTDIR staging mirrors the default install path, whose folder name comes
   # from the build machine's ProgramW6432 / ProgramFiles(x86), so find it
   # rather than guess it; it has spaces, so move it with R, not xcopy
   src <- Sys.glob(file.path(dir, "build", "*", "OpenSSL"))
   if (length(src) != 1L)
      fatal("expected one staged OpenSSL install under %s; found %i",
            shQuote(file.path(dir, "build")), length(src))
   dst <- file.path("dist", NAME, sub("^build-", "", dir))
   if (!file.rename(src, dst))
      fatal("failed to move %s to %s", shQuote(src), shQuote(dst))
   unlink(file.path(dst, "bin"), recursive = TRUE)
})

setwd("dist")
zipfile <- sprintf("%s.zip", NAME)
zip(zipfile = zipfile, files = NAME, extras = "-q")

install <- function(name) {
   unlink(file.path(OWD, "..", name), recursive = TRUE)
   file.rename(name, file.path(OWD, "..", name))
}

install(NAME)
install(zipfile)

setwd("..")
