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

# Tests for getVulnerabilityInformationImpl swap in a mock
# fetchVulnerabilityInformation. The helper clears the session-wide cache
# so each test starts from a known state, then installs the mock and
# arranges for it to be restored when the test exits.
.rs.testPpm <- list(
   repoUrl = "https://example.test/repo/latest"
)

# Install a mock for .rs.ppm.fetchVulnerabilityInformation that runs the
# supplied function. Schedules restoration via withr::defer so the swap
# only lasts for the current test_that() frame. Preserves the closure's
# lexical environment so <<- captures back into the test scope still work.
withMockFetch <- function(fn)
{
   toolsEnv <- .rs.toolsEnv()
   original <- get(".rs.ppm.fetchVulnerabilityInformation", envir = toolsEnv)
   assign(".rs.ppm.fetchVulnerabilityInformation", fn, envir = toolsEnv)
   withr::defer(
      assign(".rs.ppm.fetchVulnerabilityInformation", original, envir = toolsEnv),
      envir = parent.frame()
   )
}

# Reset the per-repo cache between tests so nothing leaks between them.
clearVulnsCache <- function()
{
   rm(list = ls(envir = .rs.ppm.vulns, all.names = TRUE), envir = .rs.ppm.vulns)
}

test_that("getVulnerabilityInformationImpl preserves cached vulns when fetch fails", {
   # Pre-seed the cache for one package, then ask about that package plus
   # a new one. A fetch failure (mock returns NULL) should leave the cache
   # alone and still surface the cached vulns at aggregation time.
   clearVulnsCache()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1", summary = "old")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   withMockFetch(function(repoUrl, pkgKeys) NULL)

   result <- .rs.ppm.getVulnerabilityInformationImpl(
      .rs.testPpm$repoUrl,
      c("pkgA==1.0.0", "pkgB==2.0.0")
   )

   expect_equal(result, expected(
      pkgA = list(list(id = "V1", summary = "old"))
   ))
   # pkgB must not be marked "queried, no vulns" -- the next refresh has
   # to be free to retry it
   expect_false(exists("pkgB==2.0.0", envir = cache, inherits = FALSE))
})

test_that("getVulnerabilityInformationImpl asks PPM only about uncached keys", {
   clearVulnsCache()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   capturedKeys <- NULL
   withMockFetch(function(repoUrl, pkgKeys) {
      capturedKeys <<- pkgKeys
      structure(list(), names = character())
   })

   .rs.ppm.getVulnerabilityInformationImpl(
      .rs.testPpm$repoUrl,
      c("pkgA==1.0.0", "pkgB==2.0.0")
   )

   expect_equal(capturedKeys, "pkgB==2.0.0")
})

test_that("getVulnerabilityInformationImpl caches empty markers for keys PPM did not return", {
   # PPM may report vulns for only a subset of requested keys. The impl
   # caches an empty list for the missing keys so subsequent refreshes
   # don't keep re-asking.
   clearVulnsCache()

   withMockFetch(function(repoUrl, pkgKeys) {
      structure(
         list(list(list(id = "V1"))),
         names = "pkgA==1.0.0"
      )
   })

   result <- .rs.ppm.getVulnerabilityInformationImpl(
      .rs.testPpm$repoUrl,
      c("pkgA==1.0.0", "pkgB==2.0.0")
   )

   expect_equal(result, expected(
      pkgA = list(list(id = "V1"))
   ))

   cache <- .rs.ppm.vulns[[.rs.testPpm$repoUrl]]
   expect_equal(get("pkgA==1.0.0", envir = cache), list(list(id = "V1")))
   expect_equal(get("pkgB==2.0.0", envir = cache), list())
})

test_that("getVulnerabilityInformationImpl returns empty without calling PPM when pkgKeys is empty", {
   clearVulnsCache()

   called <- FALSE
   withMockFetch(function(repoUrl, pkgKeys) {
      called <<- TRUE
      structure(list(), names = character())
   })

   result <- .rs.ppm.getVulnerabilityInformationImpl(.rs.testPpm$repoUrl, character())

   expect_equal(result, structure(list(), names = character()))
   expect_false(called)
})

test_that("getVulnerabilityInformationImpl skips the fetch when every key is already cached", {
   clearVulnsCache()
   cache <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(list(id = "V1")), envir = cache)
   assign(.rs.testPpm$repoUrl, cache, envir = .rs.ppm.vulns)

   called <- FALSE
   withMockFetch(function(repoUrl, pkgKeys) {
      called <<- TRUE
      structure(list(), names = character())
   })

   result <- .rs.ppm.getVulnerabilityInformationImpl(.rs.testPpm$repoUrl, "pkgA==1.0.0")

   expect_false(called)
   expect_equal(result, expected(
      pkgA = list(list(id = "V1"))
   ))
})
