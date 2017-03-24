# some laziness to ensure we move to the 'install-boost' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-boost")

source("tools.R")
section("The working directory is: '%s'", getwd())

# initialize log directory (for when things go wrong)
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
boost_url <- "https://sourceforge.net/projects/boost/files/boost/1.63.0/boost_1_63_0.7z"
output_file <- "boost-1.63-win-rtools33-gcc493.zip"
output_dir <- file.path("..", tools::file_path_sans_ext(output_file))
toolchain_bin   <- normalizePath("../Rtools33/bin", mustWork = TRUE)
toolchain_32bit <- normalizePath("../Rtools33/mingw_32/bin", mustWork = TRUE)
toolchain_64bit <- normalizePath("../Rtools33/mingw_64/bin", mustWork = TRUE)

# clear out the directory we'll create boost in
unlink(output_dir, recursive = TRUE)
ensure_dir(output_dir)
output_dir <- normalizePath(output_dir)

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

# clean up the path
PATH$remove(toolchain_32bit)
PATH$remove(toolchain_64bit)

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
PATH$prepend(toolchain_32bit)
section("Bootstrapping boost...")
exec("cmd.exe", "/C call bootstrap.bat gcc --without-libraries=python")

# create bcp executable (so we can create Boost
# using a private namespace)
exec("b2", "toolset=gcc -j 4 tools\\bcp")
invisible(file.copy("dist/bin/bcp.exe", "bcp.exe"))

# use bcp to copy boost into 'rstudio' sub-directory
unlink("rstudio", recursive = TRUE)
dir.create("rstudio")
fmt <- "--namespace=rstudio_boost --namespace-alias %s config build rstudio"
args <- sprintf(fmt, paste(boost_modules, collapse = " "))
exec("bcp", args)

# enter the 'rstudio' directory and re-bootstrap
enter("rstudio")
exec("cmd.exe", "/C call bootstrap.bat gcc --without-libraries=python")
PATH$remove(toolchain_32bit)

# construct common arguments for 32bit, 64bit boost builds
b2_build_args <- function(bitness) {
   
   prefix <- file.path(output_dir, sprintf("boost%s", bitness))
   cxxflags <- "-march=core2 -mtune=generic"
   
   unlink(prefix, recursive = TRUE)
   
   paste(
      "toolset=gcc",
      sprintf("address-model=%s", bitness),
      sprintf("--prefix=%s", prefix),
      "--without-python",
      "variant=release",
      "link=static",
      "runtime-link=static",
      "threading=multi",
      sprintf("cxxflags=\"%s\"", cxxflags),
      "define=BOOST_USE_WINDOWS_H",
      "install"
   )
}

# build 32bit Boost
PATH$prepend(toolchain_32bit)
section("Building Boost 32bit...")
exec("b2", b2_build_args("32"))
PATH$remove(toolchain_32bit)

# build 64bit Boost
PATH$prepend(toolchain_64bit)
section("Building Boost 64bit...")
exec("b2", b2_build_args("64"))
PATH$remove(toolchain_64bit)

# enter the build directory
enter(output_dir)

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
section("Creating archive at '%s'...", output_file)
PATH$prepend(toolchain_bin) # for zip.exe
if (file.exists(output_file))
   unlink(output_file)

zip(output_file, files = c("boost32", "boost64"), extras = "-q")
if (file.exists(output_file))
   progress("Archive created at '%s'.", output_file)
file.rename(output_file, file.path("..", output_file))

# success!
