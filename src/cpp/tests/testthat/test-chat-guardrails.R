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

test_that("unlink() accepts NULL under the guardrails", {

   # e.g. rmarkdown::render() cleans up with unlink(NULL) when it fails early
   expect_equal(.rs.chat.withGuardrails(unlink(NULL)), 0L)

   # the 'expand' argument was added in R 4.0.0
   if (getRversion() >= "4.0.0")
      expect_equal(.rs.chat.withGuardrails(unlink(NULL, expand = FALSE)), 0L)

})

# -- working directory ---------------------------------------------------------

# Create a 'user' directory (with a 'sub' directory inside) and an 'outside'
# directory beside it, none of them within the allowed roots, and make
# 'user' the working directory. Undone when `envir` exits.
localWorkingDirectories <- function(envir = parent.frame())
{
   state <- .rs.chat.guardrailState
   oldWd <- getwd()
   oldUserDir <- state$userWorkingDirectory
   oldAgentDir <- state$agentWorkingDirectory

   root <- tempfile("chat-guardrails-wd-", tmpdir = dirname(tempdir()))
   dirs <- list(
      user    = file.path(root, "user"),
      sub     = file.path(root, "user", "sub"),
      outside = file.path(root, "outside")
   )
   for (dir in dirs)
      dir.create(dir, recursive = TRUE)

   withr::defer({
      setwd(oldWd)
      state$userWorkingDirectory <- oldUserDir
      state$agentWorkingDirectory <- oldAgentDir
      unlink(root, recursive = TRUE)
   }, envir = envir)

   setwd(dirs$user)
   state$agentWorkingDirectory <- NULL
   lapply(dirs, .rs.chat.normalizePath)
}

workingDirectory <- function()
{
   .rs.chat.normalizePath(getwd())
}

# Make `fn` stand in for package code: what marks a function as belonging
# to a package is its environment.
asPackageFunction <- function(fn)
{
   environment(fn) <- asNamespace("testthat")
   fn
}

test_that("agent code cannot move the working directory outside the allowed roots", {

   dirs <- localWorkingDirectories()

   expect_error(.rs.chat.withGuardrails(setwd(dirs$outside)), "Action: setwd()", fixed = TRUE)
   expect_error(.rs.chat.withGuardrails(setwd("..")), "Action: setwd()", fixed = TRUE)
   expect_equal(workingDirectory(), dirs$user)

   # so a relative path cannot reach outside them either
   expect_error(.rs.chat.withGuardrails({
      setwd(dirs$outside)
      writeLines("x", "file.txt")
   }))
   expect_false(file.exists(file.path(dirs$outside, "file.txt")))

})

test_that("agent code can move within the allowed roots and back", {

   dirs <- localWorkingDirectories()

   .rs.chat.withGuardrails({
      old <- setwd("sub")
      setwd(old)
   })
   expect_equal(workingDirectory(), dirs$user)

   # the user's directory stays reachable in later evaluations, although
   # the working directory is the only root that contains it
   .rs.chat.withGuardrails(setwd("sub"))
   expect_equal(workingDirectory(), dirs$sub)
   .rs.chat.withGuardrails(setwd(".."))
   expect_equal(workingDirectory(), dirs$user)

   .rs.chat.withGuardrails(setwd(tempdir()))
   .rs.chat.withGuardrails(setwd(dirs$user))
   expect_equal(workingDirectory(), dirs$user)

})

test_that("a working directory chosen by the user replaces the remembered one", {

   dirs <- localWorkingDirectories()
   .rs.chat.withGuardrails(setwd(tempdir()))

   # the user moves (e.g. from the Console), between agent evaluations
   setwd(dirs$outside)

   .rs.chat.withGuardrails(writeLines("x", "file.txt"))
   expect_true(file.exists(file.path(dirs$outside, "file.txt")))
   expect_error(.rs.chat.withGuardrails(setwd(dirs$user)), "Action: setwd()", fixed = TRUE)

})

test_that("package code can change the working directory", {

   dirs <- localWorkingDirectories()

   inDirectory <- asPackageFunction(function(dir, callback)
   {
      old <- setwd(dir)
      on.exit(setwd(old), add = TRUE)
      callback()
   })

   path <- .rs.chat.withGuardrails(inDirectory(dirs$outside, getwd))
   expect_equal(.rs.chat.normalizePath(path), dirs$outside)
   expect_equal(workingDirectory(), dirs$user)

   # as can a helper the package defines within one of its functions,
   # whose environment is a call frame rather than the namespace
   inDirectory <- asPackageFunction(function(dir, callback)
   {
      change <- function(dir) setwd(dir)
      old <- change(dir)
      on.exit(change(old), add = TRUE)
      callback()
   })

   path <- .rs.chat.withGuardrails(inDirectory(dirs$outside, getwd))
   expect_equal(.rs.chat.normalizePath(path), dirs$outside)
   expect_equal(workingDirectory(), dirs$user)

})

test_that("setwd() within a function passed to package code is still checked", {

   dirs <- localWorkingDirectories()

   # package frames on the stack don't make the caller of setwd() package code
   invoke <- asPackageFunction(function(callback) callback())

   expect_error(
      .rs.chat.withGuardrails(invoke(function() setwd(dirs$outside))),
      "Action: setwd()",
      fixed = TRUE
   )
   expect_equal(workingDirectory(), dirs$user)

})

test_that("agent code can return to a filesystem root the user chose", {

   dirs <- localWorkingDirectories()

   # a filesystem root is never taken to contain other paths, so it can
   # only be matched exactly
   setwd("/")
   root <- workingDirectory()

   .rs.chat.withGuardrails(setwd(tempdir()))
   .rs.chat.withGuardrails(setwd(root))
   expect_equal(workingDirectory(), root)

})

test_that("source(chdir = TRUE) can change the working directory", {

   dirs <- localWorkingDirectories()
   script <- file.path(dirs$outside, "script.R")
   writeLines("path <- getwd()", script)

   envir <- new.env()
   .rs.chat.withGuardrails(source(script, local = envir, chdir = TRUE))
   expect_equal(.rs.chat.normalizePath(envir$path), dirs$outside)
   expect_equal(workingDirectory(), dirs$user)

   envir <- new.env()
   .rs.chat.withGuardrails(sys.source(script, envir = envir, chdir = TRUE))
   expect_equal(.rs.chat.normalizePath(envir$path), dirs$outside)
   expect_equal(workingDirectory(), dirs$user)

   # a reference taken before the guardrails were active is not the
   # hooked function, but calls the hooked setwd() all the same
   original <- base::source
   envir <- new.env()
   .rs.chat.withGuardrails(original(script, local = envir, chdir = TRUE))
   expect_equal(.rs.chat.normalizePath(envir$path), dirs$outside)
   expect_equal(workingDirectory(), dirs$user)

})

test_that("setwd() within a sourced script is still checked", {

   dirs <- localWorkingDirectories()
   script <- file.path(dirs$user, "script.R")
   writeLines(sprintf("setwd(%s)", deparse(dirs$outside)), script)

   expect_error(.rs.chat.withGuardrails(source(script)), "Action: setwd()", fixed = TRUE)
   expect_error(.rs.chat.withGuardrails(source(script, chdir = TRUE)), "Action: setwd()", fixed = TRUE)
   expect_equal(workingDirectory(), dirs$user)

})

test_that("source references can be read from the directory they were parsed in", {

   dirs <- localWorkingDirectories()
   writeLines("x <- 1", file.path(dirs$outside, "script.R"))

   # a srcfile records the working directory it was created in, and
   # reading its lines changes into that directory
   setwd(dirs$outside)
   srcfile <- srcfile("script.R")
   setwd(dirs$user)

   expect_equal(.rs.chat.withGuardrails(getSrcLines(srcfile, 1L, 1L)), "x <- 1")
   expect_equal(workingDirectory(), dirs$user)

})

test_that("isWorkingDirectoryCaller recognizes only the functions that restore the directory", {

   expect_true(.rs.chat.isWorkingDirectoryCaller(base::source))
   expect_true(.rs.chat.isWorkingDirectoryCaller(base::sys.source))
   expect_true(.rs.chat.isWorkingDirectoryCaller(base:::open.srcfile))

   expect_false(.rs.chat.isWorkingDirectoryCaller(base::setwd))
   expect_false(.rs.chat.isWorkingDirectoryCaller(base::eval))
   expect_false(.rs.chat.isWorkingDirectoryCaller(NULL))

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
