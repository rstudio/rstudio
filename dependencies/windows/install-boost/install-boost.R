
BOOST_VERSION <- Sys.getenv("BOOST_VERSION", unset = "1.87.0")
BOOST_TOOLSET <- Sys.getenv("BOOST_TOOLSET", unset = "msvc-14.3")
MSVC_TOOLSET_VERSION <- Sys.getenv("MSVC_TOOLSET_VERSION", unset = "143")

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
owd <- getwd()

section("Building Boost %s [msvc%s-%s-%s]", BOOST_VERSION, MSVC_TOOLSET_VERSION, variant, link)

# initialize log directory (for when things go wrong)
dir.create("logs", showWarnings = FALSE)
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
boost_name <- sprintf("boost_%s.7z", chartr(".", "_", BOOST_VERSION))
boost_url <- sprintf("https://s3.amazonaws.com/rstudio-buildtools/Boost/%s", boost_name)
output_name <- sprintf("boost-%s-win-msvc%s-%s-%s.zip", BOOST_VERSION, MSVC_TOOLSET_VERSION, variant, link)
output_dir <- normalizePath(file.path(owd, ".."), winslash = "/")
output_file <- file.path(output_dir, output_name)
install_dir <- file.path(owd, "..", tools::file_path_sans_ext(output_name))

# clear out the directory we'll create boost in
unlink(install_dir, recursive = TRUE)
dir.create(install_dir, recursive = TRUE, showWarnings = FALSE)
install_dir <- normalizePath(install_dir)

# construct paths of interest
boost_filename <- basename(boost_url)
boost_dirname <- tools::file_path_sans_ext(boost_filename)
boost_archive <- boost_filename

if (!file.exists(boost_dirname)) {

   # download boost if we don't have it
   if (!file.exists(boost_archive)) {
      section("Downloading Boost")
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

# TODO: Boost has trouble finding the vcvarsall.bat script for some reason?
# We set this environment variable here to help it find the tools.
if (is.na(Sys.getenv("B2_TOOLSET_ROOT", unset = NA))) {
   vcVarsAll <- normalizePath(Sys.which("vcvarsall.bat"), winslash = "/")
   if (nzchar(vcVarsAll)) {
      toolsetRoot <- gsub("VC/.*", "VC/", vcVarsAll)
      Sys.setenv(B2_TOOLSET_ROOT = toolsetRoot)
   }
}

# use rstudio_boost for namespaces
section("Patching Boost namespaces")
source("../../../tools/use-rstudio-boost-namespace.R")

# bootstrap the project
section("Bootstrapping boost")
run("bootstrap.bat", paste0("vc", MSVC_TOOLSET_VERSION))

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
      sprintf("toolset=%s", BOOST_TOOLSET),
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
section("Building Boost 32bit")
exec("b2", b2_build_args("32"))

# build 64bit Boost
section("Building Boost 64bit")
exec("b2", b2_build_args("64"))

# rejoice
progress("Boost built successfully!")
