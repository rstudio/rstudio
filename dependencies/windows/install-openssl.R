OWD <- getwd()
URL <- "https://www.openssl.org/source/openssl-1.0.2m.tar.gz"
NAME <- sub(".tar.gz$", "", basename(URL))
DIR <- file.path(tempdir(), "openssl")

source("tools.R")

if (!file.exists("C:/Perl/bin"))
   fatal("No perl installation detected (please install ActiveState Perl from https://www.activestate.com/activeperl/downloads)")

PATH$prepend("C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC")
PATH$prepend("C:/Perl/bin")

# move to a temporary build folder
ensure_dir(DIR)
owd <- setwd(DIR)

# download and extract
if (!file.exists(basename(URL))) {
   section("Downloading OpenSSL")
   download(URL, destfile = basename(URL))
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

section("Building OpenSSL 32bit")
NAME32 <- "build32"
unlink(NAME32, recursive = TRUE)
xcopy(NAME, NAME32)
setwd(NAME32)
exec("vcvarsall.bat", "x86 && perl Configure VC-WIN32", OPTS)
exec("vcvarsall.bat", "x86 && ms\\do_ms.bat")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "x86 && nmake -f ms\\nt.mak install")
setwd("..")

section("Building OpenSSL 64bit")
NAME64 <- "build64"
unlink(NAME64, recursive = TRUE)
xcopy(NAME, NAME64)
setwd(NAME64)
exec("vcvarsall.bat", "amd64 && perl Configure VC-WIN64A", OPTS64)
exec("vcvarsall.bat", "amd64 && ms\\do_win64a.bat")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak test")
exec("vcvarsall.bat", "amd64 && nmake -f ms\\nt.mak install")
setwd("..")

section("Building redistributible")
unlink("dist", recursive = TRUE)
dir.create("dist")
setwd("dist")
xcopy("..\\build32\\build", NAME)
xcopy("..\\build64\\build", file.path(NAME, "x64", fsep = "\\"))
setwd(NAME)
unlink("bin", recursive = TRUE)
unlink("build", recursive = TRUE)
unlink("x64/bin", recursive = TRUE)
unlink("x64/build", recursive = TRUE)
unlink("x64/include", recursive = TRUE)
setwd("..")
zipfile <- sprintf("%s.zip", NAME)
zip(zipfile = zipfile, files = NAME, extras = "-q")
unlink(file.path(OWD, NAME), recursive = TRUE)
file.rename(NAME, file.path(OWD, NAME))
file.rename(zipfile, file.path(OWD, zipfile))
setwd("..")
