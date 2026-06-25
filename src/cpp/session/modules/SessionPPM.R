#
# SessionPPM.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

.rs.setVar("ppm.vulns", new.env(parent = emptyenv()))
.rs.setVar("ppm.metadataCache", new.env(parent = emptyenv()))

# per-repo set of "name==version" keys we've asked the C++ backend to fetch
# but haven't yet recorded a response for; keyed by repository URL. The fetch
# itself happens asynchronously in C++ (see SessionPPM.cpp), so this is how the
# request plan communicates the requested keys to the response handler.
.rs.setVar("ppm.pending", new.env(parent = emptyenv()))

# once-per-session flags for noisy diagnostics we don't want to repeat
# on every package-list refresh
.rs.setVar("ppm.warnings", new.env(parent = emptyenv()))

.rs.addFunction("ppm.isIntegrationEnabled", function()
{
   .Call("rs_ppmIntegrationEnabled", PACKAGE = "(embedding)")
})

.rs.addFunction("ppm.isMetadataColumnEnabled", function()
{
   .Call("rs_ppmMetadataColumnEnabled", PACKAGE = "(embedding)")
})

.rs.addFunction("ppm.installedPackageKeys", function()
{
   # PPM's filter/packages endpoint defaults to the current snapshot's latest
   # version of each package, so a vuln that only affects an older installed
   # version is silently dropped. We key everything on the installed (name,
   # version) pairs instead so older versions still get checked.
   db <- as.data.frame(
      installed.packages(priority = "NA"),
      stringsAsFactors = FALSE
   )
   paste(db[["Package"]], db[["Version"]], sep = "==")
})

.rs.addFunction("ppm.repoVulnCache", function(repoUrl)
{
   # Per-repo cache lives in an environment keyed by "name==version" so we can
   # request only newly-installed (or upgraded) packages on subsequent
   # refreshes. Stale "old==version" entries linger in the cache but are
   # filtered out at aggregation by pkgKeys.
   cache <- .rs.ppm.vulns[[repoUrl]]
   if (is.null(cache))
   {
      cache <- new.env(parent = emptyenv())
      assign(repoUrl, cache, envir = .rs.ppm.vulns)
   }
   cache
})

.rs.addFunction("ppm.repoMetadataCache", function(repoUrl)
{
   # Per-repo metadata cache, keyed by "name==version" like the vuln cache and
   # populated from the same response (see ppm.recordVulnerabilityResponse). Read
   # cache-only by ppm.getMetadata; no network access happens at read time.
   cache <- .rs.ppm.metadataCache[[repoUrl]]
   if (is.null(cache))
   {
      cache <- new.env(parent = emptyenv())
      assign(repoUrl, cache, envir = .rs.ppm.metadataCache)
   }
   cache
})

# Build the request plan for an asynchronous vulnerability refresh. The actual
# network request is performed by the C++ backend (off the main thread); this
# helper only inspects local, already-installed state to decide *what* to
# request and how to authenticate. It returns one entry per PPM repository that
# has packages we haven't yet checked this session:
#
#   list(repoUrl, endpoint, authHeader, body)
#
# and records the requested (name==version) keys in .rs.ppm.pending so the
# response handler (ppm.recordVulnerabilityResponse) knows which keys the
# response covers.
.rs.addFunction("ppm.getVulnerabilityRequestPlan", function(repos = NULL)
{
   # NOTE: enablement is driven by whether the session's repositories point at a
   # Posit Package Manager instance -- NOT by .rs.ppm.isIntegrationEnabled() (the
   # PWB_PPM_* env vars). Vulnerability display is intentionally not opt-in: it
   # is shown whenever a PPM repo is in use, including open-source / desktop
   # RStudio. The env-var gate gates the Workbench-only metadata column
   # (.rs.ppm.isMetadataColumnEnabled), not vulnerabilities. See 88778df845
   # "always try to provide vuln info if available"; an async rewrite had briefly
   # re-applied the env-var gate here, which regressed that behavior.
   repos <- .rs.nullCoalesce(repos, getOption("repos"))
   if (length(repos) == 0L)
      return(list())

   # bail out early -- before the potentially-expensive installed.packages() call
   # below -- if none of the configured repositories is a PPM instance
   isPpmRepo <- vapply(repos, function(url) {
      length(.rs.ppm.fromRepositoryUrl(url)) > 0L
   }, logical(1))
   if (!any(isPpmRepo))
      return(list())

   pkgKeys <- .rs.ppm.installedPackageKeys()
   if (length(pkgKeys) == 0L)
      return(list())

   plan <- list()
   for (repoUrl in repos)
   {
      ppmUrl <- .rs.ppm.fromRepositoryUrl(repoUrl)
      if (length(ppmUrl) == 0L)
         next

      # only ask PPM about (name, version) pairs we haven't seen this session
      cache <- .rs.ppm.repoVulnCache(repoUrl)
      cachedKeys <- ls(envir = cache, all.names = TRUE)
      newKeys <- setdiff(pkgKeys, cachedKeys)
      if (length(newKeys) == 0L)
         next

      # the legacy /repos/{repo}/vulns endpoint was effectively anonymous;
      # filter/packages requires auth on locked-down PPM instances, so compute
      # a Basic auth header from the user's .netrc when one is configured
      endpoint <- file.path(ppmUrl[["root"]], "__api__/filter/packages")
      authHeader <- .rs.computeAuthorizationHeader(endpoint)

      # remember which keys this request covers so the response handler can
      # mark them (including the ones with no vulns) as checked
      assign(repoUrl, newKeys, envir = .rs.ppm.pending)

      plan[[length(plan) + 1L]] <- list(
         repoUrl    = .rs.scalar(repoUrl),
         endpoint   = .rs.scalar(endpoint),
         authHeader = .rs.scalar(authHeader),
         body       = .rs.scalar(.rs.ppm.buildVulnerabilityRequestBody(ppmUrl[["repos"]], newKeys))
      )
   }

   plan
})

.rs.addFunction("ppm.buildVulnerabilityRequestBody", function(reposName, pkgKeys)
{
   # NOTE: we intentionally do NOT set has_vulns. has_vulns is a server-side
   # *filter* (true => only vulnerable packages); omitting it returns a record
   # for every requested package, each carrying both its vulns array (when the
   # package is vulnerable) and its custom metadata. That lets this single
   # request feed both the vulnerability badges and the PPM metadata column --
   # see ppm.recordVulnerabilityResponse, which folds both out of one response.
   #
   # omit_package_details drops the heavier package-detail fields but does NOT
   # suppress the custom metadata array, so we keep it for a leaner response.
   data <- list(
      repo                 = reposName,
      names                = as.list(pkgKeys),
      omit_dependencies    = TRUE,
      omit_downloads       = TRUE,
      omit_package_details = TRUE
   )

   .rs.toJSON(data, unbox = TRUE)
})

#' Request PPM vulnerability info for a single package, synchronously.
#'
#' A by-hand debugging helper that exercises the same request-and-parse path as
#' the asynchronous refresh (see ppm.getVulnerabilityRequestPlan in this file and
#' ppm::refreshVulnerabilitiesAsync in SessionPPM.cpp), but performs the
#' filter/packages POST synchronously from R so the result can be inspected
#' directly at the console. Unlike the live path, it does NOT update the per-repo
#' caches or fire a client event -- it just issues one request and returns the
#' parsed records.
#'
#' @param package The package name to query.
#' @param version The package version to query. When NULL (the default), PPM is
#'   asked about the package by name only and answers for its current-snapshot
#'   version.
#' @param repoUrl The repository URL to query. When NULL (the default), the first
#'   PPM repository found in getOption("repos") is used.
#' @return The parsed per-package records (see ppm.parsePackageRecords), each
#'   carrying both its 'vulns' and 'metadata'; an empty list when PPM reported
#'   nothing; or NULL when the request failed.
.rs.addFunction("ppm.requestPackageVulnerabilities", function(package, version = NULL, repoUrl = NULL)
{
   # resolve the repository to query: the caller's repoUrl, else the first
   # configured PPM repository
   if (is.null(repoUrl))
   {
      for (url in getOption("repos"))
      {
         if (length(.rs.ppm.fromRepositoryUrl(url)) > 0L)
         {
            repoUrl <- url
            break
         }
      }
   }

   if (is.null(repoUrl))
      stop("no PPM repository found in getOption(\"repos\"); pass 'repoUrl' explicitly")

   ppmUrl <- .rs.ppm.fromRepositoryUrl(repoUrl)
   if (length(ppmUrl) == 0L)
      stop(sprintf("'%s' is not a Posit Package Manager repository URL", repoUrl))

   # build the same request the async plan would: a single "name" (or
   # "name==version") key posted to filter/packages, authenticated from the
   # user's .netrc when one is configured
   key <- if (is.null(version)) package else paste(package, version, sep = "==")
   endpoint <- file.path(ppmUrl[["root"]], "__api__/filter/packages")
   authHeader <- .rs.computeAuthorizationHeader(endpoint)
   body <- .rs.ppm.buildVulnerabilityRequestBody(ppmUrl[["repos"]], key)

   # POST synchronously and parse via the same helper the async path uses
   response <- .rs.ppm.postSync(endpoint, authHeader, body)
   .rs.ppm.parsePackageRecords(response)
})

#' Perform a synchronous HTTP POST of a JSON body via the curl binary.
#'
#' Used only by the by-hand ppm.requestPackageVulnerabilities helper; the live
#' refresh posts asynchronously from C++ (see sendVulnerabilityRequest in
#' SessionPPM.cpp). Returns the response body as a single newline-joined string,
#' the shape ppm.parsePackageRecords expects (it parses only the first element
#' of a multi-element vector, so the lines must be collapsed here).
.rs.addFunction("ppm.postSync", function(endpoint, authHeader, body)
{
   curl <- Sys.which("curl")
   if (!nzchar(curl))
      stop("could not find the 'curl' executable on the PATH")

   args <- c(
      "--silent", "--show-error",
      "--request", "POST",
      "--header", shQuote("Content-Type: application/json")
   )

   if (nzchar(authHeader))
      args <- c(args, "--header", shQuote(paste0("Authorization: ", authHeader)))

   args <- c(
      args,
      "--data", shQuote(body),
      shQuote(endpoint)
   )

   output <- suppressWarnings(system2(curl, args, stdout = TRUE, stderr = TRUE))

   status <- attr(output, "status")
   if (!is.null(status) && status != 0L)
      stop(sprintf("curl request to '%s' failed (status %d): %s",
                   endpoint, status, paste(output, collapse = "\n")))

   paste(output, collapse = "\n")
})

# Record the response to an asynchronous filter/packages request. Called by the
# C++ backend on the main thread once the network request for 'repoUrl' has
# completed successfully; 'body' is the raw (NDJSON) response payload. A single
# response carries both vulnerability and custom-metadata data, so this folds
# both into their per-repo caches. The backend publishes the aggregated vulns
# (via .rs.ppm.getCachedVulnerabilities) and re-delivers the package list (for
# the metadata column) once the whole batch of requests has drained, so this
# only needs to update the caches.
.rs.addFunction("ppm.recordVulnerabilityResponse", function(repoUrl, body)
{
   newKeys <- .rs.ppm.pending[[repoUrl]]
   if (is.null(newKeys))
      newKeys <- character()

   records <- .rs.ppm.parsePackageRecords(body)

   # NULL means the request failed (an empty body, or the server reported an
   # error on one of its records); leave the caches (and the pending keys)
   # untouched so a later refresh retries rather than masking vulns or metadata
   # with empty entries
   if (is.null(records))
      return(invisible(NULL))

   vulnsByKey <- .rs.ppm.vulnsByKey(records)
   metadataByKey <- .rs.ppm.metadataByKey(records)

   vulnCache <- .rs.ppm.repoVulnCache(repoUrl)
   metaCache <- .rs.ppm.repoMetadataCache(repoUrl)

   # record every requested key (an empty/NA marker for the ones PPM didn't
   # report on) so we don't keep re-querying packages with no vulns or metadata
   for (key in newKeys)
   {
      vulnEntry <- vulnsByKey[[key]]
      assign(key, if (is.null(vulnEntry)) list() else vulnEntry, envir = vulnCache)

      metaEntry <- metadataByKey[[key]]
      assign(key, if (is.null(metaEntry)) NA_character_ else metaEntry, envir = metaCache)
   }

   if (!is.null(.rs.ppm.pending[[repoUrl]]))
      rm(list = repoUrl, envir = .rs.ppm.pending)

   invisible(NULL)
})

# Aggregate the cached vulnerability data for all configured repositories,
# without performing any network access. Returns a list (one element per repo)
# of package-name -> vulnerability lists.
.rs.addFunction("ppm.getCachedVulnerabilities", function(repos = NULL)
{
   repos <- .rs.nullCoalesce(repos, getOption("repos"))
   pkgKeys <- .rs.ppm.installedPackageKeys()

   lapply(repos, function(repoUrl)
   {
      empty <- structure(list(), names = character())

      cache <- .rs.ppm.vulns[[repoUrl]]
      if (is.null(cache))
         return(empty)

      # isolate per-repo failures so one repo's malformed cache entry can't take
      # down the aggregation (and therefore the client update) for every repo
      tryCatch(
         .rs.ppm.aggregateVulnsByName(cache, pkgKeys),
         error = function(e) {
            .rs.logWarningMessage(paste(
               "error aggregating PPM vulnerabilities for", repoUrl, "-", conditionMessage(e)
            ))
            empty
         }
      )
   })
})

# Parse a filter/packages NDJSON body into a list of per-package records.
# Returns:
#   - an empty list for a missing body (character(0)): "nothing to record"
#   - NULL for a failure: an empty/whitespace-only 2xx body, or a body in which
#     the server reported an error on any line. NULL is the "request failed"
#     signal that tells callers to leave their caches untouched and retry on a
#     later refresh rather than caching every package as "checked, nothing".
#   - otherwise, the parsed records (lines that aren't valid JSON are skipped)
.rs.addFunction("ppm.parsePackageRecords", function(contents)
{
   # a missing body (character(0)) has no records to parse. Guarding here also
   # avoids a "subscript out of bounds" error from strsplit(character(0), ...),
   # which returns an empty list with no [[1L]] element. Treat this as "nothing
   # to record" rather than an error, matching the crash-guard intent.
   if (length(contents) == 0L)
      return(list())

   # a 2xx with an empty or whitespace-only body tells us nothing about the
   # packages we asked about. Return NULL (the "request failed" signal) so the
   # caller leaves the cache untouched and retries on a later refresh, rather
   # than caching every requested package as "checked, nothing" and silently
   # clearing any badges. trimws() is needed here because nzchar() considers a
   # whitespace-only string non-empty.
   if (!any(nzchar(trimws(contents))))
      return(NULL)

   # tolerate CRLF in case a proxy or PPM build rewrites line endings
   contents <- gsub("\r", "", contents, fixed = TRUE)
   splat <- strsplit(contents, "\n", fixed = TRUE)[[1L]]
   splat <- splat[nzchar(splat)]
   records <- lapply(splat, .rs.fromJSON)

   # bail out if the server reported an error on any line. Returning NULL
   # (rather than empty) lets the caller distinguish "nothing reported" from
   # "request failed" and skip the cache, so a transient failure doesn't
   # mask vulns for the rest of the session.
   for (record in records)
   {
      if (is.character(record[["error"]]))
      {
         code <- .rs.nullCoalesce(record[["code"]], "unknown")
         warning(sprintf(
            "error requesting package vulnerabilities; %s [error code %s]",
            record[["error"]], as.character(code)
         ), call. = FALSE)
         return(NULL)
      }
   }

   records
})

# Group the vulns out of a parsed record list, keyed by "name==version" so a
# cached entry can be invalidated individually when an installed version is
# upgraded. Records missing a name/version, or with no vulns, are skipped.
.rs.addFunction("ppm.vulnsByKey", function(records)
{
   byKey <- structure(list(), names = character())
   for (record in records)
   {
      name <- record[["name"]]
      version <- record[["version"]]
      pkgVulns <- record[["vulns"]]

      # name or version missing means PPM emitted something we can't key on --
      # warn once per session if it ever happens, then move on
      if (is.null(name) || is.null(version))
      {
         if (is.null(.rs.ppm.warnings$malformedRecord))
         {
            .rs.logWarningMessage(
               "PPM vulnerability response included a record missing 'name' or 'version'; subsequent malformed records will be silently skipped"
            )
            .rs.ppm.warnings$malformedRecord <- TRUE
         }
         next
      }

      if (length(pkgVulns) == 0L)
         next

      key <- paste(name, version, sep = "==")
      byKey[[key]] <- c(byKey[[key]], pkgVulns)
   }

   .rs.markScalars(byKey)
})

# Pull the custom metadata out of a parsed record list, keyed by "name==version"
# to match the vuln cache. A record with no metadata maps to NA so the response
# handler can still mark the key as checked (avoiding a re-query).
.rs.addFunction("ppm.metadataByKey", function(records)
{
   byKey <- list()
   for (record in records)
   {
      name <- record[["name"]]
      version <- record[["version"]]
      if (is.null(name) || is.null(version))
         next

      key <- paste(name, version, sep = "==")
      metadata <- record[["metadata"]]
      byKey[[key]] <- if (is.null(metadata)) NA_character_ else metadata
   }

   byKey
})

# Thin wrapper retained for callers/tests that only care about vulnerability
# data. Returns the by-"name==version" vuln map, or NULL on request failure.
.rs.addFunction("ppm.parseVulnerabilityResponse", function(contents)
{
   records <- .rs.ppm.parsePackageRecords(contents)
   if (is.null(records))
      return(NULL)

   .rs.ppm.vulnsByKey(records)
})

.rs.addFunction("ppm.aggregateVulnsByName", function(cache, pkgKeys)
{
   # pull only the cache entries for currently-installed (name, version)
   # keys, then re-label by just the package name
   keys <- intersect(pkgKeys, ls(envir = cache, all.names = TRUE))
   entries <- mget(keys, envir = cache)
   names(entries) <- sub("==.*$", "", names(entries))

   # group by package name and concatenate; a single installed version is
   # the common case, but the same package can show up across multiple
   # libraries, in which case we concatenate without deduping -- the
   # frontend filters per row via each vuln's versions map
   groups <- split(entries, names(entries))
   vulns <- lapply(groups, function(group) do.call(c, unname(group)))

   # drop packages with no vulns
   .rs.markScalars(vulns[lengths(vulns) > 0L])
})

.rs.addFunction("ppm.fromRepositoryUrl", function(url)
{
   # guard against non-string / NA repo entries before .rs.regexMatches: a
   # gregexpr-driven substring() on NA throws "invalid substring arguments",
   # which would abort the whole vulnerability plan. getOption("repos") can
   # legitimately carry an NA entry (e.g. an unset named repo), and this is now
   # scanned unconditionally for every session, so handle it here.
   if (!is.character(url) || length(url) != 1L || is.na(url))
      return(NULL)

   pattern <- paste0(
      "^",                                  # start of url
      "(?<root>",                           # start of root of url
         "(?<scheme>[^:]+)://",             # scheme
         "(?<authority>[^/]+)",             # authority
      ")/",                                 # end of root of url
      "(?<repos>[^/]+)/",                   # repository name
      "(?:",                                # begin optional binary parts
         "(?<binary>__[^_]+__)/",           # binary prefix
         "(?<platform>[^/]+)/",             # platform for binaries
      ")?",                                 # end optional binary parts
      "(?<snapshot>[^/]+)",                 # snapshot
      "/?",                                 # tolerate a trailing slash
      "$"
   )
   
   matches <- .rs.regexMatches(pattern, url)
   if (length(matches) == 0L || !any(nzchar(matches)))
      return(NULL)
   
   as.list(c(url = url, matches))

})

.rs.addFunction("ppm.toRepositoryUrl", function(parts)
{
   components <- c(
      parts[["root"]],
      parts[["repos"]],
      if (nzchar(parts[["binary"]])) c(
         parts[["binary"]],
         parts[["platform"]]
      ),
      parts[["snapshot"]]
   )
   
   paste(components, collapse = "/")
})

.rs.addFunction("ppm.getActiveRepository", function()
{
   repos <- getOption("repos")
   if (length(repos) == 0L)
      return(list())

   .rs.ppm.fromRepositoryUrl(repos[[1L]])
})

.rs.addFunction("ppm.getMetadataKey", function()
{
   .Call("rs_ppmMetadataKey", PACKAGE = "(embedding)")
})

#' Read cached PPM metadata for the active repository.
#'
#' Returns a (package-name -> value) map for the requested metadata key, drawn
#' entirely from the local cache. The cache is populated off the main thread by
#' the asynchronous filter/packages refresh (see ppm.recordVulnerabilityResponse
#' in this file and ppm::refreshVulnerabilitiesAsync in SessionPPM.cpp), so this
#' performs NO network access -- a slow or unreachable PPM can never block the
#' package list (and therefore the IDE) here. Packages with no cached metadata
#' yet (or none available) map to "".
#'
#' @param key The name of the metadata key which should be pulled.
.rs.addFunction("ppm.getMetadata", function(key = NULL)
{
   parts <- .rs.ppm.getActiveRepository()
   if (length(parts) == 0L)
      return(list())

   # figure out what metadata key should be used
   key <- .rs.nullCoalesce(key, .rs.ppm.getMetadataKey())

   # read whatever the asynchronous refresh has cached for the active repo
   cache <- .rs.ppm.metadataCache[[parts[["url"]]]]
   if (is.null(cache))
      return(list())

   metadata <- as.list.environment(cache, all.names = TRUE)
   if (length(metadata) == 0L)
      return(list())

   # filter to requested metadata key
   result <- lapply(metadata, function(data) {
      for (datum in data)
         if (identical(datum[[1L]], key))
            return(datum[[2L]])
      ""
   })
   names(result) <- names(metadata)

   # return sorted metadata
   result[order(names(result))]
})
