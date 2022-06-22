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
boost_url <- "https://s3.amazonaws.com/rstudio-buildtools/Boost/boost_1_78_0.7z"
output_name <- sprintf("boost-1.78.0-win-msvc142-%s-%s.zip", variant, link)
output_dir <- normalizePath(file.path(owd, ".."), winslash = "/")
output_file <- file.path(output_dir, output_name)
install_dir <- file.path(owd, "..", tools::file_path_sans_ext(output_name))

# clear out the directory we'll create boost in
unlink(install_dir, recursive = TRUE)
ensure_dir(install_dir)
install_dir <- normalizePath(install_dir)

# boost modules we need to alias
boost_modules <- c(
   "algorithm", "asio", "array", "bind", "build", "chrono", "circular_buffer",
   "config", "context", "crc", "date_time", "filesystem", "foreach", "format",
   "function", "interprocess", "iostreams", "lambda", "lexical_cast",
   "optional", "predef", "program_options", "property_tree", "random", "range",
   "ref", "regex", "scope_exit", "signals2", "smart_ptr", "spirit",
   "string_algo", "system", "test", "thread", "tokenizer", "type_traits",
   "typeof", "unordered", "utility", "variant"
)

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

# remove any documentation folders (these cause
# bcp to barf while trying to copy files)
unlink("doc", recursive = TRUE)
docs <- file.path("libs", list.files("libs"), "doc")
invisible(lapply(docs, function(doc) {
   if (file.exists(doc))
      unlink(doc, recursive = TRUE)
}))


# bootstrap the boost build directory
section("Bootstrapping boost...")
exec("cmd.exe", "/C call bootstrap.bat vc142")

# create bcp executable (so we can create Boost
# using a private namespace)
exec("b2", "-j 4 tools\\bcp")
invisible(file.copy("dist/bin/bcp.exe", "bcp.exe"))

# use bcp to copy boost into 'rstudio' sub-directory
unlink("rstudio", recursive = TRUE)
dir.create("rstudio")
fmt <- "--namespace=rstudio_boost --namespace-alias %s config build rstudio"
args <- sprintf(fmt, paste(boost_modules, collapse = " "))
exec("bcp", args)

# enter the 'rstudio' directory and re-bootstrap
enter("rstudio")
exec("cmd.exe", "/C call bootstrap.bat vc142")

# construct common arguments for 32bit, 64bit boost builds
b2_build_args <- function(bitness) {
   
   prefix <- file.path(install_dir, sprintf("boost%s", bitness), fsep = "\\")
   unlink(prefix, recursive = TRUE)
   
   paste(
      sprintf("address-model=%s", bitness),
      "toolset=msvc-14.2",
      sprintf("--prefix=\"%s\"", prefix),
      "--abbreviate-paths",
      sprintf("variant=%s", variant),
      sprintf("link=%s", link),
      sprintf("runtime-link=shared", link),
      "threading=multi",
      "define=BOOST_USE_WINDOWS_H",
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
