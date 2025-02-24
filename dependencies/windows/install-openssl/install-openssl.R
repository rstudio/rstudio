
# in case we're invoked from the root of the project
if (file.exists("dependencies/windows/install-openssl"))
   setwd("dependencies/windows/install-openssl")

OWD <- getwd()
URL <- "https://www.openssl.org/source/openssl-3.1.4.tar.gz"
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

section("Building OpenSSL 32bit (Debug)")
TARGET <- sprintf("build-%s-debug-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
prefix <- file.path(getwd(), "build")
openssldir <- file.path(prefix, "SSL")
OPTS <- paste("no-asm no-shared -DUNICODE -D_UNICODE --prefix=", prefix, " --openssldir=", openssldir, sep = "")
exec("vcvarsall.bat", "x86 && perl Configure debug-VC-WIN32 -d", OPTS)
exec("vcvarsall.bat", "x86 && nmake")
exec("vcvarsall.bat", "x86 && nmake test")
exec("vcvarsall.bat", "x86 && nmake install")
setwd("..")

section("Building OpenSSL 64bit (Debug)")
TARGET <- sprintf("build-%s-debug-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
prefix <- file.path(getwd(), "build")
openssldir <- file.path(prefix, "SSL")
OPTS <- paste("no-asm no-shared -DUNICODE -D_UNICODE --prefix=", prefix, " --openssldir=", openssldir, sep = "")
exec("vcvarsall.bat", "amd64 && perl Configure debug-VC-WIN64A -d", OPTS)
exec("vcvarsall.bat", "amd64 && nmake")
exec("vcvarsall.bat", "amd64 && nmake test")
exec("vcvarsall.bat", "amd64 && nmake install")
setwd("..")

section("Building OpenSSL 32bit (Release)")
TARGET <- sprintf("build-%s-release-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
prefix <- file.path(getwd(), "build")
openssldir <- file.path(prefix, "SSL")
OPTS <- paste("no-asm no-shared -DUNICODE -D_UNICODE --prefix=", prefix, " --openssldir=", openssldir, sep = "")
exec("vcvarsall.bat", "x86 && perl Configure VC-WIN32", OPTS)
exec("vcvarsall.bat", "x86 && nmake")
exec("vcvarsall.bat", "x86 && nmake test")
exec("vcvarsall.bat", "x86 && nmake install")
setwd("..")

section("Building OpenSSL 64bit (Release)")
TARGET <- sprintf("build-%s-release-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
prefix <- file.path(getwd(), "build")
openssldir <- file.path(prefix, "SSL")
OPTS <- paste("no-asm no-shared -DUNICODE -D_UNICODE --prefix=", prefix, " --openssldir=", openssldir, sep = "")
exec("vcvarsall.bat", "amd64 && perl Configure VC-WIN64A", OPTS)
exec("vcvarsall.bat", "amd64 && nmake")
exec("vcvarsall.bat", "amd64 && nmake test")
exec("vcvarsall.bat", "amd64 && nmake install")
setwd("..")

section("Building redistributible")
unlink("dist", recursive = TRUE)
dir.create(file.path("dist", NAME), recursive = TRUE)
dirs <- list.files(pattern = sprintf("^build-%s-", NAME))
lapply(dirs, function(dir) {
   src <- file.path(dir, "build", fsep = "\\")
   dst <- file.path("dist", NAME, sub("^build-", "", dir), fsep = "\\")
   xcopy(src, dst)
   unlink(file.path(dst, "bin"), recursive = TRUE)
   unlink(file.path(dst, "build"), recursive = TRUE)
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
