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

.rs.addFunction("ppm.isIntegrationEnabled", function()
{
   .Call("rs_ppmIntegrationEnabled", PACKAGE = "(embedding)")
})

.rs.addFunction("ppm.getVulnerabilityInformation", function(repos = NULL)
{
   # For each available repository, ask it for vulnerability information,
   # then merge that all together.
   repos <- .rs.nullCoalesce(repos, getOption("repos"))
   lapply(repos, function(repoUrl)
   {
      tryCatch(
         .rs.ppm.getVulnerabilityInformationImpl(repoUrl),
         error = function(cnd) list()
      )
   })
})

.rs.addFunction("ppm.getVulnerabilityInformationImpl", function(repoUrl)
{
   # If we have cached information available, use it.
   if (exists(repoUrl, envir = .rs.ppm.vulns))
      return(get(repoUrl, envir = .rs.ppm.vulns))
   
   # Request vulnerabilities
   ppmUrl <- .rs.ppm.fromRepositoryUrl(repoUrl)
   if (length(ppmUrl) == 0L)
      return(list())
   
   fmt <- "%s/__api__/repos/%s/vulns"
   endpoint <- sprintf(fmt, ppmUrl$root, ppmUrl$repos)
   destfile <- tempfile("ppm-vuln-")
   
   status <- tryCatch(
      download.file(endpoint, destfile = destfile, quiet = TRUE),
      condition = identity
   )
   
   if (inherits(status, "condition"))
      return(list())
   
   contents <- readLines(destfile, warn = FALSE)
   json <- .rs.fromJSON(contents)
   vulns <- .rs.markScalars(json)
   
   # Cache the result and return.
   assign(repoUrl, vulns, envir = .rs.ppm.vulns)
   vulns
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
   if (length(matches) == 0L)
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
   repos <- getOption("repos")[[1L]]
   .rs.ppm.fromRepositoryUrl(repos)
})

.rs.addFunction("ppm.getAuthorizationHeader", function(parts)
{
   # check for known authority
   authority <- parts[["authority"]]
   if (is.null(authority))
      return(NULL)
   
   # check for netrc credentials
   allcreds <- .rs.readNetrc()
   if (is.null(allcreds))
      return(NULL)
   
   # retrieve credentials for this authority
   creds <- allcreds[[authority]]
   if (is.null(creds))
      return(NULL)
   
   # form authorization header
   username <- creds[["login"]]
   password <- creds[["password"]]
   contents <- paste(username, password, sep = ":")
   if (length(contents) == 0L)
      return(NULL)
   
   encoded <- .rs.base64encode(contents)
   paste("Basic", encoded)
})

.rs.addFunction("ppm.updateMetadataCache", function(packages)
{
   # get the active ppm repository
   parts <- .rs.ppm.getActiveRepository()
   
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
   
   # TODO: can we avoid the curl requirement?
   if (!requireNamespace("curl", quietly = TRUE))
      return(cache)
   
   # begin building a curl handle
   verbose <- Sys.getenv("PWB_PPM_CURL_VERBOSE", unset = "FALSE")
   handle <- curl::new_handle(verbose = if (verbose) TRUE else FALSE)
   
   # set headers for request
   headers <- list("Content-Type" = "application/json")
   curl::handle_setheaders(handle, .list = headers)
   
   # start building POST options
   data <- list(
      repo              = parts[["repos"]],
      snapshot          = parts[["snapshot"]],
      names             = as.list(packages),
      metadata          = TRUE,
      vulns             = TRUE,
      omit_dependencies = TRUE,
      omit_downloads    = TRUE
   )
   
   json <- .rs.toJSON(data, unbox = TRUE)
   
   # get netrc file path
   curl::handle_setopt(
      handle     = handle,
      post       = TRUE,
      postfields = json
   )
   
   # use netrc if available
   netrcFile <- getOption("netrc", default = Sys.getenv("NETRC", unset = "~/.netrc"))
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
