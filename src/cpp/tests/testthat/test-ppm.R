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

test_that("aggregateVulnsByName dedupes the same vuln across multiple installed versions", {
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "hi")),
      `pkgA==1.1.0` = list(list(id = "V1", summary = "hi"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgA==1.1.0"))
   expect_equal(result, expected(
      pkgA = list(list(id = "V1", summary = "hi"))
   ))
})

test_that("aggregateVulnsByName keeps disjoint vulns from different versions", {
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", summary = "first")),
      `pkgA==2.0.0` = list(list(id = "V2", summary = "second"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgA==2.0.0"))
   expect_equal(result, expected(
      pkgA = list(
         list(id = "V1", summary = "first"),
         list(id = "V2", summary = "second")
      )
   ))
})

test_that("aggregateVulnsByName merges versions maps when deduping by id", {
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(id = "V1", versions = list(`1.0.0` = TRUE))),
      `pkgA==1.1.0` = list(list(id = "V1", versions = list(`1.1.0` = TRUE))),
      `pkgA==1.2.0` = list(list(
         id = "V1",
         versions = list(`1.0.0` = TRUE, `1.2.0` = TRUE)
      ))
   )
   result <- .rs.ppm.aggregateVulnsByName(
      cache,
      c("pkgA==1.0.0", "pkgA==1.1.0", "pkgA==1.2.0")
   )
   expect_equal(result, expected(
      pkgA = list(list(
         id = "V1",
         versions = list(`1.0.0` = TRUE, `1.1.0` = TRUE, `1.2.0` = TRUE)
      ))
   ))
})

test_that("aggregateVulnsByName keeps id-less vulns separate", {
   # Each id-less vuln coalesces to id = "", but R's list[[""]] always
   # reads NULL while each write appends a fresh positional slot, so the
   # dedup loop treats them as distinct. In practice PPM always emits an
   # OSV id; this just pins the fallback behavior.
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(summary = "one")),
      `pkgA==1.1.0` = list(list(summary = "two"))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgA==1.1.0"))
   expect_equal(result, expected(
      pkgA = list(
         list(summary = "one"),
         list(summary = "two")
      )
   ))
})

test_that("aggregateVulnsByName keeps the first record on same-id dedup", {
   # When the same vuln id appears with different summaries across versions,
   # the first occurrence wins; only the versions map gets merged.
   cache <- cacheEnv(
      `pkgA==1.0.0` = list(list(
         id = "V1", summary = "first", versions = list(`1.0.0` = TRUE)
      )),
      `pkgA==2.0.0` = list(list(
         id = "V1", summary = "second", versions = list(`2.0.0` = TRUE)
      ))
   )
   result <- .rs.ppm.aggregateVulnsByName(cache, c("pkgA==1.0.0", "pkgA==2.0.0"))
   expect_equal(result, expected(
      pkgA = list(list(
         id = "V1",
         summary = "first",
         versions = list(`1.0.0` = TRUE, `2.0.0` = TRUE)
      ))
   ))
})

test_that("aggregateVulnsByName skips cache entries that aren't currently installed", {
   # A package upgrade leaves the old (name, version) key behind in the
   # cache. aggregateVulnsByName must filter by the currently-installed
   # set so the orphan doesn't leak back into the UI.
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
