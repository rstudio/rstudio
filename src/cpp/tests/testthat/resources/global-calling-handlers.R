# Standalone subprocess for test-global-calling-handlers.R. Do not source this
# under testthat: registration requires no local condition handlers on the stack.

definitions <- readRDS(commandArgs(trailingOnly = TRUE)[[1L]])
rsEnv <- new.env(parent = baseenv())
for (name in names(definitions))
{
   assign(paste0(".rs.globalCallingHandlers.", name),
          eval(call("function", pairlist(), definitions[[name]]), rsEnv), rsEnv)
}

# Keep the production bodies and stub only the preference, logging, and
# embedding callbacks. Their lexical environment stays separate from .GlobalEnv,
# as it does for functions in tools:rstudio.
rsEnv$preference <- "none"
rsEnv$preferenceFailure <- FALSE
rsEnv$loggingFailure <- FALSE
rsEnv$diagnostics <- character()
rsEnv$.rs.uiPrefs <- list(consoleHighlightConditions = list(get = eval(quote(function()
{
   if (preferenceFailure)
      stop("broken preference")
   preference
}), rsEnv)))
rsEnv$.rs.logWarningMessage <- eval(quote(function(fmt, ...)
{
   if (loggingFailure)
      stop("broken logger")
   diagnostics <<- c(diagnostics, sprintf(fmt, ...))
}), rsEnv)
for (kind in c("Error", "Warning", "Message"))
   assign(paste0(".rs.globalCallingHandlers.on", kind),
          eval(quote(function(cnd) NULL), rsEnv), rsEnv)
attach(rsEnv, name = "tools:rstudio")
initializer <- body(rsEnv$.rs.globalCallingHandlers.initialize)

checkInitialization <- function(label, expected = NULL, diagnostic = NULL,
                                checkSelection = TRUE)
{
   base::globalCallingHandlers(NULL)
   rsEnv$diagnostics <- character()
   if (checkSelection)
      stopifnot(identical(names(rsEnv$.rs.globalCallingHandlers.handlers()), expected))

   before <- ls(.GlobalEnv, all.names = TRUE)
   eval(initializer, .GlobalEnv)
   stopifnot(identical(before, ls(.GlobalEnv, all.names = TRUE)))
   installed <- base::globalCallingHandlers()
   stopifnot(identical(names(installed), expected))
   for (kind in expected)
   {
      suffix <- switch(kind, error = "Error", warning = "Warning", message = "Message")
      stopifnot(identical(installed[[kind]],
                          get(paste0(".rs.globalCallingHandlers.on", suffix), rsEnv)))
   }

   if (is.null(diagnostic))
      stopifnot(length(rsEnv$diagnostics) == 0L)
   else
   {
      stopifnot(length(rsEnv$diagnostics) == 1L,
                grepl("Failed to initialize global calling handlers:",
                      rsEnv$diagnostics, fixed = TRUE),
                grepl(diagnostic, rsEnv$diagnostics, fixed = TRUE))
   }
   base::globalCallingHandlers(NULL)
   cat("PASS:", label, "\n")
}

for (preference in c("errors", "errors_warnings", "errors_warnings_messages", "none"))
{
   rsEnv$preference <- preference
   expected <- switch(preference,
                      errors = "error",
                      errors_warnings = c("error", "warning"),
                      errors_warnings_messages = c("error", "warning", "message"),
                      none = NULL)
   checkInitialization(preference, expected)
}

invalidPreferences <- list(NULL, "unknown", NA_character_, character(),
                           c("errors", "errors_warnings"), 1L, list())
for (preference in invalidPreferences)
{
   rsEnv$preference <- preference
   checkInitialization(paste("invalid preference", paste(deparse(preference), collapse = " ")))
}

rsEnv$preferenceFailure <- TRUE
checkInitialization("throwing getter", diagnostic = "broken preference", checkSelection = FALSE)
rsEnv$loggingFailure <- TRUE
checkInitialization("throwing logger", checkSelection = FALSE)
rsEnv$loggingFailure <- FALSE
rsEnv$preferenceFailure <- FALSE

preferences <- rsEnv$.rs.uiPrefs
rm(".rs.uiPrefs", envir = rsEnv)
checkInitialization("missing preferences", diagnostic = ".rs.uiPrefs", checkSelection = FALSE)
rsEnv$.rs.uiPrefs <- preferences

rsEnv$preference <- "errors"
errorHandler <- rsEnv$.rs.globalCallingHandlers.onError
rm(".rs.globalCallingHandlers.onError", envir = rsEnv)
checkInitialization("missing handler", diagnostic = ".rs.globalCallingHandlers.onError",
                    checkSelection = FALSE)
rsEnv$.rs.globalCallingHandlers.onError <- errorHandler

# Installation now happens after workspace restore. User functions with these
# names must not intercept either registration or its error-reporting path.
maskedNames <- c("do.call", "globalCallingHandlers", "tryCatch", "conditionMessage", "list")
for (name in maskedNames)
   assign(name, function(...) stop("masked base function called"), .GlobalEnv)
checkInitialization("masked base functions", "error")
rsEnv$preferenceFailure <- TRUE
checkInitialization("masked base functions during failure", diagnostic = "broken preference",
                    checkSelection = FALSE)
rm(list = maskedNames, envir = .GlobalEnv)
rsEnv$preferenceFailure <- FALSE

# Falling back to no RStudio handlers must preserve handlers installed by others.
base::globalCallingHandlers(custom = function(cnd) NULL)
existing <- base::globalCallingHandlers()
rsEnv$preference <- NULL
invisible(eval(initializer, .GlobalEnv))
stopifnot(identical(base::globalCallingHandlers(), existing))
base::globalCallingHandlers(NULL)
cat("PASS: existing handlers preserved\n")
