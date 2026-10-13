
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

build <- function(variant, arch, vcvars, target, test = TRUE) {
   section("Building OpenSSL %s (%s)", arch, variant)
   TARGET <- sprintf("build-%s-%s-%s", NAME, variant, arch)
   unlink(TARGET, recursive = TRUE)
   xcopy(NAME, TARGET)
   setwd(TARGET)
   destdir <- normalizePath(file.path(getwd(), "build"), winslash = "\\", mustWork = FALSE)
   exec("vcvarsall.bat", sprintf("%s && perl Configure %s", vcvars, target), OPTS)
   exec("vcvarsall.bat", sprintf("%s && nmake", vcvars))
   if (test)
      exec("vcvarsall.bat", sprintf("%s && nmake test", vcvars))
   exec("vcvarsall.bat", sprintf("%s && nmake install_sw DESTDIR=%s", vcvars, destdir))
   setwd("..")
}

if (identical(Sys.getenv("OPENSSL_ARCH"), "arm64")) {
   # the debug test suite takes hours without assembly; the release run covers it
   build("debug",   "arm64", "arm64", "debug-VC-WIN64-ARM -d", test = FALSE)
   build("release", "arm64", "arm64", "VC-WIN64-ARM")
} else {
   build("debug",   "32", "x86",   "debug-VC-WIN32 -d")
   build("debug",   "64", "amd64", "debug-VC-WIN64A -d")
   build("release", "32", "x86",   "VC-WIN32")
   build("release", "64", "amd64", "VC-WIN64A")
}

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
   target <- file.path(OWD, "..", name)
   unlink(target, recursive = TRUE)
   if (file.exists(target))
      fatal("failed to remove %s; close anything using it and re-run", shQuote(target))
   if (!file.rename(name, target))
      fatal("failed to move %s to %s", shQuote(name), shQuote(target))
}

install(NAME)
install(zipfile)

setwd("..")
