

systemFile <- function(file) {
  system.file(file, package = "rmarkdown")
}

withWorkingDir <- function(dir, code) {
  old <- setwd(dir)
  on.exit(setwd(old))
  force(code)
} 

