#
# test-chat-guardrails.R
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

context("chat guardrails")

# -- Deny-read patterns -------------------------------------------------------

test_that("deny-read patterns block credential files", {

   pattern <- paste(.rs.chat.denyReadPatterns, collapse = "|")
   matches <- function(path) .rs.chat.pathMatches(pattern, path)

   # AWS
   expect_true(matches("/home/user/.aws/credentials"))
   expect_true(matches("/home/user/.aws/config"))

   # SSH
   expect_true(matches("/home/user/.ssh/config"))
   expect_true(matches("/home/user/.ssh/id_rsa"))
   expect_true(matches("/home/user/.ssh/id_ed25519"))

   # SSH public keys should be allowed
   expect_false(matches("/home/user/.ssh/id_rsa.pub"))
   expect_false(matches("/home/user/.ssh/id_ed25519.pub"))

   # Docker / Kubernetes
   expect_true(matches("/home/user/.docker/config.json"))
   expect_true(matches("/home/user/.kube/config"))

   # Non-credential files in .docker/.kube should be allowed
   expect_false(matches("/home/user/.docker/cli-plugins/docker-compose"))
   expect_false(matches("/home/user/.kube/cache/discovery"))

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

test_that("isPathWithin guards against empty and root directory", {

   expect_equal(.rs.chat.isPathWithin("/any/path", ""), FALSE)
   expect_equal(.rs.chat.isPathWithin("/any/path", "/"), FALSE)

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
   expect_true(.rs.chat.isFileEditAllowed(path))

})

test_that("isFileEditAllowed denies edits in arbitrary directories", {

   expect_false(.rs.chat.isFileEditAllowed(file.path(path.expand("~"), "..", "nowhere", "file.R")))

})

test_that("isFileEditAllowed denies edits in .ssh even within allowed dirs", {

   path <- file.path(getwd(), ".ssh/id_rsa")
   expect_false(.rs.chat.isFileEditAllowed(path))

})

test_that("isFileEditAllowed permits edits in R library paths", {

   for (libPath in .libPaths())
   {
      path <- file.path(libPath, "testpkg/DESCRIPTION")
      expect_true(.rs.chat.isFileEditAllowed(path))
   }

})

test_that("isFileEditAllowed permits edits in R user directories", {

   for (which in c("data", "config", "cache"))
   {
      path <- file.path(tools::R_user_dir("testpkg", which = which), "config.yml")
      expect_true(.rs.chat.isFileEditAllowed(path))
   }

})

# -- isFileReadAllowed ---------------------------------------------------------

test_that("isFileReadAllowed denies reads on credential files", {

   home <- path.expand("~")

   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".aws/credentials")))
   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_rsa")))
   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".env")))
   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".docker/config.json")))
   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".kube/config")))
   expect_false(.rs.chat.isFileReadAllowed(file.path(home, ".git-credentials")))

})

test_that("isFileReadAllowed permits reads on SSH public keys", {

   home <- path.expand("~")
   expect_true(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_rsa.pub")))
   expect_true(.rs.chat.isFileReadAllowed(file.path(home, ".ssh/id_ed25519.pub")))

})
