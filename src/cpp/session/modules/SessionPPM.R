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

.rs.addFunction("ppm.getVulnerabilityInformation", function(repos = NULL)
{
   # For each available repository, ask it for vulnerability information,
   # then merge that all together.
   repos <- .rs.nullCoalesce(repos, getOption("repos"))

   # PPM's filter/packages endpoint defaults to the current snapshot's latest
   # version of each package, so a vuln that only affects an older installed
   # version is silently dropped. Ask explicitly for the installed (name,
   # version) pairs instead.
   db <- as.data.frame(
      installed.packages(priority = "NA"),
      stringsAsFactors = FALSE
   )
   pkgKeys <- paste(db[["Package"]], db[["Version"]], sep = "==")

   lapply(repos, function(repoUrl)
   {
      tryCatch(
         .rs.ppm.getVulnerabilityInformationImpl(repoUrl, pkgKeys),
         error = function(cnd)
         {
            # outer-catch: a fetch/parse path with a known failure mode
            # would have logged and returned NULL already, so an exception
            # here is unexpected. the caller receives an empty list, which
            # is indistinguishable from "no vulns" -- the log is the only
            # signal that something went wrong.
            .rs.logErrorMessage(sprintf(
               "unexpected error fetching PPM vulnerability information for %s: %s",
               repoUrl, conditionMessage(cnd)
            ))
            structure(list(), names = character())
         }
      )
   })
})

.rs.addFunction("ppm.getVulnerabilityInformationImpl", function(repoUrl, pkgKeys)
{
   empty <- structure(list(), names = character())

   # nothing to ask about
   if (length(pkgKeys) == 0L)
      return(empty)

   # Per-repo cache lives in an environment keyed by "name==version" so we
   # can request only newly-installed (or upgraded) packages on subsequent
   # refreshes. Stale "old==version" entries linger in the cache but are
   # filtered out at aggregation by pkgKeys.
   cache <- .rs.ppm.vulns[[repoUrl]]
   if (is.null(cache))
   {
      cache <- new.env(parent = emptyenv())
      assign(repoUrl, cache, envir = .rs.ppm.vulns)
   }

   # only ask PPM about (name, version) pairs we haven't seen this session
   cachedKeys <- ls(envir = cache, all.names = TRUE)
   newKeys <- setdiff(pkgKeys, cachedKeys)
   if (length(newKeys) > 0L)
   {
      fetched <- .rs.ppm.fetchVulnerabilityInformation(repoUrl, newKeys)

      # NULL means the request failed; skip the cache update so the next
      # refresh can retry rather than masking vulns with empty entries
      if (is.null(fetched))
         return(.rs.ppm.aggregateVulnsByName(cache, pkgKeys))

      # record every requested key (empty list for the ones PPM didn't
      # report on) so we don't keep re-querying packages with no vulns
      for (key in newKeys)
      {
         entry <- fetched[[key]]
         assign(key, if (is.null(entry)) list() else entry, envir = cache)
      }
   }

   .rs.ppm.aggregateVulnsByName(cache, pkgKeys)
})

.rs.addFunction("ppm.fetchVulnerabilityInformation", function(repoUrl, pkgKeys)
{
   ppmUrl <- .rs.ppm.fromRepositoryUrl(repoUrl)
   if (length(ppmUrl) == 0L)
      return(NULL)

   if (!requireNamespace("curl", quietly = TRUE))
   {
      # Without curl every refresh will hit this path and silently return,
      # so we'd lose vulnerability badges with no diagnostic trail. Log
      # once per session and return NULL so callers can still use any
      # cached entries instead of poisoning the cache with empty markers.
      if (is.null(.rs.ppm.warnings$curlMissing))
      {
         .rs.logErrorMessage(
            "cannot fetch PPM vulnerability information: the 'curl' package is not installed"
         )
         .rs.ppm.warnings$curlMissing <- TRUE
      }
      return(NULL)
   }

   # build a curl handle for the request
   verbose <- isTRUE(as.logical(Sys.getenv("PWB_PPM_CURL_VERBOSE", unset = "FALSE")))
   handle <- curl::new_handle(verbose = verbose)

   headers <- list("Content-Type" = "application/json")
   curl::handle_setheaders(handle, .list = headers)

   # ask PPM for vulnerability info on each requested (name, version)
   data <- list(
      repo                 = ppmUrl[["repos"]],
      names                = as.list(pkgKeys),
      has_vulns            = TRUE,
      omit_dependencies    = TRUE,
      omit_downloads       = TRUE,
      omit_package_details = TRUE
   )

   json <- .rs.toJSON(data, unbox = TRUE)

   curl::handle_setopt(
      handle     = handle,
      post       = TRUE,
      postfields = json
   )

   # the legacy /repos/{repo}/vulns endpoint was effectively anonymous;
   # filter/packages requires auth on locked-down PPM instances, so wire
   # up netrc when the user has one configured
   netrcFile <- .rs.netrcPath()
   if (file.exists(netrcFile))
   {
      curl::handle_setopt(
         handle     = handle,
         httpauth   = 1L,         # CURLAUTH_BASIC
         netrc      = 1L,
         netrc_file = path.expand(netrcFile)
      )
   }

   endpoint <- file.path(ppmUrl[["root"]], "__api__/filter/packages")
   response <- tryCatch(
      curl::curl_fetch_memory(endpoint, handle = handle),
      error = identity
   )

   if (inherits(response, "condition"))
   {
      .rs.logErrorMessage(sprintf(
         "PPM vulnerability request to %s failed: %s",
         endpoint, conditionMessage(response)
      ))
      return(NULL)
   }

   if (response$status_code < 200L || response$status_code >= 300L)
   {
      .rs.logErrorMessage(sprintf(
         "PPM vulnerability request to %s returned HTTP %d",
         endpoint, response$status_code
      ))
      return(NULL)
   }

   contents <- enc2utf8(rawToChar(response$content))
   .rs.ppm.parseVulnerabilityResponse(contents)
})

.rs.addFunction("ppm.parseVulnerabilityResponse", function(contents)
{
   empty <- structure(list(), names = character())

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
