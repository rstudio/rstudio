OWD <- getwd()
URL <- "https://www.openssl.org/source/openssl-1.1.1t.tar.gz"
NAME <- sub(".tar.gz$", "", basename(URL))

source("../tools.R")
dir.create("logs", showWarnings = FALSE)
options(log.dir = normalizePath("logs", winslash = "/"))

# try to find a perl installation directory
perl <- head(Filter(file.exists, c("C:/Perl64/bin", "C:/Perl/bin")), n = 1)
if (length(perl) == 0)
   fatal("No perl installation detected (please install ActiveState Perl via 'choco install activeperl')")

# try to find MSVC 2019
msvc <- head(Filter(file.exists, c("C:/Program Files (x86)/Microsoft Visual Studio/2019/Community/VC/Auxiliary/Build",
                                   "C:/Program Files (x86)/Microsoft Visual Studio/2019/BuildTools/VC/Auxiliary/Build")), n = 1)
if (length(msvc) == 0)
   fatal("No MSVC 2019 installation detected (please install Visual Studio 2019 using 'Install-RStudio-Prereqs.ps1')")

PATH$prepend("../tools")
PATH$prepend(msvc)
PATH$prepend(perl)

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
