# some laziness to ensure we move to the 'install-boost' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-boost")

source("tools.R")
section("The working directory is: '%s'", getwd())
owd <- getwd()

# initialize log directory (for when things go wrong)
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
boost_url <- "https://dl.bintray.com/boostorg/release/1.65.1/source/boost_1_65_1.7z"
output_name <- "boost-1.65.1-win-vc14.zip"
output_dir <- owd
output_file <- file.path(output_dir, output_name)
build_dir <- file.path(output_dir, tools::file_path_sans_ext(output_name))

# clear out the directory we'll create boost in
unlink(build_dir, recursive = TRUE)
ensure_dir(build_dir)
build_dir <- normalizePath(build_dir)

# boost modules we need to alias
boost_modules <- c(
   "algorithm", "asio", "array", "bind", "chrono", "circular_buffer",
   "context", "crc", "date_time", "filesystem", "foreach", "format",
   "function", "interprocess", "iostreams", "lambda", "lexical_cast",
   "optional", "program_options", "random", "range", "ref", "regex",
   "scope_exit", "signals", "smart_ptr", "spirit", "string_algo",
   "system", "test", "thread", "tokenizer", "type_traits", "typeof",
   "unordered", "utility", "variant"
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
exec("cmd.exe", "/C call bootstrap.bat vc14")

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
exec("cmd.exe", "/C call bootstrap.bat vc14")

# construct common arguments for 32bit, 64bit boost builds
b2_build_args <- function(bitness) {
   
   prefix <- file.path(build_dir, sprintf("boost%s", bitness), fsep = "\\")
   unlink(prefix, recursive = TRUE)
   
   paste(
      sprintf("address-model=%s", bitness),
      sprintf("--prefix=\"%s\"", prefix),
      "--abbreviate-paths",
      "--without-python",
      "variant=release",
      "link=static",
      "runtime-link=static",
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

# enter the build directory
enter(build_dir)

# rename the libraries (remove rstudio prefix)
setwd("boost32/lib")
src <- list.files()
tgt <- gsub("rstudio_", "", src)
file.rename(src, tgt)
setwd("../..")

setwd("boost64/lib")
src <- list.files()
tgt <- gsub("rstudio_", "", src)
file.rename(src, tgt)
setwd("../..")

# zip it all up
section("Creating archive '%s'...", output_name)
if (file.exists(output_name))
   unlink(output_name)

zip(output_name, files = c("boost32", "boost64"), extras = "-q")
if (!file.exists(output_name))
   fatal("Failed to create archive '%s'.", output_name)
progress("Created archive '%s'.", output_name)

# copy the generated file to the boost
file.rename(output_name, output_file)
if (!file.exists(output_file))
   fatal("Failed to move archive to path '%s'.", output_file)
progress("Moved archive to path '%s'.", output_file)

# rejoice
progress("Boost built successfully!")
