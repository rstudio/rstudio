source("tools.R")
section("The working directory is: '%s'", getwd())

# Put RStudio tools on PATH
PATH$prepend("../tools")

# NOTE: For reasons unknown to me Boost 1.50.0 did not build when using the '.tar.gz'
# archive. Even though the '.zip' is the largest, we use it because it works.
boost_url <- "https://sourceforge.net/projects/boost/files/boost/1.50.0/boost_1_50_0.zip"
output_file <- "boost-1.50-win-rtools33-gcc493.zip"
toolchain_bin   <- normalizePath("C:/Rtools/bin", mustWork = TRUE)
toolchain_32bit <- normalizePath("C:/Rtools/mingw_32/bin", mustWork = TRUE)
toolchain_64bit <- normalizePath("C:/Rtools/mingw_64/bin", mustWork = TRUE)

# validate paths
if (!file.exists(toolchain_32bit))
   fatal("No toolchain at path '%s'", toolchain_32bit)

if (!file.exists(toolchain_64bit))
   fatal("No toolchain at path '%s'", toolchain_64bit)

# construct paths of interest
boost_filename <- basename(boost_url)
boost_dirname <- tools::file_path_sans_ext(boost_filename)
boost_zippath <- boost_filename

if (!file.exists(boost_dirname)) {
   
   section("Downloading Boost...")
   
   download(boost_url, destfile = boost_zippath)
   if (!file.exists(boost_zippath))
      fatal("Failed to download '%s'", boost_zippath)
   
   # extract boost (be patient, this will take a while)
   progress("Extracting archive -- please wait a moment...")
   exec("unzip", boost_filename)
}

if (!file.exists(boost_dirname))
   fatal("'%s' doesn't exist (download / extract failed?)", boost_dirname)

PATH$remove(toolchain_32bit)
PATH$remove(toolchain_64bit)

enter(boost_dirname)

# Bootstrap the boost build directory
PATH$prepend(toolchain_32bit)
section("Bootstrapping boost...")
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

# Zip it all up
section("Creating archive at '%s'...", output_file)
PATH$prepend(toolchain_bin) # for zip.exe
if (file.exists(output_file))
   unlink(output_file)
zip(output_file, files = c("boost32", "boost64"), extras = "-q")
if (file.exists(output_file))
   progress("Archive created at '%s'.", output_file)

Sys.sleep(5)
