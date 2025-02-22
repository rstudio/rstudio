
BOOST_VERSION <- Sys.getenv("BOOST_VERSION", unset = "1.87.0")

argument <- function(index, default) {
   args <- commandArgs(TRUE)
   if (length(args) < index)
      default
   else
      args[[index]]
}

# parse some command line arguments
args <- as.list(commandArgs(TRUE))
variant <- argument(1, "debug")
link    <- argument(2, "static")

# some laziness to ensure we move to the 'install-boost' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-boost")

source("../tools.R")
section("The working directory is: '%s'", getwd())
progress("Producing '%s' build with '%s' linking", variant, link)
owd <- getwd()

# initialize log directory (for when things go wrong)
dir.create("logs", showWarnings = FALSE)
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
boost_name <- sprintf("boost_%s.7z", chartr(".", "_", BOOST_VERSION))
boost_url <- sprintf("https://s3.amazonaws.com/rstudio-buildtools/Boost/%s", boost_name)
output_name <- sprintf("boost-%s-win-msvc142-%s-%s.zip", BOOST_VERSION, variant, link)
output_dir <- normalizePath(file.path(owd, ".."), winslash = "/")
output_file <- file.path(output_dir, output_name)
install_dir <- file.path(owd, "..", tools::file_path_sans_ext(output_name))

# clear out the directory we'll create boost in
unlink(install_dir, recursive = TRUE)
ensure_dir(install_dir)
install_dir <- normalizePath(install_dir)

# construct paths of interest
boost_filename <- basename(boost_url)
boost_dirname <- tools::file_path_sans_ext(boost_filename)
boost_archive <- boost_filename

if (!file.exists(boost_dirname)) {
   
   # download boost if we don't have it
   if (!file.exists(boost_archive)) {
      section("Downloading Boost...")
      download(boost_url, destfile = boost_archive)
      if (!file.exists(boost_archive))
         fatal("Failed to download '%s'", boost_archive)
   }
   
   # extract boost (be patient, this will take a while)
   progress("Extracting archive -- please wait a moment...")
   progress("Feel free to get up and grab a coffee.")
   exec("7z.exe", "x -mmt4", boost_filename)
   
}

# double-check that we generated the boost folder
if (!file.exists(boost_dirname))
   fatal("'%s' doesn't exist (download / extract failed?)", boost_dirname)

# enter boost folder
enter(boost_dirname)

# bootstrap the boost build directory
section("Bootstrapping boost...")

# TODO: Boost has trouble finding the vcvarsall.bat script for some reason?
# We set this environment variable here to help it find the tools.
if (is.na(Sys.getenv("B2_TOOLSET_ROOT", unset = NA))) {
   
   candidates <- c(
      "C:/Program Files/Microsoft Visual Studio/2022/Community/VC/",
      "C:/Program Files (x86)/Microsoft Visual Studio/2019/Community/VC/",
      "C:/Program Files (x86)/Microsoft Visual Studio/2019/BuildTools/VC/"
   )
   
   for (candidate in candidates) {
      if (file.exists(candidate)) {
         Sys.setenv(B2_TOOLSET_ROOT = candidate)
         progress("Using B2_TOOLSET_ROOT = %s", candidate)
         break
      }
   }
   
}

# use rstudio_boost for namespaces
progress("Patching Boost namespaces")
source("../../../tools/use-rstudio-boost-namespace.R")

# bootstrap the project
# TODO: system2() seems to hang and never exits, so we use processx
args <- c("/c", "call", "bootstrap.bat", "vc142")
result <- processx::run("cmd.exe", args, stdout = "", stderr = "")
if (result$status != 0L)
   stop("Error bootstrapping Boost. Sorry.")

# construct common arguments for 32bit, 64bit boost builds
b2_build_args <- function(bitness) {
   
   prefix <- file.path(install_dir, sprintf("boost%s", bitness), fsep = "\\")
   unlink(prefix, recursive = TRUE)
   
   paste(
      "-q",
      "--without-graph_parallel",
      "--without-mpi",
      "--without-python",
      "--abbreviate-paths",
      sprintf("--prefix=\"%s\"", prefix),
      sprintf("address-model=%s", bitness),
      "toolset=msvc-14.2",
      sprintf("variant=%s", variant),
      sprintf("link=%s", link),
      "runtime-link=shared",
      "threading=multi",
      "define=BOOST_USE_WINDOWS_H",
      "define=NOMINMAX",
      "install"
   )
}

# build 32bit Boost
section("Building Boost 32bit...")
exec("b2", b2_build_args("32"))

# build 64bit Boost
section("Building Boost 64bit...")
exec("b2", b2_build_args("64"))

# rejoice
progress("Boost built successfully!")
