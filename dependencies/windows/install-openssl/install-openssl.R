OWD <- getwd()
URL <- "https://www.openssl.org/source/old/1.0.2/openssl-1.0.2m.tar.gz"
NAME <- sub(".tar.gz$", "", basename(URL))

source("../tools.R")
dir.create("logs", showWarnings = FALSE)
options(log.dir = normalizePath("logs", winslash = "/"))

if (!file.exists("C:/Perl/bin"))
   fatal("No perl installation detected (please install ActiveState Perl from https://www.activestate.com/activeperl/downloads)")

PATH$prepend("../tools")
PATH$prepend("C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC")
PATH$prepend("C:/Perl/bin")

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

OPTS <- "no-asm no-shared -DUNICODE -D_UNICODE --prefix=build"

section("Building OpenSSL 32bit (Debug)")
TARGET <- sprintf("build-%s-debug-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
exec("vcvarsall.bat", "x86 && perl Configure debug-VC-WIN32 -d", OPTS)
exec("vcvarsall.bat", "x86 && ms\\do_ms.bat")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak install")
setwd("..")

section("Building OpenSSL 64bit (Debug)")
TARGET <- sprintf("build-%s-debug-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
exec("vcvarsall.bat", "amd64 && perl Configure debug-VC-WIN64A -d", OPTS)
exec("vcvarsall.bat", "amd64 && ms\\do_win64a.bat")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak install")
setwd("..")

section("Building OpenSSL 32bit (Release)")
TARGET <- sprintf("build-%s-release-32", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
exec("vcvarsall.bat", "x86 && perl Configure VC-WIN32", OPTS)
exec("vcvarsall.bat", "x86 && ms\\do_ms.bat")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak install")
setwd("..")

section("Building OpenSSL 64bit (Release)")
TARGET <- sprintf("build-%s-release-64", NAME)
unlink(TARGET, recursive = TRUE)
xcopy(NAME, TARGET)
setwd(TARGET)
exec("vcvarsall.bat", "amd64 && perl Configure VC-WIN64A", OPTS)
exec("vcvarsall.bat", "amd64 && ms\\do_win64a.bat")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak install")
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
