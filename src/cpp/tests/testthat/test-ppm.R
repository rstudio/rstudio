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
# matching the per-repo layout produced by .rs.ppm.repoVulnCache and consumed
# by .rs.ppm.aggregateVulnsByName.
cacheEnv <- function(...)
{
   entries <- list(...)
   env <- new.env(parent = emptyenv())
   for (key in names(entries))
      assign(key, entries[[key]], envir = env)
   env
}

test_that("parseVulnerabilityResponse treats a missing body as nothing-to-record", {
   # character(0) is the crash-guard case: strsplit(character(0), ...) has no
   # [[1L]] element. We return the empty marker (not NULL) so the response is
   # recorded as "checked, no vulns" rather than retried forever.
   result <- .rs.ppm.parseVulnerabilityResponse(character(0))
   expect_equal(result, structure(list(), names = character()))
})

test_that("parseVulnerabilityResponse returns NULL for an empty server body", {
   # a 2xx with an empty body tells us nothing about the requested packages, so
   # we signal failure (NULL) and let a later refresh retry rather than caching
   # every package as "no vulns" and clearing badges
   expect_null(.rs.ppm.parseVulnerabilityResponse(""))
})

test_that("parseVulnerabilityResponse returns NULL for a whitespace-only body", {
   expect_null(.rs.ppm.parseVulnerabilityResponse("   "))
   expect_null(.rs.ppm.parseVulnerabilityResponse("\n"))
   expect_null(.rs.ppm.parseVulnerabilityResponse("\r\n"))
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

   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))

   expect_length(plan, 0L)
   expect_false(exists(.rs.testPpm$repoUrl, envir = .rs.ppm.pending, inherits = FALSE))
})

test_that("getVulnerabilityRequestPlan ignores the integration-enabled flag", {
   # Regression guard: vulnerability display is intentionally NOT opt-in. It is
   # driven by whether a PPM repository is configured (see the PPM-URL check
   # below), NOT by the PWB_PPM_* env vars (.rs.ppm.isIntegrationEnabled, which
   # gates only the Workbench metadata column). A plan must still be built for a
   # PPM repo even when integration reports "disabled". See 88778df845 "always
   # try to provide vuln info if available"; an async rewrite once re-applied the
   # env-var gate here and suppressed vulns for sessions that set repos by hand.
   clearVulnsState()
   withMockFunction(".rs.ppm.isIntegrationEnabled", function() FALSE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_length(plan, 1L)
})

test_that("getVulnerabilityRequestPlan skips non-PPM repositories", {
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = "https://cran.rstudio.com"))
   expect_length(plan, 0L)
})

test_that("getVulnerabilityRequestPlan keeps the PPM repo when a non-PPM repo is mixed in", {
   # the cheap any() pre-filter only decides whether to bail; the per-repo loop
   # does the real filtering, so a non-PPM repo alongside a PPM one must not
   # suppress the PPM plan entry
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(
      repos = c(CRAN = "https://cran.rstudio.com", PPM = .rs.testPpm$repoUrl)
   )
   expect_length(plan, 1L)
   expect_equal(as.character(plan[[1L]]$repoUrl), .rs.testPpm$repoUrl)
})

test_that("getVulnerabilityRequestPlan skips installed.packages() for non-PPM sessions", {
   # the PPM-URL pre-filter exists to avoid the expensive installed.packages()
   # call when no repo qualifies; this locks in that ordering
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() stop("should not be called"))

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = "https://cran.rstudio.com"))
   expect_length(plan, 0L)
})

test_that("getVulnerabilityRequestPlan tolerates an NA repo entry", {
   # getOption("repos") can carry an NA entry (e.g. an unset named repo); the
   # plan must not abort on it (.rs.regexMatches would otherwise throw)
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(
      repos = c(BIOC = NA_character_, PPM = .rs.testPpm$repoUrl)
   )
   expect_length(plan, 1L)
   expect_equal(as.character(plan[[1L]]$repoUrl), .rs.testPpm$repoUrl)
})

test_that("getVulnerabilityRequestPlan returns nothing when no packages are installed", {
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() character())

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_length(plan, 0L)
})

test_that("getVulnerabilityRequestPlan includes the computed auth header and endpoint", {
   clearVulnsState()
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgA==1.0.0")
   # mock the header explicitly so the assertion doesn't depend on the test
   # runner's real ~/.netrc
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "Basic dXNlcjpwYXNz")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(CRAN = .rs.testPpm$repoUrl))
   expect_length(plan, 1L)
   expect_equal(as.character(plan[[1L]]$authHeader), "Basic dXNlcjpwYXNz")
   expect_match(as.character(plan[[1L]]$endpoint), "/__api__/filter/packages$")
})

test_that("getVulnerabilityRequestPlan emits one entry per PPM repo with uncached keys", {
   clearVulnsState()

   repoA <- "https://a.example.test/repo/latest"
   repoB <- "https://b.example.test/repo/latest"

   # repoA already has pkgA cached; repoB has nothing cached yet
   cacheA <- new.env(parent = emptyenv())
   assign("pkgA==1.0.0", list(), envir = cacheA)
   assign(repoA, cacheA, envir = .rs.ppm.vulns)

   withMockFunction(".rs.ppm.installedPackageKeys", function() c("pkgA==1.0.0", "pkgB==2.0.0"))
   withMockFunction(".rs.computeAuthorizationHeader", function(url) "")

   plan <- .rs.ppm.getVulnerabilityRequestPlan(repos = c(A = repoA, B = repoB))
   expect_length(plan, 2L)

   repoUrls <- vapply(plan, function(e) as.character(e$repoUrl), character(1))
   planA <- plan[[which(repoUrls == repoA)]]
   planB <- plan[[which(repoUrls == repoB)]]

   # repoA only needs pkgB (pkgA is cached); repoB needs both
   expect_true(grepl("pkgB==2.0.0", planA$body, fixed = TRUE))
   expect_false(grepl("pkgA==1.0.0", planA$body, fixed = TRUE))
   expect_true(grepl("pkgA==1.0.0", planB$body, fixed = TRUE))
   expect_true(grepl("pkgB==2.0.0", planB$body, fixed = TRUE))
})

test_that("buildVulnerabilityRequestBody sets the repo and omit flags", {
   parsed <- .rs.fromJSON(
      .rs.ppm.buildVulnerabilityRequestBody("cran", c("pkgA==1.0.0", "pkgB==2.0.0"))
   )
   expect_equal(as.character(parsed$repo), "cran")
   expect_true(as.logical(parsed$omit_dependencies))
   expect_true(as.logical(parsed$omit_downloads))
   expect_true(as.logical(parsed$omit_package_details))
   expect_equal(as.character(parsed$names), c("pkgA==1.0.0", "pkgB==2.0.0"))
})

test_that("buildVulnerabilityRequestBody omits has_vulns so all packages are returned", {
   # has_vulns is a server-side filter; sending it would drop every non-vulnerable
   # package from the response, taking its metadata down with it. The single
   # request must return a record for every requested package so we can feed both
   # the vulnerability badges and the metadata column from one response.
   body <- .rs.ppm.buildVulnerabilityRequestBody("cran", "pkgA==1.0.0")
   parsed <- .rs.fromJSON(body)
   expect_null(parsed$has_vulns)
   expect_false(grepl("has_vulns", body, fixed = TRUE))
})

test_that("buildVulnerabilityRequestBody keeps a single package name as a JSON array", {
   # as.list() prevents unbox = TRUE from collapsing a one-element vector into a
   # bare string; PPM's filter/packages endpoint requires names to be an array.
   # fromJSON can't distinguish array-of-one from a scalar, so assert on the
   # serialized form directly.
   body <- .rs.ppm.buildVulnerabilityRequestBody("cran", "pkgA==1.0.0")
   expect_match(body, '"names"[[:space:]]*:[[:space:]]*\\[')
   expect_true(grepl("pkgA==1.0.0", body, fixed = TRUE))
})

test_that("fromRepositoryUrl classifies a plain PPM repo URL", {
   parts <- .rs.ppm.fromRepositoryUrl("https://packagemanager.posit.co/cran/latest")
   expect_equal(parts[["root"]], "https://packagemanager.posit.co")
   expect_equal(parts[["repos"]], "cran")
   expect_equal(parts[["snapshot"]], "latest")
   expect_false(nzchar(parts[["binary"]]))
})

test_that("fromRepositoryUrl tolerates a trailing slash", {
   # RStudio's own CRAN-mirror UI normalizes the repo URL with a trailing slash,
   # so a UI-configured PPM repo lands in options(repos) as ".../latest/". Both
   # forms must be detected as the same repo, or such users silently get no
   # vulnerability badges. (Trailing-slash defect found while investigating
   # rstudio#18018.)
   bare <- .rs.ppm.fromRepositoryUrl("https://packagemanager.posit.co/cran/latest")
   slash <- .rs.ppm.fromRepositoryUrl("https://packagemanager.posit.co/cran/latest/")
   expect_false(is.null(slash))
   expect_equal(slash[["root"]], "https://packagemanager.posit.co")
   expect_equal(slash[["repos"]], "cran")
   expect_equal(slash[["snapshot"]], "latest")
   # the snapshot must not absorb the trailing slash
   expect_equal(slash[["snapshot"]], bare[["snapshot"]])
})

test_that("fromRepositoryUrl extracts the optional binary / platform parts", {
   parts <- .rs.ppm.fromRepositoryUrl(
      "https://packagemanager.posit.co/cran/__linux__/jammy/2024-01-01"
   )
   expect_equal(parts[["repos"]], "cran")
   expect_equal(parts[["binary"]], "__linux__")
   expect_equal(parts[["platform"]], "jammy")
   expect_equal(parts[["snapshot"]], "2024-01-01")
})

test_that("fromRepositoryUrl tolerates a trailing slash on a binary URL", {
   # the optional trailing slash must apply to the binary / platform shape too,
   # and must not bleed into the snapshot.
   parts <- .rs.ppm.fromRepositoryUrl(
      "https://packagemanager.posit.co/cran/__linux__/jammy/2024-01-01/"
   )
   expect_equal(parts[["repos"]], "cran")
   expect_equal(parts[["binary"]], "__linux__")
   expect_equal(parts[["platform"]], "jammy")
   expect_equal(parts[["snapshot"]], "2024-01-01")
})

test_that("fromRepositoryUrl returns NULL for non-PPM and non-string inputs", {
   # the common desktop entries -- "@CRAN@" and "" -- are not PPM URLs, and an
   # NA entry must be tolerated rather than throwing from .rs.regexMatches
   expect_null(.rs.ppm.fromRepositoryUrl("@CRAN@"))
   expect_null(.rs.ppm.fromRepositoryUrl(""))
   expect_null(.rs.ppm.fromRepositoryUrl(NA_character_))
   expect_null(.rs.ppm.fromRepositoryUrl(NULL))
   expect_null(.rs.ppm.fromRepositoryUrl(character()))
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

test_that("recordVulnerabilityResponse caches metadata from the same response", {
   # a single filter/packages response carries both vulns and custom metadata;
   # recording it must populate the metadata cache too (the metadata column reads
   # this cache directly, with no network access of its own)
   clearVulnsState()
   rm(
      list = ls(envir = .rs.ppm.metadataCache, all.names = TRUE),
      envir = .rs.ppm.metadataCache
   )
   assign(.rs.testPpm$repoUrl, c("pkgA==1.0.0", "pkgB==2.0.0"), envir = .rs.ppm.pending)

   body <- paste(
      '{"name":"pkgA","version":"1.0.0","vulns":[{"id":"V1"}],"metadata":[{"key":"risk-level","value":"low"}]}',
      '{"name":"pkgB","version":"2.0.0"}',
      sep = "\n"
   )
   .rs.ppm.recordVulnerabilityResponse(.rs.testPpm$repoUrl, body)

   metaCache <- .rs.ppm.metadataCache[[.rs.testPpm$repoUrl]]
   # pkgA's metadata is cached in the raw [{key,value}] form getMetadata expects
   expect_equal(
      get("pkgA==1.0.0", envir = metaCache),
      list(list(key = "risk-level", value = "low"))
   )
   # pkgB reported no metadata -- cache an NA marker so we don't keep re-asking
   expect_true(is.na(get("pkgB==2.0.0", envir = metaCache)))
})

test_that("metadataByKey keys metadata by name==version and marks misses with NA", {
   records <- list(
      list(name = "pkgA", version = "1.0.0",
           metadata = list(list(key = "risk-level", value = "high"))),
      list(name = "pkgB", version = "2.0.0"),
      list(version = "9.9.9")   # missing name -- skipped entirely
   )
   result <- .rs.ppm.metadataByKey(records)
   expect_equal(result[["pkgA==1.0.0"]], list(list(key = "risk-level", value = "high")))
   expect_true(is.na(result[["pkgB==2.0.0"]]))
   expect_false("9.9.9" %in% names(result))
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

test_that("recordVulnerabilityResponse leaves cache and pending intact on an empty body", {
   # an empty 2xx body parses to NULL (request-failed signal), so nothing should
   # be cached and the key stays pending for a later retry
   clearVulnsState()
   assign(.rs.testPpm$repoUrl, "pkgB==2.0.0", envir = .rs.ppm.pending)

   .rs.ppm.recordVulnerabilityResponse(.rs.testPpm$repoUrl, "")

   expect_null(.rs.ppm.vulns[[.rs.testPpm$repoUrl]])
   expect_equal(.rs.ppm.pending[[.rs.testPpm$repoUrl]], "pkgB==2.0.0")
})

test_that("getMetadata reads the cache for the active repo without any network access", {
   clearVulnsState()
   rm(
      list = ls(envir = .rs.ppm.metadataCache, all.names = TRUE),
      envir = .rs.ppm.metadataCache
   )

   # seed the metadata cache as the async refresh would have
   metaCache <- .rs.ppm.repoMetadataCache(.rs.testPpm$repoUrl)
   assign("pkgA==1.0.0", list(list(key = "risk-level", value = "low")), envir = metaCache)
   assign("pkgB==2.0.0", NA_character_, envir = metaCache)

   withMockFunction(".rs.ppm.getActiveRepository", function() list(url = .rs.testPpm$repoUrl))
   withMockFunction(".rs.ppm.getMetadataKey", function() "risk-level")

   result <- .rs.ppm.getMetadata()
   # the requested key resolves to its value; the NA-marked package yields ""
   expect_equal(as.character(result[["pkgA==1.0.0"]]), "low")
   expect_equal(as.character(result[["pkgB==2.0.0"]]), "")
})

test_that("getMetadata returns an empty list when the active repo has no cache", {
   rm(
      list = ls(envir = .rs.ppm.metadataCache, all.names = TRUE),
      envir = .rs.ppm.metadataCache
   )
   withMockFunction(".rs.ppm.getActiveRepository", function() list(url = .rs.testPpm$repoUrl))
   withMockFunction(".rs.ppm.getMetadataKey", function() "risk-level")

   expect_equal(.rs.ppm.getMetadata(), list())
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

test_that("getCachedVulnerabilities isolates a failing repo from the others", {
   # one repo's aggregation error must not take down the client update for the
   # rest -- the failing repo yields an empty map while the others succeed
   clearVulnsState()

   repoA <- "https://a.example.test/repo/latest"   # will fail to aggregate
   repoB <- "https://b.example.test/repo/latest"   # will aggregate cleanly

   cacheA <- new.env(parent = emptyenv())
   assign("__boom__", TRUE, envir = cacheA)
   assign(repoA, cacheA, envir = .rs.ppm.vulns)

   cacheB <- new.env(parent = emptyenv())
   assign("pkgB==2.0.0", list(list(id = "V2")), envir = cacheB)
   assign(repoB, cacheB, envir = .rs.ppm.vulns)

   warned <- FALSE
   withMockFunction(".rs.logWarningMessage", function(message) warned <<- TRUE)
   withMockFunction(".rs.ppm.installedPackageKeys", function() "pkgB==2.0.0")
   withMockFunction(".rs.ppm.aggregateVulnsByName", function(cache, pkgKeys)
   {
      if (exists("__boom__", envir = cache, inherits = FALSE))
         stop("boom")
      .rs.markScalars(list(pkgB = list(list(id = "V2"))))
   })

   result <- .rs.ppm.getCachedVulnerabilities(repos = c(A = repoA, B = repoB))
   expect_true(warned)
   expect_equal(result$A, structure(list(), names = character()))
   expect_equal(result$B, expected(pkgB = list(list(id = "V2"))))
})
