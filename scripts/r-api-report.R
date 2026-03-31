#!/usr/bin/env Rscript
#
# r-api-report.R
#
# Reports which R C API symbols used by RStudio are not part of R's public API.
#
# The public API is determined by @apifun, @eapifun, @apivar, and @eapivar
# annotations in R-exts.texi. The embedding API (@embfun, @embvar) and
# formerly-API symbols (@forfun) are reported separately.
#
# Fetches the latest R-exts.texi and R headers from
# https://github.com/r-devel/r-svn (trunk branch).
#
# Usage:
#   Rscript scripts/r-api-report.R

# -- Paths ----

this_script <- sub("--file=", "", grep("--file=", commandArgs(FALSE), value = TRUE))
rstudio_root <- normalizePath(file.path(dirname(this_script), ".."))
rstudio_cpp <- file.path(rstudio_root, "src", "cpp")
stopifnot(dir.exists(rstudio_cpp))

outfile <- file.path(rstudio_root, "scripts", "r-api-report.txt")

# -- Step 0: Clone or update r-devel/r-svn ----

r_svn_repo <- "https://github.com/r-devel/r-svn.git"
r_svn_cache <- file.path(Sys.getenv("HOME"), ".cache", "rstudio", "r-svn")

if (dir.exists(file.path(r_svn_cache, ".git"))) {
   message("Updating r-devel/r-svn cache...")
   system2("git", c("-C", r_svn_cache, "fetch", "--depth=1", "origin", "trunk"))
   system2("git", c("-C", r_svn_cache, "checkout", "FETCH_HEAD", "--", "doc/manual", "src/include"))
} else {
   message("Cloning r-devel/r-svn (sparse, shallow)...")
   dir.create(dirname(r_svn_cache), recursive = TRUE, showWarnings = FALSE)
   system2("git", c("clone", "--depth=1", "--filter=blob:none", "--sparse", r_svn_repo, r_svn_cache))
   system2("git", c("-C", r_svn_cache, "sparse-checkout", "set", "doc/manual", "src/include"))
}

texi_path <- file.path(r_svn_cache, "doc", "manual", "R-exts.texi")
r_include <- file.path(r_svn_cache, "src", "include")
stopifnot(file.exists(texi_path))

texi <- readLines(texi_path)

# -- Step 1: Extract API symbols from R-exts.texi ----

extract_texi_symbols <- function(tags) {
   pattern <- paste0("@(", paste(tags, collapse = "|"), ") ([A-Za-z_][A-Za-z0-9_]*)")
   m <- regmatches(texi, regexec(pattern, texi))
   unique(vapply(m[lengths(m) > 0], `[[`, character(1), 3))
}

public_api <- extract_texi_symbols(c("apifun", "eapifun", "apivar", "eapivar"))

# Treat symbols exported by graphics headers as public API
for (gfx_header in c("R_ext/GraphicsEngine.h", "R_ext/GraphicsDevice.h")) {
   path <- file.path(r_include, gfx_header)
   if (file.exists(path)) {
      lines <- readLines(path)
      m <- regmatches(lines, gregexpr("\\b(R_|Rf_|GE)[A-Za-z_][A-Za-z0-9_]*", lines))
      public_api <- unique(c(public_api, unlist(m)))
   }
}

embed_api <- extract_texi_symbols(c("embfun", "embvar"))
embed_extras <- c(
   "Rf_initialize_R", "Rf_mainloop", "R_Suicide", "R_Consolefile", "R_Outputfile",
   "R_CStackStart", "R_running_as_main_program", "R_DirtyImage", "R_HomeDir",
   "R_Quiet", "R_NoEcho", "R_Verbose", "R_SignalHandlers",
   "R_SaveGlobalEnvToFile", "R_RestoreGlobalEnvFromFile"
)
embed_api <- unique(c(embed_api, embed_extras))

former_api <- extract_texi_symbols("forfun")

# -- Step 2: Extract R C API symbols used by RStudio ----

# Scan source files (excluding RInternal.hpp and binary files)
source_files <- list.files(
   rstudio_cpp,
   pattern = "\\.(c|cc|cpp|h|hpp|mm)$",
   recursive = TRUE,
   full.names = TRUE
)
source_files <- source_files[!grepl("RInternal\\.hpp$", source_files)]

all_source <- unlist(lapply(source_files, readLines, warn = FALSE))

extract_matches <- function(pattern) {
   m <- regmatches(all_source, gregexpr(pattern, all_source, perl = TRUE))
   unique(unlist(m))
}

rf_symbols <- extract_matches("\\bRf_[a-zA-Z_][a-zA-Z0-9_]*")
r_symbols <- extract_matches("\\bR_[a-zA-Z][a-zA-Z0-9_]*")

r_macros <- c(
   "ALTREP", "ATTRIB", "BODY", "CAAR", "CADR", "CADDR", "CADDDR", "CAD4R",
   "CAD5R", "CAR", "CDAR", "CDDDR", "CDDR", "CDR", "CHAR", "CLEAR_ATTRIB",
   "CLOENV", "COMPLEX", "COMPLEX_ELT", "DATAPTR", "DATAPTR_OR_NULL",
   "DATAPTR_RO", "DUPLICATE_ATTRIB", "ENCLOS", "FORMALS", "FRAME", "HASHTAB",
   "INTEGER", "INTEGER_ELT", "INTERNAL", "IS_SCALAR", "IS_SIMPLE_SCALAR",
   "LENGTH", "LEVELS", "LOGICAL", "LOGICAL_ELT", "MARK_NOT_MUTABLE", "MISSING",
   "NAMED", "OBJECT", "PRCODE", "PRENV", "PRINTNAME", "PROTECT",
   "PROTECT_WITH_INDEX", "PRVALUE", "RAW", "RAW_ELT", "REAL", "REAL_ELT",
   "REAL_RO", "INTEGER_RO", "LOGICAL_RO", "COMPLEX_RO", "RAW_RO", "SETCAR",
   "SETCDR", "SETCADR", "SETCADDR", "SETCADDDR", "SETCAD4R", "SETLEVELS",
   "SET_ATTRIB", "SET_COMPLEX_ELT", "SET_INTEGER_ELT", "SET_LOGICAL_ELT",
   "SET_MISSING", "SET_NAMED", "SET_RAW_ELT", "SET_REAL_ELT", "SET_S4_OBJECT",
   "SET_STRING_ELT", "SET_TAG", "SET_TYPEOF", "SET_VECTOR_ELT",
   "SHALLOW_DUPLICATE_ATTRIB", "STRING_ELT", "SYMVALUE", "TAG", "TYPEOF",
   "UNPROTECT", "VECTOR_ELT", "XLENGTH"
)
macro_pattern <- paste0("\\b(", paste(r_macros, collapse = "|"), ")\\s*\\(")
macro_matches <- regmatches(all_source, gregexpr(macro_pattern, all_source, perl = TRUE))
macro_matches <- unique(trimws(gsub("\\($", "", unlist(macro_matches))))

all_symbols <- unique(c(rf_symbols, r_symbols, macro_matches))

# Filter R_/Rf_ symbols to only those that appear in R's headers
r_header_files <- list.files(r_include, pattern = "\\.h$", recursive = TRUE, full.names = TRUE)
r_header_content <- paste(unlist(lapply(r_header_files, readLines, warn = FALSE)), collapse = "\n")

all_symbols <- Filter(function(sym) {
   if (grepl("^(R_|Rf_)", sym)) {
      grepl(paste0("\\b", sym, "\\b"), r_header_content, perl = TRUE)
   } else {
      TRUE
   }
}, all_symbols)

# Remove types, header guards, preprocessor macros
type_excludes <- c(
   "R_xlen_t", "R_SIZE_T", "R_ARCH", "R_HOME", "R_ext", "R_INTERFACE_PTRS",
   "R_INTERNALS_H_", "R_NO_REMAP", "R_USE_PROTOTYPES", "R_USE_SIGNALS",
   "SEXP", "R_CFinalizer_t", "R_CallMethodDef", "R_ObjectTable",
   "R_INCLUDE_", "R_LEN_T", "R_INLINE"
)
all_symbols <- setdiff(all_symbols, type_excludes)

# -- Step 3: Classify each symbol ----

classify <- function(sym) {
   short <- sub("^Rf_", "", sym)
   if (sym %in% public_api || short %in% public_api) return("public")
   if (sym %in% embed_api || short %in% embed_api) return("embed")
   if (sym %in% former_api || short %in% former_api) return("former")
   "nonapi"
}

classes <- vapply(all_symbols, classify, character(1))

result <- split(names(classes), classes)
for (cls in c("public", "embed", "former", "nonapi")) {
   if (is.null(result[[cls]])) result[[cls]] <- character(0)
}

# -- Step 4: Count uses for display ----

count_uses <- function(sym) {
   pattern <- paste0("\\b", sym, "\\b")
   matches <- grep(pattern, all_source, perl = TRUE, value = TRUE)
   # exclude comment-only lines
   matches <- matches[!grepl("^\\s*//", matches)]
   matches <- matches[!grepl("^\\s*\\*", matches)]
   length(matches)
}

# -- Step 5: Write report ----

out <- character()
ln <- function(...) out <<- c(out, paste0(...))

ln("R API Compliance Report")
ln("============================================================")
ln("")
ln("Generated: ", format(Sys.Date()))
ln("Source:    ", r_svn_repo, " (trunk)")
ln("Scanned:  src/cpp/ (excluding RInternal.hpp)")
ln("")
ln("Public API symbols used:    ", length(result$public))
ln("Embedding API symbols used: ", length(result$embed))
if (length(result$former) > 0) {
   ln("Former API symbols used:    ", length(result$former))
}
ln("Non-API symbols used:       ", length(result$nonapi))
ln("")

write_symbol_table <- function(symbols, header, subtitle = NULL) {
   if (length(symbols) == 0) return()

   ln(header)
   ln("------------------------------------------------------------")
   if (!is.null(subtitle)) ln(subtitle)
   ln("")
   ln(sprintf("%-35s %s", "Symbol", "Uses"))
   ln(sprintf("%-35s %s", "------", "----"))

   counts <- vapply(sort(symbols), count_uses, integer(1))
   for (sym in names(counts[counts > 0])) {
      ln(sprintf("%-35s %d", sym, counts[[sym]]))
   }
   ln("")
}

write_symbol_table(result$nonapi, "Non-Public API Symbols")
write_symbol_table(
   result$embed,
   "Embedding API Symbols",
   "(Documented in the \"Embedding R\" chapter, separate from public C API)"
)

if (length(result$former) > 0) {
   ln("Former API Symbols")
   ln("------------------------------------------------------------")
   ln("")
   for (sym in sort(result$former)) ln("  ", sym)
   ln("")
}

writeLines(out, outfile)
message("Report written to ", outfile)
