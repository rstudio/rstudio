
OWD <- getwd()

YAML_CPP_VERSION   <- "0.6.3"
YAML_CPP_BRANCH    <- paste("yaml-cpp", YAML_CPP_VERSION, sep = "-")
YAML_CPP_ROOT      <- file.path(getwd(), YAML_CPP_BRANCH)
YAML_CPP_SOURCES   <- file.path(YAML_CPP_ROOT, "sources")
YAML_CPP_BUILD     <- file.path(YAML_CPP_ROOT, "build")
YAML_CPP_INSTALL   <- file.path(YAML_CPP_ROOT, "install")

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

# run cmake
unlink(YAML_CPP_BUILD, recursive = TRUE)
dir.create(YAML_CPP_BUILD, recursive = TRUE, showWarnings = FALSE)
setwd(YAML_CPP_BUILD)

args <- c(
   sprintf("-DCMAKE_INSTALL_PREFIX=%s", shQuote(YAML_CPP_INSTALL)),
   "-DYAML_CPP_BUILD_TESTS=Off",
   shQuote(YAML_CPP_SOURCES)
)

system2("cmake", args)

# build the project
system("cmake --build . --config Debug   --target install")
system("cmake --build . --config Release --target install")

# move the install directory
file.rename(YAML_CPP_INSTALL, file.path(OWD, "..", YAML_CPP_BRANCH))

