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
   if (!.rs.ppm.isIntegrationEnabled())
      return(list())

   repos <- .rs.nullCoalesce(repos, getOption("repos"))
   if (length(repos) == 0L)
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
   data <- list(
      repo                 = reposName,
      names                = as.list(pkgKeys),
      has_vulns            = TRUE,
      omit_dependencies    = TRUE,
      omit_downloads       = TRUE,
      omit_package_details = TRUE
   )

   .rs.toJSON(data, unbox = TRUE)
})

# Record the response to an asynchronous vulnerability request. Called by the
# C++ backend on the main thread once the network request for 'repoUrl' has
# completed successfully; 'body' is the raw (NDJSON) response payload. Folds the
# response into the per-repo cache. The backend publishes the aggregated result
# (via .rs.ppm.getCachedVulnerabilities) once the whole batch of requests has
# drained, so this only needs to update the cache.
.rs.addFunction("ppm.recordVulnerabilityResponse", function(repoUrl, body)
{
   newKeys <- .rs.ppm.pending[[repoUrl]]
   if (is.null(newKeys))
      newKeys <- character()

   fetched <- .rs.ppm.parseVulnerabilityResponse(body)

   # NULL means the server reported an error on one of its records; leave the
   # cache (and the pending keys) untouched so a later refresh retries rather
   # than masking vulns with empty entries
   if (!is.null(fetched))
   {
      cache <- .rs.ppm.repoVulnCache(repoUrl)

      # record every requested key (empty list for the ones PPM didn't report
      # on) so we don't keep re-querying packages with no vulns
      for (key in newKeys)
      {
         entry <- fetched[[key]]
         assign(key, if (is.null(entry)) list() else entry, envir = cache)
      }

      if (!is.null(.rs.ppm.pending[[repoUrl]]))
         rm(list = repoUrl, envir = .rs.ppm.pending)
   }

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

.rs.addFunction("ppm.parseVulnerabilityResponse", function(contents)
{
   empty <- structure(list(), names = character())

   # a missing body (character(0)) has no records to parse. Guarding here also
   # avoids a "subscript out of bounds" error from strsplit(character(0), ...),
   # which returns an empty list with no [[1L]] element. Treat this as "nothing
   # to record" rather than an error, matching the crash-guard intent.
   if (length(contents) == 0L)
      return(empty)

   # a 2xx with an empty or whitespace-only body tells us nothing about the
   # packages we asked about. Return NULL (the "request failed" signal) so the
   # caller leaves the cache untouched and retries on a later refresh, rather
   # than caching every requested package as "checked, no vulns" and silently
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
   # (rather than empty) lets the caller distinguish "no vulns" from
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

   # group vulns by "name==version" so cached entries can be invalidated
   # individually when a specific installed version is upgraded
   byKey <- empty
   for (record in records)
   {
      name <- record[["name"]]
      version <- record[["version"]]
      pkgVulns <- record[["vulns"]]

      # name or version missing means PPM emitted something we can't key
      # on -- warn once per session if it ever happens (the get-impl
      # cache-fill below prevents us from re-querying), then move on
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

.rs.addFunction("ppm.updateMetadataCache", function(packages)
{
   # get the active ppm repository
   parts <- .rs.ppm.getActiveRepository()
   if (length(parts) == 0L)
      return(new.env(parent = emptyenv()))
   
   # check and see if we've already cached results for these packages
   url <- parts[["url"]]
   cache <- .rs.ppm.metadataCache[[url]] <- .rs.nullCoalesce(
      .rs.ppm.metadataCache[[url]],
      new.env(parent = emptyenv())
   )
   
   # update only packages which haven't yet been cached
   keys <- ls(envir = cache, all.names = TRUE)
   packages <- setdiff(packages, keys)
   if (length(packages) == 0L)
      return(cache)
   
   # one or more packages have not yet been cached; try to update them
   # make a request to the active PPM instance for available metadata
   
   # set some dummy values in our local cache, so we can avoid re-querying
   # packages which have no metadata available (or for queries that fail)
   for (package in packages)
      assign(package, list(), envir = cache)
   
   # TODO: can we avoid the curl requirement?
   if (!requireNamespace("curl", quietly = TRUE))
      return(cache)
   
   # begin building a curl handle
   verbose <- isTRUE(as.logical(Sys.getenv("PWB_PPM_CURL_VERBOSE", unset = "FALSE")))
   handle <- curl::new_handle(verbose = verbose)
   
   # set headers for request
   headers <- list("Content-Type" = "application/json")
   curl::handle_setheaders(handle, .list = headers)
   
   # start building POST options
   data <- list(
      repo                 = parts[["repos"]],
      snapshot             = parts[["snapshot"]],
      names                = as.list(packages),
      metadata             = TRUE,
      vulns                = TRUE,
      omit_dependencies    = TRUE,
      omit_downloads       = TRUE,
      omit_package_details = TRUE
   )
   
   json <- .rs.toJSON(data, unbox = TRUE)
   
   # get netrc file path
   curl::handle_setopt(
      handle     = handle,
      post       = TRUE,
      postfields = json
   )
   
   # use netrc if available
   netrcFile <- .rs.netrcPath()
   if (file.exists(netrcFile))
   {
      curl::handle_setopt(
         handle     = handle,
         httpauth   = 1L,
         netrc      = 1L,
         netrc_file = path.expand(netrcFile)
      )
   }
   
   # make the request, collect the response
   endpoint <- file.path(parts[["root"]], "__api__/filter/packages")
   response <- curl::curl_fetch_memory(endpoint, handle = handle)
   contents <- enc2utf8(rawToChar(response$content))
   splat <- strsplit(contents, "\n", fixed = TRUE)[[1L]]
   data <- lapply(splat, .rs.fromJSON)
   
   # handle errors
   for (i in seq_along(data))
   {
      error <- data[[i]][["error"]]
      if (!is.character(error))
         next
      
      code <- .rs.nullCoalesce(data[[i]][["code"]], "unknown")
      fmt <- "error requesting package metadata; %s [error code %s]"
      msg <- sprintf(fmt, error, as.character(code))
      warning(msg, call. = FALSE)
      return(cache)
   }
   
   # pull out the metadata from each response
   metadata <- lapply(data, `[[`, "metadata")
   if (length(metadata) == 0L)
      return(cache)
   
   names(metadata) <- vapply(data, function(datum) {
      paste(datum[["name"]], datum[["version"]], sep = "==")
   }, FUN.VALUE = character(1))
   
   # add these results to the cache
   list2env(metadata, envir = cache)
   
   # use NA for any requests which had no metadata available
   for (package in setdiff(packages, names(metadata)))
      if (is.null(cache[[package]]))
         cache[[package]] <- NA_character_
   
   # we're done
   cache
   
})

.rs.addFunction("ppm.getMetadataKey", function()
{
   .Call("rs_ppmMetadataKey", PACKAGE = "(embedding)")
})

#' @param key The name of the metadata key which should be pulled.
.rs.addFunction("ppm.getMetadata", function(key = NULL)
{
   parts <- .rs.ppm.getActiveRepository()
   if (length(parts) == 0L)
      return(list())

   # figure out what metadata key should be used
   key <- .rs.nullCoalesce(key, .rs.ppm.getMetadataKey())
   
   # figure out the packages for which we need to request metadata
   db <- as.data.frame(
      installed.packages(priority = "NA"),
      stringsAsFactors = FALSE
   )
   
   packages <- paste(db[["Package"]], db[["Version"]], sep = "==")
   
   # update the metadata cache
   cache <- .rs.ppm.updateMetadataCache(packages)
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
