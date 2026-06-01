#
# test-ppm.R
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

context("ppm")

# Helper: produce expected structures pre-marked with .rs.scalar so they
# compare equal to the parser's output (which runs everything through
# .rs.markScalars before returning).
expected <- function(...) .rs.markScalars(list(...))

# Helper: build a cache environment populated with the given keyed entries,
# matching the layout used by .rs.ppm.getVulnerabilityInformationImpl.
cacheEnv <- function(...)
{
   entries <- list(...)
   env <- new.env(parent = emptyenv())
   for (key in names(entries))
      assign(key, entries[[key]], envir = env)
   env
}

test_that("parseVulnerabilityResponse handles an empty body", {
   result <- .rs.ppm.parseVulnerabilityResponse("")
   expect_equal(result, structure(list(), names = character()))
})

test_that("parseVulnerabilityResponse parses a single record into a name==version key", {
   contents <- '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1","summary":"hi"}]}'
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "hi"))
   ))
})

test_that("parseVulnerabilityResponse keeps each (name, version) pair in its own key", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1","summary":"hi"}]}',
      '{"name":"pkgA","version":"1.1.0","vulns":[{"id":"V1","summary":"hi"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "hi")),
      `pkgA==1.1.0` = list(list(id = "V1", summary = "hi"))
   ))
})

test_that("parseVulnerabilityResponse groups records across distinct packages", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}',
      '{"name":"pkgB","version":"0.2.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1")),
      `pkgB==0.2.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse returns NULL and warns when any record is an error", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}',
      '{"error":"not allowed","code":"403"}',
      sep = "\n"
   )
   expect_warning(
      result <- .rs.ppm.parseVulnerabilityResponse(contents),
      "error requesting package vulnerabilities.*not allowed.*403"
   )
   expect_null(result)
})

test_that("parseVulnerabilityResponse skips records with no vulns field", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0"}',
      '{"name":"pkgB","version":"0.1.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgB==0.1.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse skips records with empty vulns arrays", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[]}',
      '{"name":"pkgB","version":"0.1.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgB==0.1.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse skips records missing the name field", {
   contents <- paste(
      '{"version":"1.0.0","vulns":[{"id":"V1"}]}',
      '{"name":"pkgB","version":"0.1.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgB==0.1.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse skips records missing the version field", {
   contents <- paste(
      '{"name":"pkgA","vulns":[{"id":"V1"}]}',
      '{"name":"pkgB","version":"0.1.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgB==0.1.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse tolerates blank lines between records", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}',
      "",
      '{"name":"pkgB","version":"0.2.0","vulns":[{"id":"V2"}]}',
      "",
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1")),
      `pkgB==0.2.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse preserves nested vuln fields", {
   contents <- paste0(
      '{"name":"pkgA","version":"1.0.0","vulns":[',
      '{"id":"V1","summary":"s","details":"d","modified":"m","published":"p",',
      '"ranges":[{"type":"SEMVER","events":[{"introduced":"0","fixed":"2"}]}],',
      '"versions":{"1.0.0":true}}',
      ']}'
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(
         id = "V1",
         summary = "s",
         details = "d",
         modified = "m",
         published = "p",
         ranges = list(list(
            type = "SEMVER",
            events = list(list(introduced = "0", fixed = "2"))
         )),
         versions = list(`1.0.0` = TRUE)
      ))
   ))
})

test_that("parseVulnerabilityResponse skips malformed JSON lines", {
   # .rs.fromJSON returns NULL on parse failure; the parser silently
   # skips those records. Pin the contract so a future change makes a
   # deliberate decision rather than an accidental one.
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}',
      '{this is not json',
      '{"name":"pkgB","version":"0.1.0","vulns":[{"id":"V2"}]}',
      sep = "\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1")),
      `pkgB==0.1.0` = list(list(id = "V2"))
   ))
})

test_that("parseVulnerabilityResponse tolerates CRLF line endings", {
   contents <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}',
      '{"name":"pkgB","version":"0.2.0","vulns":[{"id":"V2"}]}',
      sep = "\r\n"
   )
   result <- .rs.ppm.parseVulnerabilityResponse(contents)
   expect_equal(result, expected(
      `pkgA==1.0.0` = list(list(id = "V1")),
      `pkgB==0.2.0` = list(list(id = "V2"))
   ))
})

test_that("aggregateVulnsByName collects a single (name, version) into the by-name shape", {
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "hi"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, "pkgA==1.0.0")
   expect_equal(result, expected(
      pkgA = list(list(id = "V1", summary = "hi"))
   ))
})

test_that("aggregateVulnsByName groups vulns across multiple installed versions", {
   # PPM returns the same vuln list for every queried version of a given
   # package, but a user with the same package in two libraries will see
   # both versions cached. We concatenate without deduping; the rare
   # duplicate is cosmetic only -- the frontend's per-row check uses each
   # vuln's versions map, not list position.
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "hi")),
      `pkgA==1.1.0` = list(list(id = "V1", summary = "hi"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgA==1.1.0"))
   expect_equal(result, expected(
      pkgA = list(
         list(id = "V1", summary = "hi"),
         list(id = "V1", summary = "hi")
      )
   ))
})

test_that("aggregateVulnsByName groups vulns across distinct packages", {
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "first")),
      `pkgB==2.0.0` = list(list(id = "V2", summary = "second"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgB==2.0.0"))
   expect_equal(result, expected(
      pkgA = list(list(id = "V1", summary = "first")),
      pkgB = list(list(id = "V2", summary = "second"))
   ))
})

test_that("aggregateVulnsByName skips cache entries that aren't currently installed", {
   # A package upgrade leaves the old (name, version) key behind in the
   # cache. Filtering by pkgKeys means the orphan doesn't leak back to the UI.
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "stale")),
      `pkgA==2.0.0` = list()
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, "pkgA==2.0.0")
   expect_equal(result, structure(list(), names = character()))
})

test_that("aggregateVulnsByName skips cached entries with empty vuln lists", {
   # PPM may have nothing to say about a package, in which case the cache
   # holds an empty list as a "queried, no vulns" marker.
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(),
      `pkgB==0.1.0` = list(list(id = "V2"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgB==0.1.0"))
   expect_equal(result, expected(
      pkgB = list(list(id = "V2"))
   ))
})

# Tests for the asynchronous request-plan / response-recording flow. The
# network request itself happens in C++ (off the main thread); these tests
# exercise the R helpers that decide what to request and that fold a response
# back into the per-repo cache.
.rs.testPpm <- list(
   repoUrl = "https://example.test/repo/latest"
)

# Swap a tools-env function for the duration of the current test_that() frame.
# Schedules restoration via withr::defer so the swap only lasts for that frame.
withMockFunction <- function(name, fn)
{
   toolsEnv <- .rs.toolsEnv()
   original <- get(name, envir = toolsEnv)
   assign(name, fn, envir = toolsEnv)
   withr::defer(
      assign(name, original, envir = toolsEnv),
      envir = parent.frame()
   )
}

# Reset the per-repo cache and pending-key state between tests so nothing
# leaks between them.
clearVulnsState <- function()
{
   rm(list = ls(envir = .rs.ppm.vulns, all.names = TRUE), envir = .rs.ppm.vulns)
   rm(list = ls(envir = .rs.ppm.pending, all.names = TRUE), envir = .rs.ppm.pending)
}

test_that("getVulnerabilityRequestPlan asks PPM only about uncached keys", {
   clearVulnsState()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   withMockFunction(".rs.ppm.isIntegrationEnabled", function() TRUE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() c("pkgA==1.0.0", "pkgB==2.0.0"))
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))

   expect_length(plan, 1L)
   # only the uncached key should appear in the request body
   expect_true(grepl("pkgB==2.0.0", plan[[1L]]$body, fixed = TRUE))
   expect_false(grepl("pkgA==1.0.0", plan[[1L]]$body, fixed = TRUE))
   # the requested keys are recorded so the response handler can mark them
   expect_equal(.rs.ppm.pending[[.rs.testPpm$repoUrl]], "pkgB==2.0.0")
})

test_that("getVulnerabilityRequestPlan skips repos with no uncached keys", {
   clearVulnsState()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   withMockFunction(".rs.ppm.isIntegrationEnabled", function() TRUE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))

   expect_length(plan, 0L)
   expect_false(exists(.rs.testPpm$repoUrl, envir = .rs.ppm.pending, inherits = FALSE))
})

test_that("getVulnerabilityRequestPlan returns nothing when integration is disabled", {
   clearVulnsState()
   withMockFunction(".rs.ppm.isIntegrationEnabled", function() FALSE)

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_length(plan, 0L)
})

test_that("getVulnerabilityRequestPlan skips non-PPM repositories", {
   clearVulnsState()
   withMockFunction(".rs.ppm.isIntegrationEnabled", function() TRUE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = "https://cran.rstudio.com"))
   expect_length(plan, 0L)
})

test_that("getVulnerabilityRequestPlan returns nothing when no packages are installed", {
   clearVulnsState()
   withMockFunction(".rs.ppm.isIntegrationEnabled", function() TRUE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() character())

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_length(plan, 0L)
})

test_that("recordVulnerabilityResponse caches vulns and empty markers for requested keys", {
   clearVulnsState()
   assign(.rs.testPpm$repoUrl, c("pkgA==1.0.0", "pkgB==2.0.0"), envir = .rs.ppm.pending)

   body <- '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}]}'
   .rs.ppm.recordVulnerabilityResponse(.rs.testPpm$repoUrl, body)

   cache <- .rs.ppm.vulns[[.rs.testPpm$repoUrl]]
   # the parser marks leaf values as scalars, so compare against the marked form
   expect_equal(get("pkgA==1.0.0", envir = cache), expected(list(id = "V1")))
   # pkgB had no vulns reported -- cache an empty marker so we don't re-ask
   expect_equal(get("pkgB==2.0.0", envir = cache), list())
   # the pending entry is cleared once recorded
   expect_false(exists(.rs.testPpm$repoUrl, envir = .rs.ppm.pending, inherits = FALSE))
})

test_that("recordVulnerabilityResponse leaves cache and pending intact on a server error", {
   clearVulnsState()
   assign(.rs.testPpm$repoUrl, "pkgB==2.0.0", envir = .rs.ppm.pending)

   body <- '{"error":"not allowed","code":"403"}'
   expect_warning(
      .rs.ppm.recordVulnerabilityResponse(.rs.testPpm$repoUrl, body),
      "error requesting package vulnerabilities.*not allowed.*403"
   )

   # nothing cached, and the key stays pending so a later refresh retries it
   expect_null(.rs.ppm.vulns[[.rs.testPpm$repoUrl]])
   expect_equal(.rs.ppm.pending[[.rs.testPpm$repoUrl]], "pkgB==2.0.0")
})

test_that("getCachedVulnerabilities aggregates per repo without any network access", {
   clearVulnsState()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")

   result <- .rs.ppm.getCachedVulnerabilities(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_equal(result, list(CRAN = expected(
      pkgA = list(list(id = "V1"))
   )))
})
