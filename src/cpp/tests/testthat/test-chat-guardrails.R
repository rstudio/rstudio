#
# test-chat-guardrails.R
#
# Copyright (C) 2026 by Posit Software, PBC
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

context("chat guardrails")

# -- Deny-read patterns -------------------------------------------------------

test_that("deny-read patterns block credential files", {

   pattern <- paste(.rs.chat.denyReadPatterns, collapse = "|")
   matches <- function(path) .rs.chat.pathMatches(pattern, path)

   # AWS
   expect_true(matches("/home/user/.aws/credentials"))
   expect_true(matches("/home/user/.aws/config"))

   # SSH (any non-public file within .ssh, not just the id_* names)
   expect_true(matches("/home/user/.ssh/config"))
   expect_true(matches("/home/user/.ssh/id_rsa"))
   expect_true(matches("/home/user/.ssh/id_ed25519"))
   expect_true(matches("/home/user/.ssh/deploy_key"))
   expect_true(matches("/home/user/.ssh/known_hosts"))

   # SSH public keys should be allowed
   expect_false(matches("/home/user/.ssh/id_rsa.pub"))
   expect_false(matches("/home/user/.ssh/id_ed25519.pub"))

   # Docker / Kubernetes (entire directories denied)
   expect_true(matches("/home/user/.docker/config.json"))
   expect_true(matches("/home/user/.docker/cli-plugins/docker-compose"))
   expect_true(matches("/home/user/.docker"))
   expect_true(matches("/home/user/.kube/config"))
   expect_true(matches("/home/user/.kube/cache/discovery"))
   expect_true(matches("/home/user/.kube"))

   # Cloud provider credentials
   expect_true(matches("/home/user/.config/gcloud/credentials.db"))
   expect_true(matches("/home/user/.config/gcloud"))
   expect_true(matches("/home/user/.azure/accessTokens.json"))
   expect_true(matches("/home/user/.azure"))

   # GPG private keys
   expect_true(matches("/home/user/.gnupg/private-keys-v1.d/key.key"))
   expect_true(matches("/home/user/.gnupg"))

   # Package registry credentials
   expect_true(matches("/home/user/.pypirc"))
   expect_true(matches("/home/user/.gem/credentials"))

   # Database credentials
   expect_true(matches("/home/user/.pgpass"))
   expect_true(matches("/home/user/.my.cnf"))
   expect_true(matches("/home/user/.mylogin.cnf"))

   # Git credential store
   expect_true(matches("/home/user/.git-credentials"))

   # CLI/API tokens
   expect_true(matches("/home/user/.config/gh/hosts.yml"))
   expect_true(matches("/home/user/.config/gh/hosts.yaml"))
   expect_true(matches("/home/user/.huggingface/token"))

   # .env files
   expect_true(matches("/project/.env"))
   expect_true(matches("/project/.env.local"))
   expect_true(matches("/project/.env.production"))

   # .Renviron / .Rprofile
   expect_true(matches("/home/user/.Renviron"))
   expect_true(matches("/project/.Renviron.local"))
   expect_true(matches("/home/user/.Rprofile"))

   # .netrc / .npmrc
   expect_true(matches("/home/user/.netrc"))
   expect_true(matches("/home/user/.npmrc"))

   # Non-sensitive files should not match
   expect_false(matches("/home/user/project/analysis.R"))
   expect_false(matches("/home/user/.config/rstudio/config.json"))
   expect_false(matches("/home/user/documents/notes.txt"))

})

# -- Deny-edit patterns --------------------------------------------------------

test_that("deny-edit patterns block .ssh directory", {

   pattern <- paste(.rs.chat.denyEditPatterns, collapse = "|")
   matches <- function(path) .rs.chat.pathMatches(pattern, path)

   expect_true(matches("/home/user/.ssh/authorized_keys"))
   expect_true(matches("/home/user/.ssh/known_hosts"))
   expect_true(matches("/home/user/.ssh"))

   expect_false(matches("/home/user/project/file.R"))

})

# -- isPathWithin --------------------------------------------------------------

test_that("isPathWithin correctly checks containment", {

   expect_true(.rs.chat.isPathWithin("/home/user/project/file.R", "/home/user/project"))
   expect_true(.rs.chat.isPathWithin("/home/user/project", "/home/user/project"))
   expect_false(.rs.chat.isPathWithin("/home/user/project2/file.R", "/home/user/project"))
   expect_false(.rs.chat.isPathWithin("/home/other/file.R", "/home/user/project"))

})

test_that("isPathWithin guards against empty, root, and drive root directories", {

   expect_equal(.rs.chat.isPathWithin("/any/path", ""), FALSE)
   expect_equal(.rs.chat.isPathWithin("/any/path", "/"), FALSE)
   expect_equal(.rs.chat.isPathWithin("C:/Users/file.R", "C:"), FALSE)
   expect_equal(.rs.chat.isPathWithin("C:/Users/file.R", "C:/"), FALSE)

})

# -- normalizePath -------------------------------------------------------------

test_that("chat.normalizePath handles NA inputs", {

   td <- tempdir()
   result <- .rs.chat.normalizePath(c(file.path(td, "file.R"), NA, file.path(td, "other.R")))
   expect_equal(length(result), 3)
   expect_equal(result[2], "")

})

test_that("chat.normalizePath resolves . and .. components", {

   base <- .rs.chat.normalizePath(tempdir())
   result <- .rs.chat.normalizePath(file.path(tempdir(), "a", ".", "b", "..", "c"))
   expect_equal(result, file.path(base, "a", "c"))

})

# -- isFileEditAllowed ---------------------------------------------------------

test_that("isFileEditAllowed permits edits in temp directory", {

   path <- file.path(tempdir(), "test-file.R")
   expect_equal(.rs.chat.isFileEditAllowed(path), "")

})

test_that("isFileEditAllowed denies edits in arbitrary directories", {

   expect_true(nzchar(.rs.chat.isFileEditAllowed(file.path(path.expand("~"), "..", "nowhere", "file.R"))))

})

test_that("isFileEditAllowed denies edits in .ssh even within allowed dirs", {

   path <- file.path(getwd(), ".ssh/id_rsa")
   expect_true(nzchar(.rs.chat.isFileEditAllowed(path)))

})

test_that("isFileEditAllowed permits edits in R library paths", {

   for (libPath in .libPaths())
   {
      path <- file.path(libPath, "testpkg/DESCRIPTION")
      expect_equal(.rs.chat.isFileEditAllowed(path), "")
   }

})

test_that("isFileEditAllowed permits edits in R user directories", {

   skip_if(getRversion() < "4.0.0", "tools::R_user_dir requires R >= 4.0.0")

   for (which in c("data", "config", "cache"))
   {
      path <- file.path(tools::R_user_dir("testpkg", which = which), "config.yml")
      expect_equal(.rs.chat.isFileEditAllowed(path), "")
   }

})

test_that("isFileEditAllowed permits edits in a not-yet-created user library", {

   oldLibsUser <- Sys.getenv("R_LIBS_USER", unset = NA)
   on.exit({
      if (is.na(oldLibsUser))
         Sys.unsetenv("R_LIBS_USER")
      else
         Sys.setenv(R_LIBS_USER = oldLibsUser)
   }, add = TRUE)

   # point R_LIBS_USER at a directory that does not exist, and is not
   # within any other allowed root
   fakeLib <- file.path(dirname(tempdir()), "chat-guardrails-fake-lib", "4.0")
   Sys.setenv(R_LIBS_USER = fakeLib)

   path <- file.path(fakeLib, "testpkg", "DESCRIPTION")
   expect_equal(.rs.chat.isFileEditAllowed(path), "")

})

# -- allowedRoots / umask ------------------------------------------------------

test_that("isPathWithinAllowedRoots identifies allowed and disallowed paths", {

   allowedPaths <- .rs.chat.normalizePath(c(
      file.path(tempdir(), "file.R"),
      file.path(getwd(), "file.R"),
      file.path(.libPaths()[[1L]], "pkg", "DESCRIPTION")
   ))
   expect_true(all(.rs.chat.isPathWithinAllowedRoots(allowedPaths)))

   deniedPath <- .rs.chat.normalizePath("/no/such/allowed/root/file.R")
   expect_false(any(.rs.chat.isPathWithinAllowedRoots(deniedPath)))

})

test_that("umaskMasksWorldRead samples the umask once", {

   skip_on_os("windows")

   state <- .rs.chat.guardrailState
   oldValue <- state$umaskMasksWorldRead
   oldMask <- Sys.umask("022")
   on.exit({
      Sys.umask(oldMask)
      state$umaskMasksWorldRead <- oldValue
   }, add = TRUE)

   state$umaskMasksWorldRead <- NULL
   expect_false(.rs.chat.umaskMasksWorldRead())

   # the sample is cached, so later umask changes (e.g. by agent code
   # attempting to switch the permission check off) have no effect
   Sys.umask("077")
   expect_false(.rs.chat.umaskMasksWorldRead())

   state$umaskMasksWorldRead <- NULL
   expect_true(.rs.chat.umaskMasksWorldRead())

   state$umaskMasksWorldRead <- NULL
   Sys.umask("027")
   expect_true(.rs.chat.umaskMasksWorldRead())

})

test_that("allowedRoots tracks working-directory changes", {

   dir <- tempfile("chat-guardrails-wd-", tmpdir = dirname(tempdir()))
   dir.create(dir)
   oldWd <- getwd()
   on.exit({
      setwd(oldWd)
      unlink(dir, recursive = TRUE)
   }, add = TRUE)

   path <- .rs.chat.normalizePath(file.path(dir, "file.R"))
   expect_false(any(.rs.chat.isPathWithinAllowedRoots(path)))

   # the memoized roots refresh when the working directory changes
   setwd(dir)
   expect_true(all(.rs.chat.isPathWithinAllowedRoots(path)))

})

# -- isFileReadAllowed ---------------------------------------------------------

test_that("isFileReadAllowed denies reads on credential files", {

   home <- path.expand("~")

   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".aws/credentials"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_rsa"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/deploy_key"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".env"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".docker/config.json"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".docker/trust/private/root.key"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".kube/config"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".kube/cache/oidc-login/token"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".git-credentials"))))

})

test_that("isFileReadAllowed permits reads on SSH public keys", {

   home <- path.expand("~")
   expect_equal(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_rsa.pub")), "")
   expect_equal(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_ed25519.pub")), "")

})

test_that("isFileReadAllowed allows credential files when trusted", {

   home <- path.expand("~")

   # denied by default (untrusted)
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".aws/credentials"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".Renviron"))))
   expect_true(nzchar(.rs.chat.isFileReadAllowed(file.path(home, ".Rprofile"))))

   # allowed when trusted
   expect_equal(.rs.chat.isFileReadAllowed(file.path(home, ".aws/credentials"), trusted = TRUE), "")
   expect_equal(.rs.chat.isFileReadAllowed(file.path(home, ".Renviron"), trusted = TRUE), "")
   expect_equal(.rs.chat.isFileReadAllowed(file.path(home, ".Rprofile"), trusted = TRUE), "")

})

test_that("isFileReadAllowed permits non-world-readable files within allowed roots", {

   skip_on_os("windows")

   state <- .rs.chat.guardrailState
   oldValue <- state$umaskMasksWorldRead
   oldMask <- Sys.umask("022")
   on.exit({
      Sys.umask(oldMask)
      state$umaskMasksWorldRead <- oldValue
   }, add = TRUE)
   state$umaskMasksWorldRead <- NULL

   path <- tempfile("guardrails-private-")
   writeLines("private", path)
   on.exit(unlink(path), add = TRUE)
   Sys.chmod(path, "600")

   expect_equal(.rs.chat.isFileReadAllowed(path), "")

})

test_that("home directory is not read-exempt even as the working directory", {

   skip_on_os("windows")

   state <- .rs.chat.guardrailState
   oldValue <- state$umaskMasksWorldRead
   oldMask <- Sys.umask("022")
   oldWd <- getwd()
   on.exit({
      Sys.umask(oldMask)
      setwd(oldWd)
      state$umaskMasksWorldRead <- oldValue
   }, add = TRUE)
   state$umaskMasksWorldRead <- NULL

   # with no project open, the session working directory defaults to the
   # home directory -- that must not exempt private files under $HOME
   # from the permission check
   home <- path.expand("~")
   setwd(home)

   path <- tempfile("chat-guardrails-private-", tmpdir = home)
   writeLines("private", path)
   on.exit(unlink(path), add = TRUE)
   Sys.chmod(path, "600")

   expect_true(nzchar(.rs.chat.isFileReadAllowed(path)))

   # edits within the working directory remain allowed
   expect_equal(.rs.chat.isFileEditAllowed(path), "")

})

test_that("isFileReadAllowed denies non-world-readable files outside allowed roots", {

   skip_on_os("windows")

   state <- .rs.chat.guardrailState
   oldValue <- state$umaskMasksWorldRead
   oldMask <- Sys.umask("022")
   on.exit({
      Sys.umask(oldMask)
      state$umaskMasksWorldRead <- oldValue
   }, add = TRUE)
   state$umaskMasksWorldRead <- NULL

   # place the file in a sibling of the R temporary directory, outside
   # all of the allowed roots
   dir <- tempfile("chat-guardrails-", tmpdir = dirname(tempdir()))
   dir.create(dir)
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   path <- file.path(dir, "private.txt")
   writeLines("private", path)
   Sys.chmod(path, "600")

   # denied while the umask would grant world-read to new files
   expect_true(nzchar(.rs.chat.isFileReadAllowed(path)))

   # but allowed when the umask strips world-read from new files, as then
   # the missing bit carries no signal
   Sys.umask("077")
   state$umaskMasksWorldRead <- NULL
   expect_equal(.rs.chat.isFileReadAllowed(path), "")

})

test_that("deny-read patterns still apply within allowed roots", {

   path <- file.path(tempdir(), ".env")
   expect_true(nzchar(.rs.chat.isFileReadAllowed(path)))

})

test_that("installed.packages() survives guardrails under a restrictive umask", {

   skip_on_os("windows")

   state <- .rs.chat.guardrailState
   oldValue <- state$umaskMasksWorldRead
   oldMask <- Sys.umask("077")
   on.exit({
      Sys.umask(oldMask)
      state$umaskMasksWorldRead <- oldValue
   }, add = TRUE)
   state$umaskMasksWorldRead <- NULL

   # remove cached metadata so the first call writes a fresh cache that is
   # not world-readable; the second call then reads it back (this is how
   # install.packages() failed in rstudio-pro#11975)
   unlink(list.files(tempdir(), pattern = "^libloc_", full.names = TRUE))

   expect_no_error(.rs.chat.withGuardrails({
      utils::installed.packages(lib.loc = .Library)
      utils::installed.packages(lib.loc = .Library)
   }))

})

test_that("restoreBindings restores the bindings with interrupts suspended", {

   # An interrupt partway through would leave bindings hooked (#19718)
   .rs.chat.injectBindings()
   on.exit(.rs.chat.restoreBindings(), add = TRUE)

   # tools:rstudio can't be mocked with local_mocked_bindings()
   rsEnv <- as.environment("tools:rstudio")
   replaceBinding <- get(".rs.replaceBinding", envir = rsEnv)
   on.exit(
      assign(".rs.replaceBinding", replaceBinding, envir = rsEnv),
      add = TRUE,
      after = FALSE
   )

   suspended <- logical()
   assign(".rs.replaceBinding", function(...) {
      suspended <<- c(suspended, .Internal(interruptsSuspended()))
      replaceBinding(...)
   }, envir = rsEnv)

   .rs.chat.restoreBindings()

   expect_gt(length(suspended), 0L)
   expect_true(all(suspended))
   expect_false(.rs.chat.bindingsInjected)

})

test_that("restoreBindings leaves alone a namespace that was unloaded or loaded again", {

   # e.g. code that ran unloadNamespace("fs") while fs was hooked; loading
   # fs again to restore its bindings fails if it can't be loaded, and a
   # namespace loaded since has bindings that were never hooked (#19806)
   package <- "splines"
   skip_if(isNamespaceLoaded(package), "splines is already loaded")

   hooked <- .rs.chat.hookedBindings
   namespaces <- .rs.chat.hookedNamespaces
   on.exit(.rs.chat.restoreBindings(), add = TRUE)
   on.exit({
      if (exists(package, envir = hooked, inherits = FALSE))
         rm(list = package, envir = hooked)
      if (exists(package, envir = namespaces, inherits = FALSE))
         rm(list = package, envir = namespaces)
      if (isNamespaceLoaded(package))
         unloadNamespace(package)
   }, add = TRUE, after = FALSE)

   # stands in for an original saved from the namespace by injectBindings()
   original <- function(...) NULL

   # unloaded
   loadNamespace(package)
   hooked[[package]] <- list2env(list(bs = original), parent = emptyenv())
   namespaces[[package]] <- asNamespace(package)
   unloadNamespace(package)

   .rs.chat.injectBindings()
   expect_no_warning(.rs.chat.restoreBindings())
   expect_false(isNamespaceLoaded(package))
   expect_false(exists(package, envir = hooked, inherits = FALSE))
   expect_false(.rs.chat.bindingsInjected)

   # unloaded, and loaded again
   loadNamespace(package)
   hooked[[package]] <- list2env(list(bs = original), parent = emptyenv())
   namespaces[[package]] <- asNamespace(package)
   unloadNamespace(package)
   loadNamespace(package)
   bs <- get("bs", envir = asNamespace(package))

   .rs.chat.injectBindings()
   expect_no_warning(.rs.chat.restoreBindings())
   expect_identical(get("bs", envir = asNamespace(package)), bs)
   expect_false(exists(package, envir = hooked, inherits = FALSE))
   expect_false(.rs.chat.bindingsInjected)

})

test_that("originals saved from a namespace that was replaced aren't hooked from", {

   # e.g. one loaded again since: its bindings were never hooked (#19806)
   path <- file.path(tempfile(), ".ssh", "file.txt")
   before <- utils::download.file
   on.exit({
      if (!identical(utils::download.file, before))
         .rs.replaceBinding("download.file", "utils", before)
   }, add = TRUE)

   stale <- function(...) NULL
   .rs.chat.hookedBindings[["utils"]] <- list2env(
      list(download.file = stale),
      parent = emptyenv()
   )
   .rs.chat.hookedNamespaces[["utils"]] <- new.env()

   # hooked from the current binding instead
   expect_error(
      .rs.chat.withGuardrails(utils::download.file("http://127.0.0.1:9/", path)),
      "Action: download.file()",
      fixed = TRUE
   )
   expect_identical(utils::download.file, before)
   expect_identical(get("download.file", envir = as.environment("package:utils")), before)

})

test_that("an original defined outside its namespace is still restored", {

   # e.g. one mocked in the namespace before the guardrails were activated
   before <- utils::download.file
   attached <- as.environment("package:utils")
   on.exit({
      .rs.replaceBindingImpl(asNamespace("utils"), "download.file", before)
      .rs.replaceBindingImpl(attached, "download.file", before)
   }, add = TRUE)

   replacement <- function(url, destfile, ...) "replaced"
   .rs.replaceBindingImpl(asNamespace("utils"), "download.file", replacement)
   .rs.replaceBindingImpl(attached, "download.file", replacement)

   .rs.chat.withGuardrails(NULL)
   expect_identical(body(utils::download.file), body(replacement))
   expect_identical(body(get("download.file", envir = attached)), body(replacement))

})

test_that("a binding that fails to restore doesn't turn off later checks", {

   # refused even within the allowed locations, and in a directory that
   # doesn't exist, so nothing is written even if the check is skipped
   path <- file.path(tempfile(), ".ssh", "file.txt")
   attached <- as.environment("package:utils")
   before <- list(
      writeChar = base::writeChar,
      download.file = utils::download.file,
      write.table = utils::write.table
   )
   on.exit(.rs.chat.restoreBindings(), add = TRUE)

   # tools:rstudio can't be mocked with local_mocked_bindings()
   rsEnv <- as.environment("tools:rstudio")
   replaceBinding <- get(".rs.replaceBinding", envir = rsEnv)
   on.exit(
      assign(".rs.replaceBinding", replaceBinding, envir = rsEnv),
      add = TRUE,
      after = FALSE
   )

   # write.table() is restored in the utils namespace, and then fails before
   # it's restored on the search path; a binding in failFully fails before
   # either is
   failFully <- character()
   failingReplaceBinding <- function(binding, package, override) {
      if (binding %in% failFully)
         stop("simulated failure")
      if (!identical(binding, "write.table"))
         return(replaceBinding(binding, package, override))
      .rs.replaceBindingImpl(asNamespace(package), binding, override)
      stop("simulated failure")
   }

   .rs.chat.injectBindings()
   assign(".rs.replaceBinding", failingReplaceBinding, envir = rsEnv)
   expect_warning(
      .rs.chat.restoreBindings(),
      "failed to restore binding 'write.table'"
   )
   assign(".rs.replaceBinding", replaceBinding, envir = rsEnv)

   # the next activation hooks write.table() in the namespace too, from the
   # original saved for it (each activation's own restore repairs the
   # partial one, so this must come first). Unhooked, it would be refused
   # anyway, by the file() it calls.
   expect_error(
      .rs.chat.withGuardrails(utils::write.table(1, path)),
      "Action: write.table()",
      fixed = TRUE
   )

   # and checks again (#19806)
   expect_error(
      .rs.chat.withGuardrails(writeLines("x", path)),
      "agent file operations were blocked"
   )
   expect_identical(utils::write.table, before$write.table)
   expect_identical(get("write.table", envir = attached), before$write.table)

   # likewise when options(warn = 2) makes the restore's warning an error,
   # and with a binding left hooked as well
   failFully <- "writeChar"
   .rs.chat.injectBindings()
   assign(".rs.replaceBinding", failingReplaceBinding, envir = rsEnv)
   error <- tryCatch(
      withr::with_options(list(warn = 2), .rs.chat.restoreBindings()),
      error = identity
   )
   assign(".rs.replaceBinding", replaceBinding, envir = rsEnv)

   expect_s3_class(error, "error")
   expect_match(conditionMessage(error), "failed to restore binding 'writeChar'")
   expect_match(conditionMessage(error), "failed to restore binding 'write.table'")

   # the bindings after the failures were still restored
   expect_false(.rs.chat.bindingsInjected)
   expect_identical(utils::download.file, before$download.file)
   expect_identical(get("download.file", envir = attached), before$download.file)

   # and the next activation hooks writeChar() from its saved original, not
   # from the hooked version
   expect_error(
      .rs.chat.withGuardrails(writeLines("x", path)),
      "agent file operations were blocked"
   )
   expect_identical(base::writeChar, before$writeChar)
   expect_identical(utils::write.table, before$write.table)
   expect_identical(get("write.table", envir = attached), before$write.table)

})

# -- hasTrustedCallerImpl ----------------------------------------------------

test_that("hasTrustedCallerImpl returns TRUE for safe functions", {

   expect_true(.rs.chat.hasTrustedCallerImpl(list(utils::install.packages)))
   expect_true(.rs.chat.hasTrustedCallerImpl(list(utils::download.packages)))
   expect_true(.rs.chat.hasTrustedCallerImpl(list(utils::available.packages)))

})

test_that("hasTrustedCallerImpl rejects non-safe utils functions", {

   expect_false(.rs.chat.hasTrustedCallerImpl(list(utils::str)))
   expect_false(.rs.chat.hasTrustedCallerImpl(list(utils::read.table)))

})

test_that("hasTrustedCallerImpl returns FALSE for empty stack", {

   expect_false(.rs.chat.hasTrustedCallerImpl(list()))

})

test_that("hasTrustedCallerImpl returns FALSE when agent calls base functions directly", {

   # Simulates: agent calls readLines("~/.aws/credentials")
   fns <- list(base::readLines)
   expect_false(.rs.chat.hasTrustedCallerImpl(fns))

})

test_that("hasTrustedCallerImpl returns FALSE for primitive / NULL-env functions", {

   fns <- list(base::`+`, base::`[`)
   expect_false(.rs.chat.hasTrustedCallerImpl(fns))

})

test_that("hasTrustedCallerImpl returns TRUE when package code reads a file", {

   # Simulates: devtools::document() -> ... -> base::readLines()
   skip_if_not_installed("devtools")
   fns <- list(devtools::document, base::readLines)
   expect_true(.rs.chat.hasTrustedCallerImpl(fns))

})
