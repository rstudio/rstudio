#
# SessionObjectExplorer.R
#
# Copyright (C) 2009-17 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# NOTE: these should be synchronized with the enum defined in ObjectExplorerEvent.java
.rs.setVar("explorer.types", list(
   NEW        = "new",
   OPEN_NODE  = "open_node",
   CLOSE_NODE = "close_node"
))

.rs.setVar("explorer.tags", list(
   ATTRIBUTES = "attributes",
   VIRTUAL    = "virtual"
))

# NOTE: this should be synchronized with DEFAULT_ROW_LIMIT
# in ObjectExplorerDataGrid.java
.rs.setVar("explorer.defaultRowLimit", 1000)

# this environment holds data objects currently open within
# a viewer tab; this environment will be persisted across
# RStudio sessions
.rs.setVar("explorer.cache", new.env(parent = emptyenv()))

# this environment holds custom inspectors that might be
# registered by client packages
.rs.setVar("explorer.inspectorRegistry", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("explorer_inspect_object", function(id,
                                                          extractingCode,
                                                          name,
                                                          access,
                                                          tags,
                                                          start)
{
   # retrieve object from cache
   object <- .rs.explorer.getCachedObject(
      id = id,
      extractingCode = extractingCode,
      refresh = FALSE
   )
   
   # construct context
   context <- .rs.explorer.createContext(
      name      = name,
      access    = access,
      tags      = tags,
      recursive = 1,
      start     = start + 1,   # 0 -> 1-based indexing,
      end       = start + .rs.explorer.defaultRowLimit
   )
   
   # generate inspection result
   result <- .rs.explorer.inspectObject(object, context)
   result
})

.rs.addJsonRpcHandler("explorer_begin_inspect", function(id, name)
{
   # retrieve object from cache
   object <- .rs.explorer.getCachedObject(
      id = id,
      extractingCode = NULL,
      refresh = TRUE
   )
   
   # construct context
   context <- .rs.explorer.createContext(
      name      = name,
      access    = NULL,
      tags      = character(),
      recursive = 1,
      start     = 1,
      end       = .rs.explorer.defaultRowLimit
   )
   
   # generate inspection result
   result <- .rs.explorer.inspectObject(object, context)
   result
})

.rs.addJsonRpcHandler("explorer_end_inspect", function(id)
{
   .rs.explorer.removeCachedObject(id)
})

.rs.addFunction("objectAddress", function(object)
{
   .Call("rs_objectAddress", object, PACKAGE = "(embedding)")
})

.rs.addFunction("objectClass", function(object)
{
   .Call("rs_objectClass", object, PACKAGE = "(embedding)")
})

.rs.addFunction("objectType", function(object)
{
   type <- typeof(object)
   
   if (type %in% "closure")
      type <- "function"
   else if (type %in% c("builtin", "special"))
      type <- "function (primitive)"
   
   dimensions <- dim(object)
   if (!is.null(dimensions))
   {
      dimtext <- paste(dimensions, collapse = " x ")
      type <- paste(type, sprintf("[%s]", dimtext))
   }
   else if (is.character(object) || is.numeric(object) ||
            is.raw(object) || is.complex(object) ||
            is.list(object) || is.environment(object) ||
            is.factor(object) || is.logical(object))
   {
      type <- paste(type, sprintf("[%s]", length(object)))
   }
   
   type
})

.rs.addFunction("objectAttributes", function(object)
{
   .Call("rs_objectAttributes", object, PACKAGE = "(embedding)")
})

.rs.addFunction("explorer.hasRelevantAttributes", function(object)
{
   attrib <- attributes(object)
   
   boring <-
      is.null(attrib) ||
      identical(object, attrib) ||
      identical(names(attrib), "names")
   
   if (boring)
      return(FALSE)
   
   TRUE
})

.rs.addFunction("explorer.saveCache", function(cacheDir)
{
   cache <- .rs.explorer.getCache()
   ids <- ls(envir = cache)
   lapply(ids, function(id) {
      file <- file.path(cacheDir, id)
      tryCatch(
         saveRDS(cache[[id]], file = file),
         error = warning
      )
   })
})

.rs.addFunction("explorer.restoreCache", function(cacheDir)
{
   cache <- .rs.explorer.getCache()
   ids <- list.files(cacheDir)
   for (id in ids) {
      tryCatch(
         {
            path <- file.path(cacheDir, id)
            object <- readRDS(path)
            cache[[id]] <- object
         },
         
         error = warning
      )
   }
})

.rs.addFunction("explorer.getCache", function()
{
   .rs.explorer.cache
})

.rs.addFunction("explorer.getCachedObject", function(id,
                                                     extractingCode = NULL,
                                                     refresh = FALSE)
{
   # retrieve cached entry
   cache <- .rs.explorer.getCache()
   entry <- cache[[id]]
   
   # handle NULL entries (e.g. the cache somehow became out-of-sync)
   if (is.null(entry))
      return(NULL)
   
   # get object (refreshing if requested). note that refreshes following a
   # restart may lose reference to the original object if e.g. the object lived
   # in the global environment but the global environment was not restored
   if (refresh && is.character(entry$title)) {
      tryCatch(
         expr = {
            object <- eval(parse(text = entry$title), envir = entry$envir)
            entry$object <- object
            cache[[id]] <- entry
         },
         error = identity
      )
   }
   object <- entry$object
   
   # return if no sub-extraction needed
   if (is.null(extractingCode))
      return(object)
   
   # otherwise, evaluate expression to retrieve sub-object
   envir <- new.env(parent = globalenv())
   envir[["__OBJECT__"]] <- object
   
   tryCatch(
      eval(parse(text = extractingCode), envir = envir),
      error = warning
   )
})

.rs.addFunction("explorer.setCachedObject", function(object,
                                                     id = .rs.createUUID())
{
   cache <- .rs.explorer.getCache()
   cache[[id]] <- object
   id
})

.rs.addFunction("explorer.removeCachedObject", function(id)
{
   cache <- .rs.explorer.getCache()
   if (exists(id, envir = cache))
      rm(list = id, envir = cache)
})

#' @param name The display name, as should be used in UI.
#' @param access A string of R code, indicating how this object
#'   should be accessed from a parent object. The '#' character
#'   is used as a placeholder for the name of the parent object.
#' @param tags An optional character vector of tags, used to identify
#'   special nodes (e.g. R attributes).
#' @param recursive Whether children of this object should be
#'   inspected recursively (if applicable). Can either be a boolean
#'   argument, or a numeric argument indicating the maximum depth
#'   of the recursion.
#' @param start The index at which inspection should begin, for children.
#' @param end The index at which inspection should end, for children.
.rs.addFunction("explorer.createContext", function(name = NULL,
                                                   access = NULL,
                                                   tags = character(),
                                                   recursive = FALSE,
                                                   start = 1,
                                                   end = .rs.explorer.defaultRowLimit)
{
   list(
      name      = name,
      access    = access,
      tags      = tags,
      recursive = recursive,
      start     = start,
      end       = end
   )
})

.rs.addFunction("explorer.createChildContext", function(context,
                                                        name,
                                                        access,
                                                        tags)
{
   # decrement a numeric recursion count
   recursive <- context$recursive
   if (is.numeric(recursive))
      recursive <- recursive - 1
   
   # establish a new context
   .rs.explorer.createContext(
      name      = name,
      access    = access,
      tags      = tags,
      recursive = recursive,
      start     = 1,
      end       = .rs.explorer.defaultRowLimit
   )
})

.rs.addFunction("explorer.fireEvent", function(type, data = list())
{
   .rs.enqueClientEvent("object_explorer_event", list(
      type = .rs.scalar(type),
      data = data
   ))
})


.rs.addFunction("explorer.viewObject", function(object,
                                                title = NULL,
                                                envir = .GlobalEnv)
{
   # attempt to divine title from call when not supplied
   if (is.null(title))
   {
      call <- match.call()
      title <- paste(deparse(call$object, width.cutoff = 500), collapse = " ")
   }
   
   # provide an appropriate name for the root node
   name <- .rs.explorer.objectName(object, title)
   
   # generate a handle for this object
   handle <- .rs.explorer.createHandle(object, name, title, envir)
   
   # fire event to client
   .rs.explorer.fireEvent(.rs.explorer.types$NEW, handle)
})

.rs.addFunction("explorer.createHandle", function(object,
                                                  name,
                                                  title,
                                                  envir)
{
   # save in cached data environment
   entry <- list(object = object, name = name, title = title, envir = envir)
   id <- .rs.explorer.setCachedObject(entry)
   
   # return a handle object
   list(
      id    = .rs.scalar(id),
      name  = .rs.scalar(name),
      title = .rs.scalar(title)
   )
})

# NOTE: synchronize the structure of this object with
# the JSO defined in 'ObjectExplorerInspectionResult.java'
.rs.addFunction("explorer.createInspectionResult", function(object,
                                                            context = NULL,
                                                            children = NULL)
{
   # because 'object' can be missing, and attempts to directly
   # evaluate missing objects will fail, we use a silly bit of indirection
   # whenever introspecting that object
   . <- environment()
   
   # extract pertinent values from context
   name      <- context$name
   access    <- context$access
   tags      <- context$tags
   recursive <- context$recursive
   more      <- isTRUE(context$more)
   
   # if we did a recursive lookup, but children is still NULL,
   # set it as an empty list
   if (recursive && is.null(children))
      children <- list()
   
   # determine whether this is an S4 object
   s4 <- isS4(.$object)
   
   # figure out whether this object is expandable
   # (note that the client will still need to refine behavior
   # depending on whether attributes are being shown)
   n <- length(.$object)
   expandable <-
      
      
      # is this an R list / environment with children?
      (is.recursive(.$object) && !is.primitive(.$object) && n > 0) ||
      
      # is this an S4 object with one or more slots?
      (s4 && length(.rs.slotNames(.$object)) > 0) ||
      
      # is this a named atomic vector?
      (is.atomic(.$object) && !is.null(names(.$object)) && n > 0) ||
      
      # do we have relevant attributes?
      .rs.explorer.hasRelevantAttributes(.$object)
   
   # extract attributes when relevant
   attributes <- NULL
   if (context$recursive && .rs.explorer.hasRelevantAttributes(.$object))
   {
      childName <- "(attributes)"
      childAccess <- "attributes(#)"
      childTags <- c(.rs.explorer.tags$ATTRIBUTES, .rs.explorer.tags$VIRTUAL)
      childContext <- .rs.explorer.createChildContext(context,
                                                      childName,
                                                      childAccess,
                                                      childTags)
      childResult <- .rs.explorer.inspectObject(attributes(.$object), childContext)
      attributes <- childResult
   }
   
   # elements dictating how this should be displayed in UI
   display <- list(
      name = .rs.scalar(name),
      type = .rs.scalar(.rs.explorer.objectType(.$object)),
      desc = .rs.scalar(.rs.explorer.objectDesc(.$object))
   )
   
   # create inspection result
   list(
      address    = .rs.scalar(.rs.objectAddress(.$object)),
      type       = .rs.scalar(typeof(.$object)),
      class      = class(.$object),
      length     = .rs.scalar(length(.$object)),
      access     = .rs.scalar(access),
      recursive  = .rs.scalar(is.recursive(.$object)),
      expandable = .rs.scalar(expandable),
      atomic     = .rs.scalar(is.atomic(.$object)),
      named      = .rs.scalar(!is.null(names(.$object))),
      s4         = .rs.scalar(s4),
      tags       = as.character(tags),
      display    = display,
      attributes = attributes,
      children   = if (is.list(children)) unname(children),
      more       = .rs.scalar(more)
   )
})

.rs.addFunction("explorer.isValidInspectionResult", function(result)
{
   if (!is.list(result))
      return(FALSE)
   
   expected <- .rs.explorer.createInspectionResult(NULL)
   keys <- names(expected)
   missing <- setdiff(keys, names(result))
   if (length(missing))
      return(FALSE)
   
   TRUE
})

.rs.addFunction("explorer.callCustomInspector", function(object, context)
{
   classes <- class(object)
   
   # find a custom inspector method in the registry
   method <- NULL
   for (class in classes) {
      candidate <- .rs.explorer.inspectorRegistry[[class]]
      if (is.function(candidate)) {
         method <- candidate
         break
      }
   }
   
   # bail if we failed to find anything relevant
   if (is.null(method))
      return(NULL)
   
   # give the user's inspection routine 1 second to produce
   # an inspection result (returns NULL if we were forced
   # to halt execution)
   result <- .rs.withTimeLimit(1, method(object, context))
   if (is.null(result))
      return(NULL)
   
   # ensure we copy relevant context fields
   special <- c("name", "access", "tags")
   for (field in special)
      if (is.null(result[[field]]))
         result[[field]] <- context[[field]]
   
   # ensure that this is a valid inspection result
   if (!.rs.explorer.isValidInspectionResult(result))
      return(NULL)
   
   result
})

.rs.addFunction("explorer.inspectObject", function(object,
                                                   context = .rs.explorer.createContext())
{
   # check for missingness (can occur when inspecting an object
   # recursively that contain keys which map to missing objects)
   if (missing(object) || identical(object, quote(expr = )))
      return(.rs.explorer.createInspectionResult(quote(expr = ), context))
   
   # check for a custom registered inspector for this object's class
   result <- .rs.explorer.callCustomInspector(object, context)
   if (!is.null(result))
      return(result)
   
   # default to internal inspectors
   if (inherits(object, "xml_node") && "xml2" %in% loadedNamespaces())
      .rs.explorer.inspectXmlNode(object, context)
   else if (is.list(object) || is.call(object) || is.expression(object))
      .rs.explorer.inspectList(object, context)
   else if (is.environment(object))
      .rs.explorer.inspectEnvironment(object, context)
   else if (isS4(object))
      .rs.explorer.inspectS4(object, context)
   else if (is.function(object))
      .rs.explorer.inspectFunction(object, context)
   else
      .rs.explorer.inspectDefault(object, context)
})

.rs.addFunction("explorer.inspectXmlNode", function(object,
                                                    context = .rs.explorer.createContext())
{
   children <- NULL
   if (context$recursive)
   {
      # examine xml children
      xmlChildren <- xml2::xml_children(object)
      xmlNames <- xml2::xml_name(xmlChildren)
      indices <- seq_along(xmlChildren)
      children <- lapply(indices, function(i)
      {
         name <- sprintf("<%s>", xmlNames[[i]])
         access <- sprintf("xml_child(#, %i)", i)
         tags <- character()
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         .rs.explorer.inspectObject(xmlChildren[[i]], childContext)
      })
      
      # examine xml attributes
      xmlAttributes <- xml2::xml_attrs(object)
      name <- "(xml attributes)"
      access <- "xml_attrs(#)"
      tags <- .rs.explorer.tags$VIRTUAL
      childContext <- .rs.explorer.createChildContext(context, name, access, tags)
      children[[length(children) + 1]] <-
         .rs.explorer.inspectObject(xmlAttributes, childContext)
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectList", function(object,
                                                 context = .rs.explorer.createContext())
{
   # list children if requested
   children <- NULL
   if (context$recursive)
   {
      names <- names(object)
      indices <- .rs.slice(seq_along(object), context$start, context$end)
      context$more <- length(object) > context$end
      
      # iterate over children and inspect
      children <- lapply(indices, function(i)
      {
         if (is.null(names) || !nzchar(names[[i]]))
         {
            name <- sprintf("[[%i]]", i)
            access <- sprintf("#[[%i]]", i)
            tags <- .rs.explorer.tags$VIRTUAL
         }
         else
         {
            name <- names[[i]]
            access <- sprintf("#[[\"%s\"]]", name)
            tags <- character()
         }
         
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         .rs.explorer.inspectObject(object[[i]], childContext)
      })
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectEnvironment", function(object,
                                                        context = .rs.explorer.createContext())
{
   # list children if requested
   children <- NULL
   if (context$recursive)
   {
      # retrieve keys
      allKeys <- ls(envir = object, all.names = TRUE)
      keys <- .rs.slice(allKeys, context$start, context$end)
      context$more <- length(allKeys) > context$end
      
      children <- lapply(keys, function(key)
      {
         # we need special handling for '...'
         if (inherits(object[[key]], "..."))
         {
            value <- eval(quote(pairlist(...)), envir = object)
            access <- sprintf("eval(quote(pairlist(...)), envir = #)")
         }
         else
         {
            value <- object[[key]]
            access <- sprintf("#[[\"%s\"]]", key)
         }
         
         name <- key
         tags <- character()
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         result <- .rs.explorer.inspectObject(value, childContext)
         result[order(names(result))]
      })
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectS4", function(object,
                                               context = .rs.explorer.createContext())
{
   # get child slots if applicable
   children <- NULL
   if (context$recursive)
   {
      slots <- .rs.slotNames(object)
      children <- lapply(slots, function(slot) {
         name <- slot
         access <- sprintf("#@%s", name)
         tags <- character()
         
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         value <- eval(call("@", object, slot))
         .rs.explorer.inspectObject(value, childContext)
      })
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectFunction", function(object,
                                                     context = .rs.explorer.createContext())
{
   # construct interesting pieces of function
   children <- NULL
   if (is.primitive(object))
   {
      children <- list()
   }
   else if (context$recursive)
   {
      parts <- list(
         formals = formals(object),
         body = body(object),
         environment = environment(object)
      )
      
      children <- .rs.enumerate(parts, function(key, value) {
         
         # construct child context
         name <- key
         access <- sprintf("%s(#)", name)
         tags <- .rs.explorer.tags$VIRTUAL
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         
         # inspect with context
         .rs.explorer.inspectObject(value, childContext)
      })
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectDefault", function(object,
                                                    context = .rs.explorer.createContext())
{
   # allow children when object is named
   children <- NULL
   if (context$recursive && !is.null(names(object)))
   {
      names <- names(object)
      indices <- .rs.slice(seq_along(object), context$start, context$end)
      context$more <- length(object) > context$end
      
      # iterate over children and inspect
      children <- lapply(indices, function(i)
      {
         if (is.null(names) || !nzchar(names[[i]]))
         {
            name <- sprintf("[[%i]]", i)
            access <- sprintf("#[[%i]]", i)
            tags <- .rs.explorer.tags$VIRTUAL
         }
         else
         {
            name <- names[[i]]
            access <- sprintf("#[[\"%s\"]]", name)
            tags <- character()
         }
         
         childContext <- .rs.explorer.createChildContext(context, name, access, tags)
         .rs.explorer.inspectObject(object[[i]], childContext)
      })
   }
   
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.objectName", function(object, default)
{
   if (inherits(object, "xml_node"))
      return(sprintf("<%s>", xml2::xml_name(object)))
   
   default
})

.rs.addFunction("explorer.objectType", function(object)
{
   # some specialized behavior for certain objects
   if (inherits(object, "factor"))
   {
      type <- "factor"
      classes <- setdiff(class(object), "factor")
      if (length(classes))
         type <- paste(type, sprintf("(%s)", paste(classes, collapse = ", ")))
      return(type)
   }
   else if (inherits(object, "formula"))
   {
      return("formula")
   }
   
   # ascertain the object type
   type <- .rs.objectType(object)
   
   # append class information when relevant
   class <- .rs.objectClass(object)
   if (is.null(class) || identical(type, class))
      return(type)
   
   # generate short description from classes
   desc <- NULL
   if (isS4(object))
   {
      package <- attr(class, "package")
      is_pkg_relevant <-
         !is.null(package) &&
         !identical(package, ".GlobalEnv")
      desc <- if (is_pkg_relevant)
         paste(package, class, sep = "::")
      else
         class
   }
   else if (inherits(object, "R6"))
   {
      class <- setdiff(class, "R6")
      desc <- paste("R6:", paste(class, collapse = ", "))
   }
   else
   {
      desc <- paste("S3:", paste(class, collapse = ", "))
   }
   
   # append description on to type
   if (!is.null(desc))
   {
      type <- sprintf("%s (%s)", type, desc)
   }
   
   type
})

.rs.addFunction("explorer.objectDesc", function(object)
{
   output <- ""
   more <- FALSE
   trailing <- " ..."
   n <- 6L
   
   if (is.primitive(object))
   {
      output <- paste(capture.output(print(object)), collapse = " ")
      output <- sub("function ", "function", output)
      more <- FALSE
   }
   else if (is.function(object))
   {
      # construct argument list
      fmls <- formals(object)
      
      pieces <- .rs.enumerate(fmls, function(key, value) {
         
         if (identical(value, quote(expr = )))
            return(key)
         
         value <- if (is.call(value))
            format(value)
         else if (is.symbol(value))
            as.character(value)
         else
            value
         
         paste(key, value, sep = " = ")
      })
      
      output <- sprintf(
         "function(%s) { ... }",
         paste(pieces, collapse = ", ")
      )
      more <- FALSE
   }
   else if (is.factor(object))
   {
      fmt <- "%s with %i %s: %s"
      header <- head(levels(object), n)
      collapse <- if (is.ordered(object)) " < " else ", "
      output <- sprintf(
         fmt,
         if (is.ordered(object)) "Ordered factor" else "Factor",
         length(levels(object)),
         if (length(levels(object)) == 1) "level" else "levels",
         paste(.rs.surround(header, with = "\""), collapse = collapse)
      )
      more <- length(levels(object)) > n
      trailing <- if (is.ordered(object)) " < ..." else ", ..."
   }
   else if (is.character(object))
   {
      header <- head(object, n)
      output <- paste(encodeString(header, quote = "'"), collapse = " ")
      more <- length(object) > n
   }
   else if (is.call(object))
   {
      output <- paste(format(object), collapse = " ")
      more <- FALSE
   }
   else if (is.symbol(object))
   {
      output <- if (identical(object, quote(expr = )))
         "<missing>"
      else
         .rs.surround(object, with = "`")
      more <- FALSE
   }
   else if (is.data.frame(object))
   {
      fmt <- "A %s with %s %s and %s %s"
      
      name <- if (inherits(object, "tbl"))
         "tibble"
      else if (inherits(object, "data.table"))
         "data.table"
      else
         "data.frame"
      
      output <- sprintf(
         fmt,
         name,
         nrow(object),
         if (nrow(object) == 1) "row" else "rows",
         ncol(object),
         if (ncol(object) == 1) "column" else "columns"
      )
      
      more <- FALSE
   }
   else if (is.pairlist(object))
   {
      output <- sprintf("Pairlist of length %i", length(object))
      more <- FALSE
   }
   else if (is.list(object))
   {
      output <- sprintf("List of length %i", length(object))
      more <- FALSE
   }
   else if (is.environment(object))
   {
      if (inherits(object, "R6"))
      {
         fmt <- "R6 object of %s %s"
         class <- setdiff(class(object), "R6")
         output <- sprintf(
            fmt,
            if (length(class) > 1) "classes" else "class",
            paste(class, collapse = ", ")
         )
         more <- FALSE
      }
      else
      {
         # NOTE: R prevents us from calling 'unclass' on environment
         # objects, so we need to do something a bit different here.
         # We also want to avoid 'print' dispatching to custom methods
         # to avoid evaluating arbitrary user code here
         oldClass <- class(object)
         tryCatch({
            class(object) <- "environment"
            output <- capture.output(base::print(object))[[1]]
            more <- FALSE
         }, error = identity)
         class(object) <- oldClass
      }
   }
   else if (is.double(object))
   {
      if (length(object) > 1)
      {
         header <- head(object, n)
         formatted <- format(header, digits = 3)
         output <- paste(formatted, collapse = " ")
         more <- length(object) > n
      }
      else
      {
         output <- format(object)
         more <- FALSE
      }
   }
   else if (is.atomic(object))
   {
      output <- paste(head(object, n), collapse = " ")
      more <- length(object) > n
   }
   else if (isS4(object))
   {
      output <- sprintf("S4 object of class %s", class(object))
      more <- FALSE
   }
   else if (.rs.isExternalPointer(object))
   {
      output <- capture.output(base::print.default(object))
      more <- FALSE
   }
   
   # guard against unexpected inputs
   if (length(output) == 0)
   {
      output <- ""
   }
   else if (is.na(output) || !is.character(output))
   {
      output <- "<NA>"
   }
   else if (more || nchar(output) > 80)
   {
      truncated <- substring(output, 1, 80)
      output <- paste(truncated, trailing, sep = "")
   }
   
   output
})

.rs.addFunction("explorer.objectSize", function(object)
{
   format(object.size(object), units = "auto")
})

# Utility Functions ----

.rs.addFunction("getRefCount", function(object)
{
   .Call("rs_getRefCount",
         as.name("object"),
         environment(),
         PACKAGE = "(embedding)")
})

.rs.addFunction("setRefCount", function(object, count)
{
   .Call("rs_setRefCount",
         as.name("object"),
         environment(),
         as.integer(count),
         PACKAGE = "(embedding)")
})
