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
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
boost_url <- "https://s3.amazonaws.com/rstudio-buildtools/boost_1_65_1.7z"
output_name <- sprintf("boost-1.65.1-win-msvc141-%s-%s.zip", variant, link)
output_dir <- normalizePath(file.path(owd, ".."), winslash = "/")
output_file <- file.path(output_dir, output_name)
install_dir <- file.path(owd, "..", tools::file_path_sans_ext(output_name))

# clear out the directory we'll create boost in
unlink(install_dir, recursive = TRUE)
ensure_dir(install_dir)
install_dir <- normalizePath(install_dir)

# boost modules we need to alias
boost_modules <- c(
   "algorithm", "asio", "array", "bind", "chrono", "circular_buffer",
   "context", "crc", "date_time", "filesystem", "foreach", "format",
   "function", "interprocess", "iostreams", "lambda", "lexical_cast",
   "optional", "program_options", "property_tree", "random", "range",
   "ref", "regex", "scope_exit", "signals", "smart_ptr", "spirit",
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
   
   
   # Building RStudio using boost 1.65.1 and recent VS releases
   # will output a warning "Unknown compiler version..." for each source file,
   # Newer boost versions have this warning commented out, so let's do the 
   # same until we upgrade to a newer boost release.
   replace_one_line(file.path(boost_dirname, "boost", "config", "compiler", "visualc.hpp"),
      '#     pragma message("Unknown compiler version - please run the configure tests and report the results")',
      '#     // pragma message("Unknown compiler version - please run the configure tests and report the results")')
   
   # Building RStudio using boost 1.65.1 and recent VS releases will
   # output many warnings of the form "warning STL4019: The member std::fpos::seekpos() is non-Standard...".
   # This was fixed more recently in Boost: https://github.com/boostorg/iostreams/pull/57/files
   # Apply that same fix to our build of Boost.
   replace_one_line(file.path(boost_dirname, "boost", "iostreams", "detail", "config", "fpos.hpp"),
                    '     !defined(_STLPORT_VERSION) && !defined(__QNX__) && !defined(_VX_CPU)',
                    '     !defined(_STLPORT_VERSION) && !defined(__QNX__) && !defined(_VX_CPU) && !(defined(BOOST_MSVC) && _MSVC_STL_VERSION >= 141)')
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
exec("cmd.exe", "/C call bootstrap.bat vc141")

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
exec("cmd.exe", "/C call bootstrap.bat vc141")

# construct common arguments for 32bit, 64bit boost builds
b2_build_args <- function(bitness) {
   
   prefix <- file.path(install_dir, sprintf("boost%s", bitness), fsep = "\\")
   unlink(prefix, recursive = TRUE)
   
   paste(
      sprintf("address-model=%s", bitness),
      "toolset=msvc-14.1",
      sprintf("--prefix=\"%s\"", prefix),
      "--abbreviate-paths",
      "--without-python",
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
