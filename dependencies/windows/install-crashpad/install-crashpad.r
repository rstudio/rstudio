# some laziness to ensure we move to the 'install-crashpad' folder
if (file.exists("rstudio.Rproj"))
   setwd("dependencies/windows/install-crashpad")

source("../tools.R")
section("The working directory is: '%s'", getwd())
progress("Producing crashpad build")
owd <- getwd()

# initialize log directory (for when things go wrong)
unlink("logs", recursive = TRUE)
dir.create("logs")
options(log.dir = normalizePath("logs"))

# put RStudio tools on PATH
PATH$prepend("../tools")

# initialize variables
crashpad_debug_url <- "https://s3.amazonaws.com/rstudio-buildtools/crashpad-debug.zip"
crashpad_release_url <- "https://s3.amazonaws.com/rstudio-buildtools/crashpad-release.zip"
output_dir <- normalizePath(file.path(owd, ".."), winslash = "\\")
crashpad_debug_dir <- file.path(output_dir, "crashpad-debug")
crashpad_release_dir <- file.path(output_dir, "crashpad-release")
crashpad_debug_zip <- file.path(output_dir, "crashpad-debug.zip")
crashpad_release_zip <- file.path(output_dir, "crashpad-release.zip")

downloadAndUnzip <- function(outputFile, extractDir, outputDir, url) {
   # download zip if we don't already have it
   if (!file.exists(outputFile)) {
      section("Downloading '%s' from '%s'", outputFile, url)
	  download(url, destfile = outputFile)
	  if (!file.exists(outputFile))
	     fatal("Failed to download '%s'", outputFile)
   }
   
   if (!file.exists(outputDir)) {
      # extract zip file
      progress("Extracting zip file '%s'", outputFile)
      unzip(outputFile, exdir = extractDir)
   }
}

downloadAndUnzip(crashpad_debug_zip, output_dir, crashpad_debug_dir, crashpad_debug_url)
downloadAndUnzip(crashpad_release_zip, output_dir, crashpad_release_dir, crashpad_release_url)

progress("crashpad installed successfully!")
