
updated <- Rcpp::compileAttributes()
wd <- .rs.normalizePath(".", winslash = "/")
for (file in updated) {
  file <- substr(file, nchar(wd)+2, nchar(file))
  cat("* Updated ", file, "\n", sep="")
}

