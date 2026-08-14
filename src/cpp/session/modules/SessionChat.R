#
# SessionChat.R
#
# Copyright (C) 2025 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# NOTE: The guardrails implemented here are intended to prevent accidental
# misuse and unintentionally destructive AI-generated code. They should
# not be relied upon to defend against deliberately malicious code.

# Base package names, excluded from "trusted caller" detection
# because the agent could call them directly to access files.
.rs.setVar("chat.basePackages", rownames(
   installed.packages(priority = "base", lib.loc = .Library)
))

# Specific functions from base/recommended packages that are allowed to
# access credential files as part of their legitimate operation (e.g.
# reading ~/.netrc for HTTP authentication). These are captured before
# hook injection so that if they were ever added to the hook list in the
# future, the safe list would still hold the original pre-hook closures.
#
# An explicit allowlist is needed because these functions live in packages
# (like 'utils') that are in the basePkgs set, and therefore excluded
# from the usual trusted-caller namespace check.
.rs.setVar("chat.safeFunctions", list(
   utils::install.packages,
   utils::download.packages,
   utils::available.packages
))


# Each hooked namespace gets its own environment within this container,
# keyed by the namespace name. The per-namespace environment maps
# binding names to their original values.
.rs.setVar("chat.hookedBindings", new.env(parent = emptyenv()))
.rs.setVar("chat.bindingsInjected", FALSE)

# Cached state for guardrail checks: the sampled umask, the memoized
# allowed roots, and the normalized home directory. See the individual
# accessors (chat.umaskMasksWorldRead, chat.allowedRoots,
# chat.homeDirectory) for the caching rationale.
.rs.setVar("chat.guardrailState", new.env(parent = emptyenv()))


# PCRE patterns matched against normalized paths to deny reads.
# Paths are always absolute and normalized before matching.
#
# NOTE: These patterns work together with the file permission check in
# isFileReadAllowed (denying files without world-readable permissions).
# That check is a heuristic which only applies outside the allowed roots,
# and only when the umask grants world-read to newly-created files, so
# these patterns serve as the backstop for common credential paths.
.rs.setVar("chat.denyReadPatterns", c(

   # Deny files that are likely to contain credentials
   "/\\.aws/credentials$",
   "/\\.aws/config$",
   "/\\.netrc$",
   "/\\.npmrc$",

   # Deny container/cloud credential directories
   "/\\.docker(/|$)",
   "/\\.kube(/|$)",

   # Deny cloud provider credential directories
   "/\\.config/gcloud(/|$)",
   "/\\.azure(/|$)",

   # Deny GPG private keys
   "/\\.gnupg(/|$)",

   # Deny package registry credential files
   "/\\.pypirc$",
   "/\\.gem/credentials$",

   # Deny database credential files
   "/\\.pgpass$",
   "/\\.my(login)?\\.cnf$",

   # Deny git credential store
   "/\\.git-credentials$",

   # Deny CLI/API token files
   "/\\.config/gh/hosts\\.ya?ml$",
   "/\\.huggingface/token$",

   # Deny files like .env, .env.local, .Renviron, .Rprofile, and so on.
   "/\\.env(\\.|$)",
   "/\\.Renviron(\\.|$)",
   "/\\.Rprofile(\\.|$)",

   # Deny access to non-public files within the .ssh directory (config,
   # private keys under any name, known_hosts); public keys stay readable.
   "/\\.ssh/.*(?<!\\.pub)$"

))


# PCRE patterns matched against normalized paths to deny edits.
# Note that file edits are disallowed by default, except for files within
# the allowed roots enumerated by chat.allowedRoots.
#
# This list serves to deny edits for certain files even if they're within
# one of the 'allowed' roots.
.rs.setVar("chat.denyEditPatterns", c(

   # Deny edits on or within the .ssh directory.
   "/\\.ssh(/|$)"

))

# PCRE patterns matched against normalized paths to deny access to
# sensitive system files.
.rs.setVar("chat.denySystemPatterns", c(
   "/etc/master\\.passwd$",
   "/etc/passwd$",
   "/etc/shadow$",
   "/etc/sudoers$",
   "/proc/self/environ$"
))


#' Add a preflight validation hook to an existing function.
#'
#' Prepends the body of `hook` to the body of the target function,
#' so that the hook runs before the original implementation. The
#' original binding is saved for later restoration by
#' `.rs.chat.restoreBindings`.
#'
#' @param package The name of the package containing the binding.
#' @param binding The name of the function to hook.
#' @param hook A function whose body will be prepended to the original.
.rs.addFunction("chat.addPreflightHook", function(package, binding, hook)
{
   # resolve the namespace environment
   envir <- asNamespace(package)

   # skip bindings that don't exist (e.g. removed in a future R version)
   if (!exists(binding, envir = envir, inherits = FALSE))
   {
      warning(sprintf("binding '%s' not found in '%s'; skipping hook", binding, package))
      return(invisible(NULL))
   }

   # grab the original binding
   original <- envir[[binding]]

   # get or create the per-namespace storage for original bindings
   if (!exists(package, envir = .rs.chat.hookedBindings, inherits = FALSE))
      .rs.chat.hookedBindings[[package]] <- new.env(parent = emptyenv())
   .rs.chat.hookedBindings[[package]][[binding]] <- original

   # set hook environment and formals to match original (replaceBinding
   # will also set the environment, but we need it here so the merged
   # body can resolve symbols from the original namespace)
   environment(hook) <- environment(original)
   formals(hook) <- formals(original)

   # inject the body of our hook as a prefix to the original code
   body(hook) <- call("{", body(hook), body(original))

   # replace in both namespace and search path
   .rs.replaceBinding(binding, package, hook)

   # return old binding in case caller needs it
   invisible(original)
})

.rs.addFunction("chat.addPreflightHooks", function(package, hooks)
{
   if (!isNamespaceLoaded(package))
      return(invisible())

   .rs.enumerate(hooks, function(binding, hook)
   {
      .rs.chat.addPreflightHook(package, binding, hook)
   })
})


#' Match paths against a pattern.
#'
#' Wrapper around grepl that uses case-insensitive matching on
#' platforms with case-insensitive filesystems (macOS, Windows).
#'
#' @param pattern A PCRE regular expression.
#' @param x A character vector of file paths.
#' @param ... Additional arguments passed to `grepl()`.
.rs.addFunction("chat.pathMatches", function(pattern, x, ...)
{
   grepl(pattern, x, ignore.case = !.rs.platform.isLinux, perl = TRUE, ...)
})

#' Check whether a path lies within a directory.
#'
#' Both `path` and `directory` should be normalized before calling
#' this function.
#'
#' @param path A character vector of file paths.
#' @param directory A single directory path.
.rs.addFunction("chat.isPathWithin", function(path, directory)
{
   # guard against empty, root, or bare Windows drive root directory,
   # any of which would match all (or nearly all) paths
   if (!nzchar(directory) || directory == "/" || grepl("^[A-Za-z]:/?$", directory))
      return(rep.int(FALSE, length(path)))

   pattern <- paste0("^\\Q", directory, "\\E(/|$)")
   .rs.chat.pathMatches(pattern, path)
})

#' Normalize file paths for use in guardrail path comparisons.
#'
#' Expands leading '~' via `path.expand()`, then uses Boost filesystem to
#' resolve symlinks, normalize separators to '/', and remove '.' and '..'
#' components.
#'
#' @param path A character vector of file paths.
.rs.addFunction("chat.normalizePath", function(path)
{
   path[is.na(path)] <- ""
   .Call("rs_chatNormalizePath", path.expand(path), PACKAGE = "(embedding)")
})

#' Check whether a list of functions contains a trusted caller.
#'
#' Given a list of functions (typically extracted from the call stack),
#' returns `TRUE` if any function's environment is a non-base, non-recommended
#' package namespace, or if any function matches the `chat.safeFunctions`
#' allowlist. When either condition is met, the call is considered "trusted" --
#' package code legitimately accessing files should not be blocked by the
#' credential-path deny list.
#'
#' Base packages are excluded because they are general-purpose utilities that
#' the agent could call directly to launder file access (e.g.
#' `base::readLines("~/.aws/credentials")`). Specific functions from these
#' excluded packages (e.g. `utils::install.packages`) that legitimately need
#' credential access are granted trust via the `chat.safeFunctions` allowlist.
#'
#' @param fns A list of functions to check.
#' @return `TRUE` if a trusted caller is found (either a non-base package
#'   namespace or an explicitly safe function).
.rs.addFunction("chat.hasTrustedCallerImpl", function(fns)
{
   basePkgs <- .rs.chat.basePackages
   safeFns  <- .rs.chat.safeFunctions

   for (fn in fns)
   {
      # Allow explicitly safe functions from base/recommended packages
      # (e.g. install.packages) that legitimately access credential files.
      # These are checked before the basePkgs guard below because their
      # namespace would otherwise cause them to be skipped.
      for (safeFn in safeFns)
      {
         if (identical(fn, safeFn))
            return(TRUE)
      }

      envir <- environment(fn)
      if (is.null(envir))
         next

      if (!isNamespace(envir))
         next

      pkg <- getNamespaceName(envir)
      if (pkg %in% basePkgs)
         next

      return(TRUE)
   }

   FALSE
})

#' Check whether the current call has a trusted caller.
#'
#' Collects the functions on the call stack and delegates to
#' `.rs.chat.hasTrustedCallerImpl()`.
#'
#' @return `TRUE` if a trusted caller is on the call stack.
.rs.addFunction("chat.hasTrustedCaller", function()
{
   nframe <- sys.nframe() - 1L  # exclude our own frame
   if (nframe < 1L)
      return(FALSE)
   fns <- lapply(seq_len(nframe), sys.function)
   .rs.chat.hasTrustedCallerImpl(fns)
})

#' Check whether reading the given paths is allowed.
#'
#' Reads are allowed by default, but denied for files that lack
#' world-readable permissions or match well-known sensitive path
#' patterns (e.g. `~/.aws/credentials`, `.env`, `.Renviron`).
#'
#' The permission check is a heuristic for spotting sensitive files, so
#' it only applies where a missing world-read bit is actually a signal:
#' outside the allowed roots (see `chat.allowedRoots()`), and only when
#' the umask would grant world-read to newly-created files. Otherwise
#' the check would deny every file the session itself creates in
#' environments with a restrictive umask (e.g. containers with umask
#' 077), including caches that `install.packages()` needs to read back.
#'
#' The home directory itself never counts as an allowed root here, even
#' though it can be one for edits (it is the fallback working directory
#' when no project is open): exempting all of `$HOME` would hide
#' deliberately-private dotfiles (e.g. mode-600 tokens and keys) from
#' the permission check.
#'
#' When `trusted` is `TRUE` (i.e. the call originates from a
#' non-base package), the deny-pattern check is skipped so that
#' package code can legitimately access credential files.
#'
#' @param path A character vector of file paths.
#' @param trusted Whether to skip the deny-pattern check.
#' @return A character vector the same length as `path`, where
#'   empty strings indicate allowed reads and non-empty strings
#'   give the reason the read was denied.
.rs.addFunction("chat.isFileReadAllowed", function(path, trusted = FALSE)
{
   # normalize path for comparison
   path <- .rs.chat.normalizePath(path)

   # assume file reads are permitted by default
   reasons <- rep.int("", length(path))

   # deny reads on files that lack read permission for 'others', skipping
   # files within the allowed roots -- the session itself creates files
   # there that would fail the check under a restrictive umask. skip the
   # check entirely when the umask strips world-read from new files, as
   # then every file is private by default and the missing bit carries no
   # signal (use which() to drop NA modes from non-existent files)
   if (!.rs.chat.umaskMasksWorldRead())
   {
      # exclude the home directory from the exemption: as the fallback
      # working directory it can be an allowed root, but private files
      # directly under $HOME are exactly what this check is for
      roots <- setdiff(.rs.chat.allowedRoots(), .rs.chat.homeDirectory())

      info <- suppressWarnings(file.info(path))
      deny <- bitwAnd(info$mode, 4L) == 0L & !.rs.chat.isPathWithinAllowedRoots(path, roots)
      reasons[which(deny)] <- "File is not world-readable."
   }

   # deny reads matching sensitive path patterns
   # (skip for trusted callers, i.e. non-base package code)
   if (!trusted)
   {
      pattern <- paste(.rs.chat.denyReadPatterns, collapse = "|")
      reasons[.rs.chat.pathMatches(pattern, path)] <- "File may contain secret keys or credentials."
   }

   # deny reads on sensitive system files
   pattern <- paste(.rs.chat.denySystemPatterns, collapse = "|")
   reasons[.rs.chat.pathMatches(pattern, path)] <- "File may contain sensitive system information."

   reasons
})

#' Compute the RStudio user scratch path.
#'
#' Delegates to the C++ `xdg::userDataDir()` so the resolution logic
#' (environment variable overrides, platform defaults, directory
#' creation) stays in one place.
#'
#' @return A single normalized directory path.
.rs.addFunction("chat.userScratchPath", function()
{
   .Call("rs_userDataDir", PACKAGE = "(embedding)")
})

#' Enumerate the roots within which agent file edits are allowed.
#'
#' Edits are allowed within:
#'
#' - The R temporary directory
#' - The current working directory
#' - The RStudio user scratch path (tool-invoked code may update files there)
#' - R library paths (e.g. for package installation)
#' - The user library (R_LIBS_USER), even before it has been created
#' - The active project directory (when a project is open)
#' - R user directories (data, config, cache)
#'
#' Reads within these roots (except the home directory itself; see
#' `chat.isFileReadAllowed`) are also exempt from the world-readable
#' permission check, since the session itself creates files there.
#'
#' The result is memoized on the inputs that can change while the
#' session runs (working directory, library paths, `R_LIBS_USER`):
#' this runs on every hooked file operation, and enumerating the
#' roots is comparatively expensive (the scratch-path lookup alone
#' touches the filesystem).
#'
#' @return A character vector of normalized directory paths.
.rs.addFunction("chat.allowedRoots", function()
{
   userLibs <- strsplit(Sys.getenv("R_LIBS_USER"), .Platform$path.sep, fixed = TRUE)[[1L]]
   key <- list(getwd(), .libPaths(), userLibs)

   state <- .rs.chat.guardrailState
   if (identical(state$allowedRootsKey, key))
      return(state$allowedRoots)

   roots <- c(tempdir(), getwd(), .rs.chat.userScratchPath(), .libPaths())

   # include the user library even when it doesn't exist yet -- a nonexistent
   # user library is excluded from .libPaths(), but install.packages() offers
   # to create it on first use
   roots <- c(roots, userLibs[nzchar(userLibs)])

   projectDir <- .rs.getProjectDirectory()
   if (!is.null(projectDir))
      roots <- c(roots, projectDir)

   # R user directories (data, config, cache),
   # e.g. ~/.local/share/R, ~/.config/R, ~/.cache/R on Linux.
   # Use dirname() to obtain the parent directory from a dummy package name.
   # tools::R_user_dir was introduced in R 4.0.0.
   if (getRversion() >= "4.0.0")
   {
      for (which in c("data", "config", "cache"))
         roots <- c(roots, dirname(tools::R_user_dir("_", which = which)))
   }

   roots <- .rs.chat.normalizePath(roots)
   state$allowedRootsKey <- key
   state$allowedRoots <- roots
   roots
})

#' Check whether paths lie within any of the allowed roots.
#'
#' @param path A character vector of normalized file paths.
#' @param roots The roots to check against.
#' @return A logical vector the same length as `path`.
.rs.addFunction("chat.isPathWithinAllowedRoots", function(path, roots = .rs.chat.allowedRoots())
{
   allowed <- rep.int(FALSE, length(path))
   for (root in roots)
      allowed <- allowed | .rs.chat.isPathWithin(path, root)
   allowed
})

#' The normalized home directory, computed once.
#'
#' @return A single normalized directory path.
.rs.addFunction("chat.homeDirectory", function()
{
   state <- .rs.chat.guardrailState
   if (is.null(state$homeDirectory))
      state$homeDirectory <- .rs.chat.normalizePath(path.expand("~"))
   state$homeDirectory
})

#' Check whether the umask strips world-read from new files.
#'
#' When it does (e.g. hardened containers commonly run with umask 077),
#' a file lacking the world-read bit is just the environment's default
#' rather than a deliberate act of protection, so the missing bit
#' carries no signal about the file's sensitivity.
#'
#' The umask is sampled once and cached: `Sys.umask(NA)` reads the umask
#' by briefly setting it to 0 and restoring it, which is both unsafe to
#' run on every hooked file operation (a background thread creating a
#' file inside that window would get mode 666) and would let agent code
#' switch the permission check off by changing the umask. The sample is
#' taken no later than the first `chat.injectBindings()` call, before
#' any agent code has run.
#'
#' @return `TRUE` if newly-created files would not be world-readable.
.rs.addFunction("chat.umaskMasksWorldRead", function()
{
   state <- .rs.chat.guardrailState
   if (is.null(state$umaskMasksWorldRead))
   {
      umask <- Sys.umask(NA)
      state$umaskMasksWorldRead <- bitwAnd(as.integer(umask), 4L) != 0L
   }
   state$umaskMasksWorldRead
})

#' Check whether editing the given paths is allowed.
#'
#' Edits are denied by default, but allowed within the allowed roots
#' (see `chat.allowedRoots()`). Edits within sensitive directories
#' (e.g. `~/.ssh`) are always denied.
#'
#' @param path A character vector of file paths.
#' @return A character vector the same length as `path`, where
#'   empty strings indicate allowed edits and non-empty strings
#'   give the reason the edit was denied.
.rs.addFunction("chat.isFileEditAllowed", function(path)
{
   # normalize path for comparison
   path <- .rs.chat.normalizePath(path)

   # assume file edits are disallowed by default
   reasons <- rep.int(
      "Path is not within the project, working directory, or other allowed locations.",
      length(path)
   )

   # allow edits within the allowed roots
   reasons[.rs.chat.isPathWithinAllowedRoots(path)] <- ""

   # deny edits matching sensitive path patterns (both read and edit
   # deny lists apply, since edits should be at least as restrictive)
   pattern <- paste(c(.rs.chat.denyReadPatterns, .rs.chat.denyEditPatterns), collapse = "|")
   reasons[.rs.chat.pathMatches(pattern, path)] <- "File may contain secret keys or credentials."

   # deny edits on sensitive system files
   pattern <- paste(.rs.chat.denySystemPatterns, collapse = "|")
   reasons[.rs.chat.pathMatches(pattern, path)] <- "File may contain sensitive system information."

   reasons
})

.rs.addFunction("chat.validateFileEdit", function(action, path)
{
   reasons <- .rs.chat.isFileEditAllowed(path)
   denied <- nzchar(reasons)
   if (!any(denied))
      return(TRUE)

   details <- sprintf("- Action: %s()\n- Path:   %s\n- Reason: %s",
      action, path[denied], reasons[denied])
   msg <- paste(c("One or more agent file operations were blocked.", details), collapse = "\n\n")
   stop(msg, call. = FALSE)
})

.rs.addFunction("chat.validateFileRead", function(action, path)
{
   trusted <- .rs.chat.hasTrustedCaller()
   reasons <- .rs.chat.isFileReadAllowed(path, trusted = trusted)
   denied <- nzchar(reasons)
   if (!any(denied))
      return(TRUE)

   details <- sprintf("- Action: %s()\n- Path:   %s\n- Reason: %s",
      action, path[denied], reasons[denied])
   msg <- paste(c("One or more agent file operations were blocked.", details), collapse = "\n\n")
   stop(msg, call. = FALSE)
})

#' Validate a connection open based on the open mode.
#'
#' When `open` is "" (deferred), the connection could later be used for
#' reading or writing, so it is validated as an edit (which applies both
#' the read and edit deny lists).
#'
#' @param name The name of the connection function (e.g. "file", "gzfile").
#' @param description The file path passed to the connection constructor.
#' @param open The open mode string.
.rs.addFunction("chat.validateConnection", function(name, description, open)
{
   if (!nzchar(description))
      return()

   if (!nzchar(open) || grepl("[wWaA+]", open))
   {
      .rs.chat.validateFileEdit(name, description)
   }
   else
   {
      .rs.chat.validateFileRead(name, description)
   }
})

# inject preflight validation hooks into security-sensitive R
# functions. Each hook is prepended to the original function body via
# .rs.chat.addPreflightHook so that calls are validated before the real
# implementation executes. hooks are removed by .rs.chat.restoreBindings.
.rs.addFunction("chat.injectBindings", function()
{
   # guard against reentrant calls -- if hooks are already injected,
   # skip injection to avoid overwriting saved originals
   if (.rs.chat.bindingsInjected)
      return(invisible())

   # sample the umask before agent code can run, so the cached value
   # cannot be influenced by the code the guardrails police
   .rs.chat.umaskMasksWorldRead()

   baseHooks <- list(

      unlink = function()
      {
         # Block recursive deletes in the user's home directory
         if ("*" %in% x)
         {
            workDir <- .rs.chat.normalizePath(getwd())
            homeDir <- .rs.chat.normalizePath("~")
            if (identical(workDir, homeDir))
            {
               msg <- "denied agent from executing unlink(\"*\") on user home directory"
               stop(msg, call. = FALSE)
            }
         }
         
         # The 'expand' formal was added in R 4.0.0; on older versions,
         # unlink always performed glob expansion.
         doExpand <- getRversion() < "4.0.0" || expand
         paths <- if (doExpand) Sys.glob(x) else x
         .rs.chat.validateFileEdit("unlink", paths)
      },

      file.create = function()
      {
         .rs.chat.validateFileEdit("file.create", c(...))
      },

      file.remove = function()
      {
         .rs.chat.validateFileEdit("file.remove", c(...))
      },

      file.rename = function()
      {
         .rs.chat.validateFileEdit("file.rename", c(from, to))
      },

      file.append = function()
      {
         .rs.chat.validateFileEdit("file.append", file1)
         .rs.chat.validateFileRead("file.append", file2)
      },

      file.copy = function()
      {
         .rs.chat.validateFileRead("file.copy", from)
         .rs.chat.validateFileEdit("file.copy", to)
      },

      file.symlink = function()
      {
         .rs.chat.validateFileRead("file.symlink", from)
         .rs.chat.validateFileEdit("file.symlink", to)
      },

      file.link = function()
      {
         .rs.chat.validateFileRead("file.link", from)
         .rs.chat.validateFileEdit("file.link", to)
      },

      file = function()
      {
         .rs.chat.validateConnection("file", description, open)
      },

      gzfile = function()
      {
         .rs.chat.validateConnection("gzfile", description, open)
      },

      bzfile = function()
      {
         .rs.chat.validateConnection("bzfile", description, open)
      },

      xzfile = function()
      {
         .rs.chat.validateConnection("xzfile", description, open)
      },

      readLines = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readLines", con)
      },
      
      writeLines = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeLines", con)
      },

      cat = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("cat", file)
      },

      readChar = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readChar", con)
      },

      writeChar = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeChar", con)
      },

      readBin = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileRead("readBin", con)
      },

      writeBin = function()
      {
         if (is.character(con) && nzchar(con))
            .rs.chat.validateFileEdit("writeBin", con)
      },

      save = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("save", file)
      },

      load = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("load", file)
      },

      source = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("source", file)
      },

      sys.source = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("sys.source", file)
      },

      readRDS = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileRead("readRDS", file)
      },

      saveRDS = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("saveRDS", file)
      },

      dput = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("dput", file)
      },

      dump = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("dump", file)
      },

      dir.create = function()
      {
         .rs.chat.validateFileEdit("dir.create", path)
      },

      Sys.chmod = function()
      {
         .rs.chat.validateFileEdit("Sys.chmod", paths)
      },

      Sys.setFileTime = function()
      {
         .rs.chat.validateFileEdit("Sys.setFileTime", path)
      },

      sink = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("sink", file)
      }

   )

   .rs.chat.addPreflightHooks("base", baseHooks)

   utilsHooks <- list(

      download.file = function()
      {
         .rs.chat.validateFileEdit("download.file", destfile)
      },

      write.table = function()
      {
         if (is.character(file) && nzchar(file))
            .rs.chat.validateFileEdit("write.table", file)
      }

   )

   .rs.chat.addPreflightHooks("utils", utilsHooks)

   fsHooks <- list(

      file_create = function()
      {
         .rs.chat.validateFileEdit("file_create", path)
      },

      file_delete = function()
      {
         .rs.chat.validateFileEdit("file_delete", path)
      },

      file_copy = function()
      {
         .rs.chat.validateFileRead("file_copy", path)
         .rs.chat.validateFileEdit("file_copy", new_path)
      },

      file_move = function()
      {
         .rs.chat.validateFileEdit("file_move", c(path, new_path))
      },

      file_chmod = function()
      {
         .rs.chat.validateFileEdit("file_chmod", path)
      },

      file_chown = function()
      {
         .rs.chat.validateFileEdit("file_chown", path)
      },

      file_touch = function()
      {
         .rs.chat.validateFileEdit("file_touch", path)
      },

      file_show = function()
      {
         .rs.chat.validateFileRead("file_show", path)
      }

   )

   .rs.chat.addPreflightHooks("fs", fsHooks)

   .rs.setVar("chat.bindingsInjected", TRUE)
   invisible(TRUE)
})

.rs.addFunction("chat.restoreBindings", function()
{
   packages <- ls(envir = .rs.chat.hookedBindings, all.names = TRUE)
   allRestored <- TRUE
   
   for (package in packages)
   {
      originals <- .rs.chat.hookedBindings[[package]]
      bindings <- ls(envir = originals, all.names = TRUE)
      
      for (binding in bindings)
      {
         status <- .rs.tryCatch(.rs.replaceBinding(binding, package, originals[[binding]]))
         if (inherits(status, "error"))
         {
            warning(sprintf(
               "failed to restore binding '%s' in '%s': %s",
               binding, package, conditionMessage(status)
            ))
            allRestored <- FALSE
         }
         else
         {
            rm(list = binding, envir = originals)
         }
      }
      
      # only remove the package entry if all bindings were restored
      if (length(ls(envir = originals, all.names = TRUE)) == 0L)
         rm(list = package, envir = .rs.chat.hookedBindings)
   }
   
   if (allRestored)
      .rs.setVar("chat.bindingsInjected", FALSE)
})


# Run an expression with the chat guardrail bindings active, restoring the
# originals on exit (even if `expr` errors). Errors propagate to the caller;
# this is the building block for safeEval (which swallows them) and for
# Playwright tests that want to drive each path deterministically without
# installing a per-suite test helper that races with R startup.
.rs.addFunction("chat.withGuardrails", function(expr, envir = parent.frame())
{
   on.exit({
      tryCatch(
         .rs.chat.restoreBindings(),
         error = function(e) {
            warning("failed to restore bindings: ", conditionMessage(e))
         }
      )
   }, add = TRUE)
   .rs.chat.injectBindings()
   eval(expr, envir = envir)
})


# Helper function for evaluating code for the 'runCode' tool.
.rs.addFunction("chat.safeEval", function(expr, envir = globalenv())
{
   conditionState <- new.env(parent = emptyenv())
   conditionState$conditions <- list()

   tryCatch(
      error = function(e) {
         attr(e, "assistant_conditions") <- conditionState$conditions
         e
      },
      interrupt = function(e) {
         attr(e, "assistant_conditions") <- conditionState$conditions
         e
      },
      {

         # Register cleanup first so bindings are restored even if
         # injectBindings() itself errors partway through
         on.exit({
            tryCatch(
               .rs.chat.restoreBindings(),
               error = function(e) {
                  warning("failed to restore bindings: ", conditionMessage(e))
               }
            )
         }, add = TRUE)
         .rs.chat.injectBindings()

         # Evaluate the provided code. Warnings are muffled after recording:
         # left alone, R defers them to the end of the whole batch and prints
         # them with the internal eval() call as context. The caller re-emits
         # them per expression instead (see writeWarningMessages in
         # SessionChat.cpp, fed by .rs.chat.formatWarningMessages below).
         # Recording follows the REPL's warn option: with warn < 0 warnings
         # are suppressed entirely, and with warn >= 2 muffling would defeat
         # the warning-to-error conversion (which happens after calling
         # handlers run) and the warning surfaces as the error itself, so
         # neither is recorded for re-emission. (warn = 1's immediate
         # printing becomes end-of-expression printing here -- a deliberate
         # divergence that keeps output attached to its expression.)
         result <- withCallingHandlers(
            withVisible(eval(expr, envir = envir)),
            warning = function(w) {
               warnLevel <- getOption("warn", 0L)
               if (warnLevel >= 2L)
                  return()
               if (warnLevel >= 0L)
               {
                  conditionState$conditions[[length(conditionState$conditions) + 1L]] <- list(
                     type = "warning",
                     text = conditionMessage(w)
                  )
               }
               invokeRestart("muffleWarning")
            },
            message = function(m) {
               conditionState$conditions[[length(conditionState$conditions) + 1L]] <- list(
                  type = "message",
                  text = conditionMessage(m)
               )
            }
         )
         result$conditions <- conditionState$conditions
         result

      }
   )
})

# Format warnings recorded by chat.safeEval approximately the way the REPL
# prints deferred warnings after an expression completes. Only the message
# text is recorded, so the REPL's "In <call> :" context and its ">50
# warnings" collapsing are not reproduced.
.rs.addFunction("chat.formatWarningMessages", function(conditions)
{
   texts <- character(0)
   for (condition in conditions)
   {
      if (identical(condition$type, "warning"))
         texts <- c(texts, condition$text)
   }

   if (length(texts) == 0L)
      ""
   else if (length(texts) == 1L)
      paste0("Warning message:\n", texts, "\n")
   else
      paste0(
         "Warning messages:\n",
         paste0(seq_along(texts), ": ", texts, "\n", collapse = "")
      )
})

.rs.addFunction("chat.callExpressionBoundaryHook", function(name, expr, value, ok, visible, error = NULL, conditions = list())
{
   if (!nzchar(name))
      return(invisible(NULL))

   hook <- get0(name, envir = globalenv(), inherits = FALSE)
   if (!is.function(hook))
      return(invisible(NULL))

   # Swallow hook errors so a buggy hook can't abort the user's code, but
   # log them -- otherwise a hook bug degrades interleaving with no signal.
   tryCatch(
      hook(
         expr = expr,
         value = value,
         ok = ok,
         visible = visible,
         error = error,
         conditions = conditions
      ),
      error = function(e) {
         .rs.logWarningMessage(paste0(
            "expression boundary hook '", name, "' failed: ", conditionMessage(e)
         ))
         NULL
      }
   )

   invisible(NULL)
})

# Helper function to capture a recorded plot as base64-encoded PNG
.rs.addFunction("chat.capturePlotFromRecorded", function(recordedPlot)
{
   # Get plot dimensions from options or use defaults
   # Default: 7x7 inches at 96 DPI (R's standard default)
   width <- getOption("repr.plot.width", 7)
   height <- getOption("repr.plot.height", 7)
   dpi <- 96

   # Calculate pixel dimensions
   widthPx <- as.integer(width * dpi)
   heightPx <- as.integer(height * dpi)

   # Create temporary file for PNG output
   tmpFile <- tempfile(fileext = ".png")
   on.exit(unlink(tmpFile), add = TRUE)

   # Open PNG device and replay plot
   tryCatch({
      png(tmpFile, width = widthPx, height = heightPx, res = dpi)
      replayPlot(recordedPlot)
      dev.off()

      # Base64 encode the PNG file using built-in C++ implementation
      encoded <- .rs.base64encodeFile(tmpFile)

      list(
         data = encoded,
         mimeType = "image/png",
         width = widthPx,
         height = heightPx
      )
   }, error = function(e) {
      # Ensure device is closed on error
      if (dev.cur() > 1) dev.off()
      warning(paste("Failed to capture plot:", conditionMessage(e)))
      NULL
   })
})

# Get the current recorded plot (used to detect if plotting occurred)
.rs.addFunction("chat.getRecordedPlot", function()
{
   if (dev.cur() <= 1)
      return(NULL)

   tryCatch(recordPlot(), error = function(e) NULL)
})

# Capture the current plot, but only if plotting occurred since the given
# recorded plot snapshot (to avoid returning stale plots from previous executions).
#
# NOTE: This only captures the final plot state. If code creates multiple plots
# (e.g., plot(1); plot(2)), only the last one is captured. This is a known
# limitation compared to the previous evaluate-based approach.
#
# Returns NULL if no NEW plot is available, otherwise returns a list with:
#   - data: base64-encoded PNG
#   - mimeType: "image/png"
#   - width: pixel width
#   - height: pixel height
.rs.addFunction("chat.captureCurrentPlot", function(plotBefore = NULL)
{
   # No graphics device open
   if (dev.cur() <= 1)
      return(NULL)

   # Try to record the current plot
   recordedPlot <- tryCatch({
      recorded <- recordPlot()
      # Check if the display list is non-empty
      if (is.null(recorded) || length(recorded[[1]]) == 0)
         return(NULL)
      recorded
   }, error = function(e) {
      NULL
   })

   if (is.null(recordedPlot))
      return(NULL)

   # Check if the plot changed since before execution
   # If the plots are identical, this is a stale plot from a previous execution
   if (!is.null(plotBefore)) {
      # Compare the plot objects to see if plotting actually occurred
      if (identical(recordedPlot, plotBefore))
         return(NULL)
   }

   # Use the helper to capture and encode the plot
   plotData <- .rs.chat.capturePlotFromRecorded(recordedPlot)

   if (!is.null(plotData)) {
      # Also replay the plot to the current RStudio device so it appears in the plots pane
      tryCatch({
         replayPlot(recordedPlot)
      }, error = function(e) {
         # Ignore errors in replay - the plot was already captured successfully
      })
   }

   plotData
})

#' Build the R command run in a --vanilla child process to fetch the manifest.
#'
#' The parent session has already resolved download.file.* options (profile +
#' runtime), so capture them here and inject them so the vanilla child reproduces
#' the parent's proxy/method configuration. options(timeout = 30L) is the
#' documented transfer-timeout knob; download.file() has no timeout argument.
#'
#' @param url The manifest URL to download.
.rs.addFunction("chat.manifestDownloadCommand", function(url)
{
   serialize <- function(value) paste(deparse(value), collapse = " ")

   args <- c(serialize(url), "destfile = tmp", "quiet = TRUE")

   method <- getOption("download.file.method")
   if (!is.null(method))
      args <- c(args, sprintf("method = %s", serialize(method)))

   extra <- getOption("download.file.extra")
   if (!is.null(extra))
      args <- c(args, sprintf("extra = %s", serialize(extra)))

   # Propagate download failures as a non-zero process exit. Some methods return
   # a non-zero status with only a warning (rather than erroring), so capture the
   # status and stop() before reading the file -- the manifest subprocess
   # contract is that a download failure surfaces as a non-zero exit status, not
   # a clean exit with a partial/empty body on stdout.
   # cat() with the default sep = " " would replace newlines with spaces; use
   # sep = "\n" so the downloaded body round-trips verbatim on stdout rather than
   # relying on JSON tolerating that substitution.
   sprintf(
      "{ options(timeout = 30L); tmp <- tempfile(); status <- download.file(%s); if (!isTRUE(status == 0)) stop(paste('download.file failed, status', status)); cat(readLines(tmp, warn = FALSE), sep = '\\n') }",
      paste(args, collapse = ", ")
   )
})
