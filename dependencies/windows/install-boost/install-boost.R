source("tools.R")
section("The working directory is: '%s'", getwd())

# Put RStudio tools on PATH
PATH$prepend("../tools")

boost_url <- "https://sourceforge.net/projects/boost/files/boost/1.63.0/boost_1_63_0.7z"
output_file <- "boost-1.63-win-rtools33-gcc493.zip"
toolchain_bin   <- normalizePath("C:/Rtools/bin", mustWork = TRUE)
toolchain_32bit <- normalizePath("C:/Rtools/mingw_32/bin", mustWork = TRUE)
toolchain_64bit <- normalizePath("C:/Rtools/mingw_64/bin", mustWork = TRUE)

# validate paths
if (!file.exists(toolchain_32bit))
   fatal("No toolchain at path '%s'", toolchain_32bit)

if (!file.exists(toolchain_64bit))
   fatal("No toolchain at path '%s'", toolchain_64bit)

# ensure that '7z' is on the PATH
path_program("7z.exe")

# boost modules we need to use
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
   
   section("Downloading Boost...")
   download(boost_url, destfile = boost_archive)
   if (!file.exists(boost_archive))
      fatal("Failed to download '%s'", boost_archive)
   
   # extract boost (be patient, this will take a while)
   progress("Extracting archive -- please wait a moment...")
   progress("Feel free to get up and grab a coffee.")
   exec("7z.exe", "x", boost_filename)
}

if (!file.exists(boost_dirname))
   fatal("'%s' doesn't exist (download / extract failed?)", boost_dirname)

PATH$remove(toolchain_32bit)
PATH$remove(toolchain_64bit)

enter(boost_dirname)

# Remove any documentation folders (these cause
# bcp to barf while trying to copy files)
unlink("doc", recursive = TRUE)
docs <- file.path("libs", list.files("libs"), "doc")
invisible(lapply(docs, function(doc) {
   if (file.exists(doc))
      unlink(doc, recursive = TRUE)
}))

# Bootstrap the boost build directory
PATH$prepend(toolchain_32bit)
section("Bootstrapping boost...")
exec("cmd.exe /C call bootstrap.bat gcc")
exec("b2 toolset=gcc tools\\bcp")
file.copy("dist/bin/bcp.exe", "bcp.exe")
dir.create("rstudio")
fmt <- "bcp --namespace=rstudio_boost --namespace-alias %s config build rstudio"
exec(sprintf(fmt, paste(boost_modules, collapse = " ")))
setwd("rstudio")
exec("cmd.exe /C call bootstrap.bat gcc")
PATH$remove(toolchain_32bit)

# Construct common arguments for 32bit, 64bit boost builds
bjam_command <- function(bitness) {
   paste(
      "b2",
      "toolset=gcc",
      sprintf("address-model=%s", bitness),
      sprintf("--prefix=%s", sprintf("../boost%s", bitness)),
      "--with-date_time",
      "--with-filesystem",
      "--with-iostreams",
      "--with-program_options",
      "--with-regex",
      "--with-signals",
      "--with-system",
      "--with-thread",
      "--with-chrono",
      "variant=debug,release",
      "link=static",
      "threading=multi",
      "define=BOOST_USE_WINDOWS_H",
      "install"
   )
}

# Build 32bit Boost
PATH$prepend(toolchain_32bit)
section("Building Boost 32bit...")
exec(bjam_command("32"))
PATH$remove(toolchain_32bit)

# Build 64bit Boost
PATH$prepend(toolchain_64bit)
section("Building Boost 64bit...")
exec(bjam_command("64"))
PATH$remove(toolchain_64bit)

setwd("..")

# Rename the libraries
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

# Zip it all up
section("Creating archive at '%s'...", output_file)
PATH$prepend(toolchain_bin) # for zip.exe
if (file.exists(output_file))
   unlink(output_file)
zip(output_file, files = c("boost32", "boost64"), extras = "-q")
if (file.exists(output_file))
   progress("Archive created at '%s'.", output_file)
file.rename(output_file, file.path("..", output_file))
Sys.sleep(5)
