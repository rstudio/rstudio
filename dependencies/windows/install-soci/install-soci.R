# some laziness to ensure we move to the 'install-soci' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-soci")

source("../tools.R")
section("The working directory is: '%s'", getwd())
progress("Producing SOCI build")
owd <- getwd()

# initialize log directory (for when things go wrong)
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# try to find MSVC 2019
msvc <- head(Filter(file.exists, c("C:/Program Files (x86)/Microsoft Visual Studio/2019/Community/VC/Auxiliary/Build",
                                   "C:/Program Files (x86)/Microsoft Visual Studio/2019/BuildTools/VC/Auxiliary/Build")), n = 1)
if (length(msvc) == 0)
   fatal("No MSVC 2019 installation detected (please install Visual Studio 2019 using 'Install-RStudio-Prereqs.ps1')")
PATH$prepend(msvc)

# initialize variables
soci_version="4.0.3"
soci_base_name <- paste0("soci-", soci_version)
soci_tar <- paste0(soci_base_name, ".tar")
soci_archive <- paste0(soci_tar, ".gz")
output_dir <- normalizePath(file.path(owd, ".."), winslash = "\\")
boost_dir <- normalizePath(file.path(output_dir, "boost-1.78.0-win-msvc142-release-static\\boost64"), winslash = "\\")
soci_url <- paste0("https://rstudio-buildtools.s3.amazonaws.com/soci-", soci_version, ".tar.gz")
soci_dir <- file.path(owd, soci_base_name)
soci_build_dir <- file.path(soci_dir, "build")
sqlite_dir <- file.path(owd, "sqlite")
postgresql_dir <- file.path(owd, "postgresql")
sqlite_header_zip_url <- "https://sqlite.org/2020/sqlite-amalgamation-3310100.zip"
sqlite_header_zip <- file.path(sqlite_dir, "sqlite-header.zip")
sqlite_header_dir <- file.path(sqlite_dir, "sqlite-amalgamation-3310100")
postgresql_zip <- file.path(owd, "win-postgresql.zip")
postgresql_zip_url <- "https://rstudio-buildtools.s3.amazonaws.com/win-postgresql.zip"

downloadAndUnzip <- function(outputFile, extractDir, url) {
   
   # download zip if we don't already have it
   if (!file.exists(outputFile)) {
      section("Downloading '%s' from '%s'", outputFile, url)
	  download(url, destfile = outputFile)
	  if (!file.exists(outputFile))
	     fatal("Failed to download '%s'", outputFile)
   }

   # extract zip file
   progress("Extracting zip file '%s'", outputFile)
   unzip(outputFile, exdir = extractDir)
}

if (!file.exists(normalizePath(file.path(soci_build_dir, "x64\\lib\\Release\\libsoci_core_4_0.lib"), winslash = "\\", mustWork = FALSE))) {
   
   # download and install sqlite source
   dir.create(sqlite_dir, recursive = TRUE, showWarnings = FALSE)
   downloadAndUnzip(sqlite_header_zip, sqlite_dir, sqlite_header_zip_url)

   # build SQLite static library
   exec("build-sqlite.cmd")

   # download and install postgresql includes/libraries - we prebuild these because the postgresql build process is non-trivial
   downloadAndUnzip(postgresql_zip, owd, postgresql_zip_url)

   # clone repository if we dont already have it
   if (!file.exists(soci_base_name)) {
      section("Downloading SOCI sources")
      download(soci_url, destfile = soci_archive)
      section("Unzipping SOCI sources")
      exec("7z.exe", "-aoa", "e", soci_archive)
      exec("7z.exe", "-aoa", "x", soci_tar)
   }

   # create build directories
   setwd(soci_dir)
   dir.create("build", showWarnings = FALSE, recursive = TRUE)
   setwd("build")
   dir.create("x86", showWarnings = FALSE)
   dir.create("x64", showWarnings = FALSE)

   # run CMAKE for each platform (x86, x64) and each configuration (Debug, Release)
   setwd("x86")
   cmake_args <- paste0("-G \"Visual Studio 16 2019\" ",
                        "-A Win32 ",
                        "-DCMAKE_VERBOSE_MAKEFILE=ON ",
                        "-DCMAKE_INCLUDE_PATH=\"", file.path(boost_dir, "include"), "\" ",
                        "-DBoost_USE_STATIC_LIBS=ON ",
                        "-DCMAKE_LIBRARY_PATH=\"", file.path(boost_dir, "lib"), "\" ",
                        "-DSOCI_TESTS=OFF ",
                        "-DSOCI_SHARED=OFF ",
                        "-DWITH_POSTGRESQL=ON ",
                        "-DWITH_SQLITE3=ON ",
                        "-DSQLITE3_INCLUDE_DIR=\"", sqlite_header_dir, "\" ",
                        "-DSQLITE3_LIBRARY=\"", file.path(sqlite_dir, "sqlite3-debug-x86.lib"), "\" ",
                        "-DPOSTGRESQL_INCLUDE_DIR=\"", file.path(postgresql_dir, "include"), "\" ",
                        "-DPOSTGRESQL_LIBRARY=\"", file.path(postgresql_dir, "lib/x86/Debug/libpq.lib"), "\" ",
                        "..\\..")
   
   # remove rtools from path, as otherwise CMake may find and try to use the
   # standard library headers from the Rtools installation and barf
   path <- strsplit(Sys.getenv("PATH"), ";")[[1]]
   path <- grep("rtools", path, invert = TRUE, value = TRUE)
   Sys.setenv(PATH = paste(path, collapse = ";"))
   
   # x86 debug build
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Debug")

   # x86 release build
   cmake_args <- gsub("sqlite3-debug-x86.lib", "sqlite3-release-x86.lib", cmake_args)
   cmake_args <- gsub("lib/x86/Debug/libpq.lib", "lib/x86/Release/libpq.lib", cmake_args)
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Release")

   # x64 debug build
   setwd(normalizePath("..\\x64", winslash = "\\"))
   cmake_args <- gsub("-A Win32", "-A x64", cmake_args)
   cmake_args <- gsub("sqlite3-release-x86.lib", "sqlite3-debug-x64.lib", cmake_args)
   cmake_args <- gsub("lib/x86/Release/libpq.lib", "lib/x64/Debug/libpq.lib", cmake_args)
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Debug")

   # x64 release build
   cmake_args <- gsub("sqlite3-debug-x64.lib", "sqlite3-release-x64.lib", cmake_args)
   cmake_args <- gsub("lib/x64/Debug/libpq.lib", "lib/x64/Release/libpq.lib", cmake_args)
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Release")
   
}

progress("SOCI installed successfully!")
