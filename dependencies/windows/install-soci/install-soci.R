
# some laziness to ensure we move to the 'install-soci' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-soci")

SOCI_VERSION         <- Sys.getenv("SOCI_VERSION", unset = "4.0.3")
BOOST_VERSION        <- Sys.getenv("BOOST_VERSION", unset = "1.87.0")
MSVC_TOOLSET_VERSION <- Sys.getenv("MSVC_TOOLSET_VERSION", unset = "143")
CMAKE_GENERATOR      <- Sys.getenv("CMAKE_GENERATOR", unset = "Visual Studio 17 2022")

owd <- getwd()
source("../tools.R")
section("Building SOCI %s", SOCI_VERSION)

# initialize log directory (for when things go wrong)
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
output_dir <- normalizePath("..", winslash = "/")

soci_base_name <- paste0("soci-", SOCI_VERSION)
soci_tar <- paste0(soci_base_name, ".tar")
soci_archive <- paste0(soci_tar, ".gz")
soci_url <- paste0("https://rstudio-buildtools.s3.amazonaws.com/soci-", SOCI_VERSION, ".tar.gz")
soci_dir <- file.path(owd, soci_base_name)
soci_install_name <- sprintf("soci-msvc%s-%s", MSVC_TOOLSET_VERSION, SOCI_VERSION)
soci_install_dir <- file.path(owd, soci_install_name)
soci_build_dir <- file.path(soci_install_dir, "build")

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
   if (!interactive()) quit()
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
run("build-sqlite.cmd")

# download and install postgresql includes/libraries
# we prebuild these because the postgresql build process is non-trivial
downloadAndUnzip(postgresql_zip, owd, postgresql_zip_url)

# clone repository if we dont already have it
if (!file.exists(soci_base_name)) {
   section("Downloading SOCI sources")
   download(soci_url, destfile = soci_archive)
   section("Unzipping SOCI sources")
   exec("7z.exe", "-aoa", "e", soci_archive)
   exec("7z.exe", "-aoa", "x", soci_tar)
}

# move extracted directory
file.rename(soci_dir, soci_install_dir)
setwd(soci_install_dir)

# create build directories
unlink("build", recursive = TRUE)
dir.create("build", showWarnings = FALSE, recursive = TRUE)
setwd("build")

build <- function(arch, config) {

   # compute architecture
   winarch <- if (arch == "x86") "Win32" else "x64"

   # build cmake arguments
   boost_arch <- if (arch == "x86") "boost32" else "boost64"
   boost_name <- sprintf("boost-%s-win-msvc%s-%s-static", BOOST_VERSION, MSVC_TOOLSET_VERSION, tolower(config))
   boost_root <- file.path(output_dir, boost_name, boost_arch)
   boost_include_dir <- list.files(file.path(boost_root, "include"), full.names = TRUE)
   boost_library_dir <- file.path(boost_root, "lib")

   boost_version_major <- format(numeric_version(BOOST_VERSION)[1, 1])
   boost_version_minor <- format(numeric_version(BOOST_VERSION)[1, 2])
   boost_version_patch <- format(numeric_version(BOOST_VERSION)[1, 3])

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
      -DCMAKE_POLICY_VERSION_MINIMUM=3.5
      -DCMAKE_CXX_FLAGS="/FS /EHsc"
      -DBOOST_INCLUDEDIR="{boost_include_dir}"
      -DBOOST_LIBRARYDIR="{boost_library_dir}"
      -DBoost_MAJOR_VERSION="{boost_version_major}"
      -DBoost_MINOR_VERSION="{boost_version_minor}"
      -DBoost_SUBMINOR_VERSION="{boost_version_patch}"
      -DBoost_USE_STATIC_LIBS=ON
      -DSOCI_TESTS=OFF
      -DSOCI_SHARED=OFF
      -DWITH_POSTGRESQL=ON
      -DWITH_SQLITE3=ON
      -DSQLITE3_INCLUDE_DIR="{sqlite_header_dir}"
      -DSQLITE3_LIBRARY="{sqlite_library_path}"
      -DPOSTGRESQL_INCLUDE_DIR="{postgresql_include_dir}"
      -DPOSTGRESQL_LIBRARY="{postgresql_library_path}"
      ../..
   ')

   args <- gsub("\\s+", " ", args, perl = TRUE)

   # move to build directory
   dir.create(arch, showWarnings = FALSE)
   owd <- setwd(arch)
   on.exit(setwd(owd), add = TRUE)

   progress(sprintf("Configuring SOCI [%s-%s]", arch, config))
   run("cmake", args)

   progress(sprintf("Building SOCI [%s-%s]", arch, config))
   run("cmake --build . --config", config)

}

build("x86", "Debug")
build("x86", "Release")

build("x64", "Debug")
build("x64", "Release")
