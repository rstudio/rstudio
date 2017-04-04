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

# this environment holds custom inspectors that might be
# registered by client packages
.rs.setVar("explorer.inspectorRegistry", new.env(parent = emptyenv()))

.rs.addJsonRpcHandler("explorer_inspect_object", function(id, name, recursive)
{
   object <- .rs.explorer.getCachedObject(id)
   context <- .rs.explorer.createContext(name = name, recursive = recursive)
   .rs.explorer.inspectObject(object, context)
})

.rs.addFunction("objectAddress", function(object)
{
   .Call("rs_objectAddress", object)
})

.rs.addFunction("objectClass", function(object)
{
   .Call("rs_objectClass", object)
})

.rs.addFunction("explorer.getCache", function(envir = .rs.CachedDataEnv)
{
   # TODO: tie each cache to a session id, to make it easier
   # to clean up old inspection results
   if (!exists("explorer", envir = envir))
      envir[["explorer"]] <- new.env(parent = emptyenv())
   envir[["explorer"]]
})

.rs.addFunction("explorer.getCachedObject", function(id)
{
   cache <- .rs.explorer.getCache()
   cache[[id]]
})

.rs.addFunction("explorer.cacheObject", function(object,
                                                 id = .rs.createUUID())
{
   cache <- .rs.explorer.getCache()
   cache[[id]] <- object
   id
})

.rs.addFunction("explorer.clearCache", function()
{
   cache <- .rs.explorer.getCache()
   rm(list = ls(envir = cache), envir = cache)
   cache
})

#' @param name The binding name, typically used when this value is
#'   presented in UI.
#' @param recursive Whether children of this object should be
#'   inspected recursively (if applicable). Can either be a boolean
#'   argument, or a numeric argument indicating the maximum depth
#'   of the recursion.
.rs.addFunction("explorer.createContext", function(name = NULL,
                                                   recursive = FALSE)
{
   list(
      name = name,
      recursive = recursive
   )
})

.rs.addFunction("explorer.createChildContext", function(context,
                                                        name = NULL)
{
   childContext <- context
   
   # support recursion depth
   recursive <- context$recursive
   if (is.numeric(recursive) && recursive)
      childContext$recursive <- recursive - 1
   
   # attach name if provided
   if (!is.null(name))
      childContext[["name"]] <- name
   
   # return
   childContext
})

.rs.addFunction("explorer.fireEvent", function(type, data = list())
{
   .rs.enqueClientEvent("object_explorer_event", list(
      type = .rs.scalar(type),
      data = data
   ))
})

.rs.addFunction("explorer.inferNameFromCall", function(call)
{
   paste(deparse(call$object, width.cutoff = 500), collapse = " ")
})

.rs.addFunction("explorer.viewObject", function(object)
{
   # attempt to divine name from call
   call <- match.call()
   title <- .rs.explorer.inferNameFromCall(call)
   name <- if (inherits(object, "xml_node"))
      sprintf("<%s>", xml2::xml_name(object))
   else
      title
   
   # generate a handle for this object
   handle <- .rs.explorer.createHandle(object, name, title)
   
   # fire event to client
   .rs.explorer.fireEvent(.rs.explorer.types$NEW, handle)
})

.rs.addFunction("explorer.createHandle", function(object,
                                                  name,
                                                  title = name)
{
   # save in cached data environment
   id <- .rs.explorer.cacheObject(object)
   
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
   # extract pertinent values from context
   name <- context$name
   
   s4 <- isS4(object)
   expandable <- is.recursive(object) || s4
   
   # extract attributes when relevant
   if (context$recursive &&
       !is.null(attributes(object)) &&
       !identical(object, attributes(object)))
   {
      childContext <- .rs.explorer.createChildContext(context, "(attributes)")
      childResult <- .rs.explorer.inspectObject(attributes(object), childContext)
      children[[length(children) + 1]] <- childResult
   }
   
   # create inspection result
   list(
      id         = .rs.scalar(.rs.explorer.cacheObject(object)),
      address    = .rs.scalar(.rs.objectAddress(object)),
      name       = .rs.scalar(name),
      type       = .rs.scalar(.rs.explorer.objectType(object)),
      class      = class(object),
      desc       = .rs.scalar(.rs.explorer.objectDesc(object)),
      size       = .rs.scalar(.rs.explorer.objectSize(object)),
      length     = .rs.scalar(length(object)),
      recursive  = .rs.scalar(is.recursive(object)),
      expandable = .rs.scalar(expandable),
      s4         = .rs.scalar(isS4(object)),
      children   = if (is.list(children)) children
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
   
   if (is.null(result$name))
      result$name <- context$name
   
   # ensure that this is a valid inspection result
   if (!.rs.explorer.isValidInspectionResult(result))
      return(NULL)
   
   result
})

.rs.addFunction("explorer.inspectObject", function(object,
                                                   context = .rs.explorer.createContext())
{
   # check for a custom registered inspector for this object's class
   result <- .rs.explorer.callCustomInspector(object, context)
   if (!is.null(result))
      return(result)
   
   # default to internal inspectors
   if (inherits(object, "xml_node") && "xml2" %in% loadedNamespaces())
      .rs.explorer.inspectXmlNode(object, context)
   else if (is.list(object))
      .rs.explorer.inspectList(object, context)
   else if (is.environment(object))
      .rs.explorer.inspectEnvironment(object, context)
   else if (isS4(object))
      .rs.explorer.inspectS4(object, context)
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
         childContext <- .rs.explorer.createChildContext(context, name)
         .rs.explorer.inspectObject(xmlChildren[[i]], childContext)
      })
      
      # examine xml attributes
      xmlAttributes <- as.list(xml2::xml_attrs(object))
      childContext <- .rs.explorer.createChildContext(context, "(xml attributes)")
      children[[length(children) + 1]] <-
         .rs.explorer.inspectObject(xmlAttributes, childContext)
   }
   
   children <- unname(children)
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectList", function(object,
                                                 context = .rs.explorer.createContext())
{
   # list children if requested
   children <- NULL
   if (context$recursive)
   {
      # handle cases where list has no names
      names <- names(object)
      indices <- seq_along(object)
      if (is.null(names))
         names <- sprintf("[[%i]]", indices)
      
      # iterate over children and get information
      children <- lapply(indices, function(i)
      {
         childContext <- .rs.explorer.createChildContext(context, names[[i]])
         .rs.explorer.inspectObject(object[[i]], childContext)
      })
   }
   
   children <- unname(children)
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectEnvironment", function(object,
                                                        context = .rs.explorer.createContext())
{
   # list children if requested
   children <- NULL
   if (context$recursive)
   {
      children <- .rs.enumerate(object, function(key, value)
      {
         childContext <- .rs.explorer.createChildContext(context, key)
         result <- .rs.explorer.inspectObject(value, childContext)
         result[order(names(result))]
      })
   }
   
   children <- unname(children)
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectS4", function(object,
                                               context = .rs.explorer.createContext())
{
   # get child slots if applicable
   children <- NULL
   if (context$recursive)
   {
      slots <- methods::slotNames(object)
      children <- lapply(slots, function(slot) {
         childContext <- .rs.explorer.createChildContext(context, slot)
         value <- eval(call("@", object, slot))
         .rs.explorer.inspectObject(value, childContext)
      })
   }
   
   children <- unname(children)
   .rs.explorer.createInspectionResult(object, context, children)
})

.rs.addFunction("explorer.inspectDefault", function(object,
                                                    context = .rs.explorer.createContext())
{
   .rs.explorer.createInspectionResult(object, context)
})

.rs.addFunction("explorer.objectType", function(object)
{
   # ascertain the object type
   type <- typeof(object)
   
   # append class information when relevant
   class <- .rs.objectClass(object)
   if (is.null(class))
      return(type)
   
   # generate short description from classes
   desc <- NULL
   if (isS4(object))
   {
      package <- attr(class, "package")
      desc <- if (is.null(package))
         class
      else
         paste(package, class, sep = "::")
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
   n <- 6L
   
   if (is.character(object))
   {
      header <- head(object, n)
      output <- paste(.rs.surround(header, with = "\""), collapse = " ")
      more <- length(object) > n
   }
   else if (is.symbol(object))
   {
      output <- .rs.surround(object, with = "`")
      more <- FALSE
   }
   else if (is.atomic(object))
   {
      output <- paste(head(object, n), collapse = " ")
      more <- length(object) > n
   }
   
   else if (isS4(object))
   {
      captured <- capture.output(show(object))
      output <- captured[[1]]
      more <- FALSE
   }
   
   if (more || nchar(output) > 80)
   {
      truncated <- substring(output, 1, 80)
      output <- paste(truncated, "...")
   }
   
   output
})

.rs.addFunction("explorer.objectSize", function(object)
{
   format(object.size(object), units = "auto")
})
