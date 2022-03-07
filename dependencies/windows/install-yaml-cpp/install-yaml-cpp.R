
OWD <- getwd()

YAML_CPP_VERSION   <- "0.6.3"
YAML_CPP_BRANCH    <- paste("yaml-cpp", YAML_CPP_VERSION, sep = "-")
YAML_CPP_ROOT      <- file.path(getwd(), YAML_CPP_BRANCH)
YAML_CPP_SOURCES   <- file.path(YAML_CPP_ROOT, "sources")
YAML_CPP_BUILD_32  <- file.path(YAML_CPP_ROOT, "build/x86")
YAML_CPP_BUILD_64  <- file.path(YAML_CPP_ROOT, "build/x64")
YAML_CPP_INSTALL   <- file.path(YAML_CPP_ROOT, YAML_CPP_BRANCH)

if (file.exists(YAML_CPP_ROOT))
   shell(paste("rmdir /S /Q", shQuote(YAML_CPP_ROOT)))

# clone yaml-cpp repository
args <- c(
   "clone",
   "--depth", "1",
   "--branch", YAML_CPP_BRANCH,
   "https://github.com/jbeder/yaml-cpp",
   YAML_CPP_SOURCES
)

unlink(YAML_CPP_SOURCES, recursive = TRUE)
dir.create(dirname(YAML_CPP_SOURCES), recursive = TRUE, showWarnings = FALSE)
system2("git", args)

# make 32-bit build
unlink(YAML_CPP_BUILD_32, recursive = TRUE)
dir.create(YAML_CPP_BUILD_32, recursive = TRUE, showWarnings = FALSE)
setwd(YAML_CPP_BUILD_32)

args <- c(
   "-G", shQuote("Visual Studio 16 2019"),
   "-A", "Win32",
   "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreadedDebugDLL",
   "-DYAML_CPP_BUILD_TESTS=Off",
   "-DYAML_CPP_BUILD_TOOLS=Off",
   shQuote(YAML_CPP_SOURCES)
)

system2("cmake", args)

# build the project
system("cmake --build . --config Debug")
system("cmake --build . --config Release")

# make 64-bit build
unlink(YAML_CPP_BUILD_64, recursive = TRUE)
dir.create(YAML_CPP_BUILD_64, recursive = TRUE, showWarnings = FALSE)
setwd(YAML_CPP_BUILD_64)

args <- c(
   "-G", shQuote("Visual Studio 16 2019"),
   "-A", "x64",
   "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreadedDLL",
   "-DYAML_CPP_BUILD_TESTS=Off",
   "-DYAML_CPP_BUILD_TOOLS=Off",
   shQuote(YAML_CPP_SOURCES)
)

system2("cmake", args)

# build the project
system("cmake --build . --config Debug")
system("cmake --build . --config Release")

# copy to final destinations for use in build
unlink(YAML_CPP_INSTALL, recursive = TRUE)
dir.create(YAML_CPP_INSTALL, recursive = TRUE, showWarnings = FALSE)
setwd(OWD)

copy <- function(source, target) {
   
   tinfo <- file.info(target, extra_cols = FALSE)
   if (tinfo$isdir %in% TRUE)
      system(paste("rmdir /s /q", shQuote(target)))
   else if (tinfo$isdir %in% FALSE)
      system(paste("del", shQuote(target)))
      
   dir.create(dirname(target), recursive = TRUE, showWarnings = FALSE)
   
   sinfo <- file.info(source, extra_cols = FALSE)
   if (sinfo$isdir %in% TRUE)
      shell(paste("xcopy /E /I /Y", shQuote(source), shQuote(target)))
   else
      shell(paste("copy /Y", shQuote(source), shQuote(target)))
   
}

copy(
   file.path(YAML_CPP_SOURCES, "include"),
   file.path(YAML_CPP_INSTALL, "include")
)

# copy the libraries
dir.create(file.path(YAML_CPP_INSTALL, "lib/x86"), recursive = TRUE, showWarnings = FALSE)
dir.create(file.path(YAML_CPP_INSTALL, "lib/x64"), recursive = TRUE, showWarnings = FALSE)

targets <- list(
   
   list(
      source = file.path(YAML_CPP_BUILD_32, "Debug/libyaml-cppmdd.lib"),
      target = file.path(YAML_CPP_INSTALL, "lib/x86/libyaml-cppmdd.lib")
   ),
   
   list(
      source = file.path(YAML_CPP_BUILD_32, "Release/libyaml-cppmd.lib"),
      target = file.path(YAML_CPP_INSTALL, "lib/x86/libyaml-cppmd.lib")
   ),
   
   list(
      source = file.path(YAML_CPP_BUILD_64, "Debug/libyaml-cppmdd.lib"),
      target = file.path(YAML_CPP_INSTALL, "lib/x64/libyaml-cppmdd.lib")
   ),
   
   list(
      source = file.path(YAML_CPP_BUILD_64, "Release/libyaml-cppmd.lib"),
      target = file.path(YAML_CPP_INSTALL, "lib/x64/libyaml-cppmd.lib")
   )
   
)

for (i in seq_along(targets)) {
   src <- targets[[i]]$source
   tgt <- targets[[i]]$target
   file.copy(src, tgt)
}

# zip it up (for AWS)
setwd(dirname(YAML_CPP_INSTALL))
files <- basename(YAML_CPP_INSTALL)
zipfile <- paste(files, "zip", sep = ".")
zip(zipfile, files)

# and copy a version to the root location
copy(YAML_CPP_INSTALL, file.path(OWD, "..", YAML_CPP_BRANCH))
