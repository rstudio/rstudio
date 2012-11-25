

versionInfo <- function() {
  info <- list()
  info$version <- utils::packageVersion("rstudio")
  info$mode <- .Call(getNativeSymbolInfo("rs_rstudioProgramMode", 
                                         PACKAGE=""))
  info
}

diagnosticsReport <- function() {
  invisible(.Call(getNativeSymbolInfo("rs_sourceDiagnostics", PACKAGE="")))
}

loadHistory <- function(file = ".Rhistory") {
  invisible(.Call(getNativeSymbolInfo("rs_loadHistory", PACKAGE=""), file))
}

saveHistory <- function(file = ".Rhistory") {
  invisible(.Call(getNativeSymbolInfo("rs_saveHistory", PACKAGE=""), file))
}

viewData <- function(x, title)
{
  if (missing(title))
    title <- paste("Data:", deparse(substitute(x))[1])
  as.num.or.char <- function(x) {
    if (is.character(x))
      x
    else if (is.numeric(x)) {
      storage.mode(x) <- "double"
      x
    }
    else as.character(x)
  }
  x0 <- as.data.frame(x)
  x <- lapply(x0, as.num.or.char)
  rn <- row.names(x0)
  if (any(rn != seq_along(rn)))
    x <- c(list(row.names = rn), x)
  if (!is.list(x) || !length(x) || !all(sapply(x, is.atomic)) ||
        !max(sapply(x, length)))
    stop("invalid 'x' argument")
  invisible(.Call(getNativeSymbolInfo("rs_viewData", PACKAGE=""), x, title))
}
