#
# SessionAutomationRemoteObject.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received self program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# self program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. self program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("automation.initJsObject", function(object,
                                                    self,
                                                    objectId,
                                                    objectClass,
                                                    parentObjectId = NULL) 
{
   attr(object, "self")   <- self
   attr(object, "id")     <- objectId
   attr(object, "parent") <- parentObjectId
   
   class(object) <- objectClass
   object
})

.rs.addFunction("automation.wrapJsResponse", function(self, response, parentObjectId = NULL)
{
   if (is.list(response) && is.null(names(response)))
   {
      result <- lapply(
         response,
         .rs.automation.wrapJsResponse,
         self = self,
         parentObjectId = parentObjectId
      )
      
      return(result)
   }
   
   result <- .rs.nullCoalesce(response$result, response$object)
   if (identical(result$type, "function"))
      return(.rs.automation.wrapJsFunction(self, result$objectId, parentObjectId))
   else if (identical(result$type, "object"))
      return(.rs.automation.wrapJsObject(self, result$objectId, parentObjectId))
   else if (!is.null(result$value))
      return(result$value)
})

.rs.addFunction("automation.wrapJsFunction", function(self, objectId, parentObjectId)
{
   object <- function(...) {
      .rs.automation.invokeJsFunction(self, objectId, parentObjectId, list(...))
   }
   
   .rs.automation.initJsObject(
      object         = object,
      self           = self,
      objectId       = objectId,
      objectClass    = c("jsFunction", "jsObject"),
      parentObjectId = parentObjectId
   )
})

.rs.addFunction("automation.wrapJsObject", function(self, objectId, parentObjectId)
{
   .rs.automation.initJsObject(
      object         = objectId,
      self           = self,
      objectId       = objectId,
      objectClass    = "jsObject",
      parentObjectId = parentObjectId
   )
})


.rs.addFunction("automation.invokeJsFunction", function(self, objectId, parentObjectId, params)
{
   # Create function we'll invoke.
   jsFunc <- .rs.heredoc('
      function(context) {
         return this.apply(context, [].slice.call(arguments, 1));
      }
   ')
 
   # Initialize arguments array.
   arguments <- vector("list", length(params) + 1L)
   arguments[[1L]] <- list(objectId = parentObjectId)
   
   # Insert our parameters into the arguments array.
   for (i in seq_along(params))
   {
      param <- params[[i]]
      arguments[[i + 1L]] <- if (inherits(param, "jsObject"))
         list(objectId = attr(param, "id", exact = TRUE))
      else
         list(value = param)
   }
   
   # Invoke the function.
   response <- self$client$Runtime.callFunctionOn(
      functionDeclaration = jsFunc,
      objectId = objectId,
      arguments = arguments
   )
   
   .rs.automation.wrapJsResponse(self, response)
})

.rs.addFunction("automation.initRemoteMethods", function()
{
   registerS3method("format", "jsObject", function(x, ...) {
      self <- attr(x, "self", exact = TRUE)
      id <- attr(x, "id", exact = TRUE)
      valid <- .rs.automation.isClientValid(self$client)
      fmt <- if (valid) "%s <%s>" else "%s <%s> [detached]"
      sprintf(fmt, paste(class(x), collapse = "/"), id)
   })
   
   registerS3method("str", "jsObject", function(object, ...) {
      writeLines(format(object))
   })
   
   registerS3method("print", "jsObject", function(x, ...) {
      writeLines(format(x))
   })
   
   registerS3method(".DollarNames", "jsObject", function(x, pattern) {
      
      self <- attr(x, "self", exact = TRUE)
      if (!.rs.automation.isClientValid(self$client))
         return(NULL)
      
      callback <- .rs.heredoc('
         function() {
            var result = {};
            for (var key in this) {
               result[key] = typeof this[key];
            }
            return JSON.stringify(result);
         }
      ')
      
      objectId <- attr(x, "id", exact = TRUE)
      response <- self$js.call(objectId, callback)
      value <- .rs.fromJSON(response$result$value)
      value <- value[sort(names(value))]
      
      completions <- names(value)
      types <- unlist(value, use.names = FALSE)
      
      attr(completions, "types") <- ifelse(
         types == "function",
         .rs.acCompletionTypes$FUNCTION,
         .rs.acCompletionTypes$UNKNOWN
      )
      
      completions
   })
   
   registerS3method("$", "jsObject", function(x, name) {
      
      self <- attr(x, "self", exact = TRUE)
      if (!.rs.automation.isClientValid(self$client))
         return(NULL)
      
      objectId <- attr(x, "id", exact = TRUE)
      jsFunc <- sprintf("function() { return this[%s]; }", deparse(name))
      
      response <- self$client$Runtime.callFunctionOn(
         functionDeclaration = jsFunc,
         objectId = objectId
      )
      
      .rs.automation.wrapJsResponse(self, response, objectId)
      
   })
   
   registerS3method("[[", "jsObject", function(x, i, j, ..., drop = FALSE) {
      
      self <- attr(x, "self", exact = TRUE)
      if (!.rs.automation.isClientValid(self$client))
         return(NULL)
      
      objectId <- attr(x, "id", exact = TRUE)
      subsetIndex <- .rs.toJSON(i, unbox = TRUE)
      jsFunc <- sprintf("function() { return this[%s]; }", subsetIndex)
      
      response <- self$client$Runtime.callFunctionOn(
         functionDeclaration = jsFunc,
         objectId = objectId
      )
      
      .rs.automation.wrapJsResponse(self, response, objectId)
      
   })
   
   registerS3method("length", "jsObject", function(x) {
      
      self <- attr(x, "self", exact = TRUE)
      if (!.rs.automation.isClientValid(self$client))
         return(0L)
      
      objectId <- attr(x, "id", exact = TRUE)
      response <- self$client$Runtime.callFunctionOn(
         functionDeclaration = "function() { return this.length; }",
         objectId = objectId
      )
      
      .rs.automation.wrapJsResponse(self, response, objectId)
      
   })
   
   registerS3method("as.vector", "jsObject", function(x, mode = "any") {
      
      self <- attr(x, "self", exact = TRUE)
      objectId <- attr(x, "id", exact = TRUE)
      
      response <- self$client$Runtime.callFunctionOn(
         functionDeclaration = "function() { return JSON.stringify(this); }",
         objectId = objectId
      )
      
      .rs.fromJSON(response$result$value)
   })
   
})

if (interactive())
{
   .rs.automation.initRemoteMethods()
}
