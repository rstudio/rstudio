
# some laziness to ensure we move to the 'install-soci' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-soci")

SOCI_VERSION    <- Sys.getenv("SOCI_VERSION", unset = "4.0.3")
BOOST_VERSION   <- Sys.getenv("BOOST_VERSION", unset = "1.87.0")
MSVC_VERSION    <- Sys.getenv("MSVC_VERSION", unset = "vc142")
CMAKE_GENERATOR <- Sys.getenv("CMAKE_GENERATOR", unset = "Visual Studio 16 2019")

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

boost_name <- sprintf("boost-%s-win-ms%s-release-static/boost64", BOOST_VERSION, MSVC_VERSION)
boost_dir <- file.path(output_dir, boost_name)
boost_dir <- normalizePath(boost_dir, winslash = "/")
boost_include_dir <- file.path(boost_dir, "include")
boost_library_dir <- file.path(boost_dir, "lib")
      
soci_base_name <- paste0("soci-", SOCI_VERSION)
soci_tar <- paste0(soci_base_name, ".tar")
soci_archive <- paste0(soci_tar, ".gz")
soci_url <- paste0("https://rstudio-buildtools.s3.amazonaws.com/soci-", SOCI_VERSION, ".tar.gz")
soci_dir <- file.path(owd, soci_base_name)
soci_build_dir <- file.path(soci_dir, "build")

sqlite_dir <- file.path(owd, "sqlite")
sqlite_header_zip_url <- "https://sqlite.org/2020/sqlite-amalgamation-3310100.zip"
sqlite_header_zip <- file.path(sqlite_dir, "sqlite-header.zip")
sqlite_header_dir <- file.path(sqlite_dir, "sqlite-amalgamation-3310100")

postgresql_dir <- file.path(owd, "postgresql")
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

soci_core_lib <- file.path(soci_build_dir, "x64/lib/Release/libsoci_core_4_0.lib")
if (file.exists(soci_core_lib)) {
   progress("SOCI is already installed; nothing to do.")
   quit()
}

# remove Rtools from the path, as it can confuse cmake and cause it
# to find standard library headers in the wrong location
path <- strsplit(Sys.getenv("PATH"), ";", fixed = TRUE)[[1L]]
path <- grep("C:\\\\(?:rtools|rbuildtools)", path, value = TRUE, invert = TRUE, ignore.case = TRUE)
Sys.setenv(PATH = paste(path, collapse = ";"))

   
# download and install sqlite source
dir.create(sqlite_dir, recursive = TRUE, showWarnings = FALSE)
downloadAndUnzip(sqlite_header_zip, sqlite_dir, sqlite_header_zip_url)

# build SQLite static library
system("cmd.exe /c build-sqlite.cmd")

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
unlink("build", recursive = TRUE)
dir.create("build", showWarnings = FALSE, recursive = TRUE)
setwd("build")

build <- function(arch, config) {
   
   # compute architecture
   winarch <- if (arch == "x86") "Win32" else "x64"
   
   # build cmake arguments
   sqlite_library_name <- sprintf("sqlite3-%s-%s.lib", tolower(config), arch)
   sqlite_library_path <- file.path(sqlite_dir, sqlite_library_name)
   
   postgresql_include_dir <- file.path(postgresql_dir, "include")
   postgresql_library_name <- sprintf("lib/%s/%s/libpq.lib", arch, config)
   postgresql_library_path <- file.path(postgresql_dir, "lib/x86/Debug/libpq.lib")
   
   # put together intro big string
   args <- interpolate('
      -G "{CMAKE_GENERATOR}"
      -A {winarch}
      -DCMAKE_VERBOSE_MAKEFILE=ON
      -DCMAKE_INCLUDE_PATH="{boost_include_dir}"
      -DCMAKE_LIBRARY_PATH="{boost_library_dir}"
      -DCMAKE_CXX_FLAGS="/FS /EHsc"
      -DSOCI_TESTS=OFF
      -DSOCI_SHARED=OFF
      -DWITH_POSTGRESQL=ON
      -DWITH_SQLITE3=ON
      -DBoost_USE_STATIC_LIBS=ON
      -DSQLITE3_INCLUDE_DIR="{sqlite_header_dir}"
      -DSQLITE3_LIBRARY="{sqlite_library_path}"
      -DPOSTGRESQL_INCLUDE_DIR="{postgresql_include_dir}"
      -DPOSTGRESQL_LIBRARY="{postgresql_library_path}"
      ..\\..
   ')
   
   args <- gsub("\\s+", " ", args, perl = TRUE)
   
   # move to build directory
   dir.create(arch, showWarnings = FALSE)
   owd <- setwd(arch)
   on.exit(setwd(owd), add = TRUE)

   # build it -- we run the process through 'pipe()' because, for whatever
   # reason, 'system()' seemed to stall after trying to run cmake.
   progress(sprintf("Configuring SOCI [%s-%s]", arch, config))
   command <- paste("cmake", args, "2>&1")
   conn <- pipe(command, open = "rb")
   
   output <- ""
   while (length(output)) {
      output <- readLines(conn, n = 1L)
      writeLines(output)
   }
   
   progress(sprintf("Building SOCI [%s-%s]", arch, config))
   command <- paste("cmake --build . --config", config, "2>&1")
   conn <- pipe(command, open = "rb")
   
   output <- ""
   while (length(output)) {
      output <- readLines(conn, n = 1L)
      writeLines(output)
   }
   
}

build("x86", "Debug")
build("x86", "Release")

build("x64", "Debug")
build("x64", "Release")

