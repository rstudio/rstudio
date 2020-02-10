# some laziness to ensure we move to the 'install-crashpad' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-crashpad")

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

# initialize variables
output_dir <- normalizePath(file.path(owd, ".."), winslash = "\\")
boost_dir <- normalizePath(file.path(output_dir, "boost-1.69.0-win-msvc141-release-static\\boost64"), winslash = "\\")
soci_branch <- "release/4.0"
soci_dir <- file.path(owd, "soci")
soci_build_dir <- file.path(soci_dir, "build")
sqlite_dir <- file.path(owd, "sqlite")
sqlite_header_zip_url <- "https://sqlite.org/2020/sqlite-amalgamation-3310100.zip"
sqlite_header_zip <- file.path(sqlite_dir, "sqlite-header.zip")
sqlite_header_dir <- file.path(sqlite_dir, "sqlite-amalgamation-3310100")

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

if (!file.exists(normalizePath(file.path(soci_build_dir, "lib\\Release\\libsoci_core_4_0.lib"), winslash = "\\"))) {
   # download and install sqlite source
   dir.create(sqlite_dir)
   downloadAndUnzip(sqlite_header_zip, sqlite_dir, sqlite_header_zip_url)
   
   # build SQLite static library
   exec("build-sqlite.cmd")
   
   # clone repository if we dont already have it
   if (!file.exists("soci")) {
      section("Cloning SOCI repository")
      exec("git", "clone https://github.com/SOCI/soci.git")
   }

   # checkout SOCI branch
   setwd(soci_dir)
   exec("git", paste("checkout", soci_branch))
   
   # create build directory
   dir.create("build")
   setwd("build")
   
   # run CMAKE twice - once for debug lib (/MDd) and once for release lib (/MD)
   cmake_args <- sprintf("-G \"Visual Studio 15 2017 Win64\" -DCMAKE_VERBOSE_MAKEFILE=ON -DCMAKE_INCLUDE_PATH=\"%s\" -DBoost_USE_STATIC_LIBS=ON -DCMAKE_LIBRARY_PATH=\"%s\" -DWITH_POSTGRESQL=ON -DWITH_SQLITE3=ON -DSQLITE3_INCLUDE_DIR=\"%s\" -DSQLITE3_LIBRARY=\"%s\" -DSOCI_SHARED=OFF ..", file.path(boost_dir, "include"), file.path(boost_dir, "lib"), sqlite_header_dir, file.path(sqlite_dir, "sqlite3-debug.lib"))
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Debug")
   
   cmake_args <- gsub("sqlite3-debug.lib", "sqlite3-release.lib", cmake_args)
   exec("cmake", cmake_args)
   exec("cmake", "--build . --config Release")
}

progress("SOCI installed successfully!")
