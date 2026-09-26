
.rs.logging.setStderrLogLevel <- function(section, level) {
   .Call("rs_loggingSetStderrLogLevel", as.character(section), as.integer(level), PACKAGE = "(embedding)")
   invisible(level)
}
