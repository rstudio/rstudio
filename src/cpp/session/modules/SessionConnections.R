#
# SessionConnections.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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


# install the S4 classes which represent active connections 
setClass("rstudioConnectionAction", representation(
  icon = "character",
  callback = "function"
))

setClass("rstudioConnection", representation(
  type = "character",
  host = "character",
  displayName = "character",
  icon = "character",
  connectCode = "character",
  disconnectCode = "function",
  listTables = "function",
  listColumns = "function",
  previewTable = "function",
  actions = "list"
))

.rs.addFunction("validateCharacterParams", function(params, optional = FALSE) {
   paramNames <- names(params)
   for (param in paramNames) {
      value <- params[[param]]
      if (optional && is.null(value))
         next
      if (!is.character(value) || length(value) != 1)
         stop(param, " must be a single element character vector", call. = FALSE)
   }
})

# create an environment which will host the known active connections
assign(".rs.activeConnections", 
       value = new.env(parent = emptyenv()), 
       envir = .rs.toolsEnv())

# given a connection type and host, find a matching active connection, or NULL
# if no connection was found
.rs.addFunction("findActiveConnection", function(type, host) {
   connections <- ls(.rs.activeConnections)
   for (name in connections) {
      connection <- get(name, envir = .rs.activeConnections)
      if (identical(connection@type, type) && 
          identical(connection@host, host)) {
         return(connection)
      }
   }
   # indicates no connection was found
   NULL
})

options(connectionObserver = list(
   connectionOpened = function(connection) {
      # validate connection object
      if (!inherits(connection, "rstudioConnection"))
         stop("argument must be an object of class rstudioConnection")

      # generate an internal key for this connection in the local cache 
      cacheKey <- paste(connection@type, connection@host, 
                        .Call("rs_generateShortUuid"), 
                        sep = "_")
      assign(cacheKey, value = connection, envir = .rs.activeConnections)

      # serialize and generate client events
      invisible(.Call("rs_connectionOpened", connection))
   },
   connectionClosed = function(type, host, ...) {
      .rs.validateCharacterParams(list(type = type, host = host))
      invisible(.Call("rs_connectionClosed", type, host))
   },
   connectionUpdated = function(type, host, hint, ...) {
      .rs.validateCharacterParams(list(type = type, host = host, hint = hint))
      invisible(.Call("rs_connectionUpdated", type, host, hint))
   }
))


.rs.addFunction("getConnectionObjectName", function(finder, host) {
   finderFunc <- eval(parse(text = finder))
   finderFunc(globalenv(), host)
})

.rs.addFunction("getConnectionObject", function(type, host) {
   name <- .rs.getConnectionObjectName(type, host)
   get(name, envir = globalenv())
})

.rs.addFunction("getDisconnectCode", function(type, host) {
   connection <- .rs.findActiveConnection(type, host)
   if (!is.null(connection))
      connection@disconnectCode()
   else
      ""
})

.rs.addFunction("connectionListTables", function(type, host) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) 
      connection@listTables()
   else
      character()
})

.rs.addFunction("connectionListColumns", function(type, host, table) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection))
      listColumnsCode <- connection@listColumns(table)
   else
      NULL
})

.rs.addFunction("connectionPreviewTable", function(type, host, table, limit) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) {
      df <- connection@previewTable(table, limit)
      .rs.viewDataFrame(df, table, TRUE)
   }

   NULL
})

.rs.addJsonRpcHandler("get_new_connection_context", function() {
  context <- list()

  context
})

.rs.addFunction("launchEmbeddedShinyConnectionUI", function(package)
{
   shiny::runGadget(sparklyr::connection_spark_shinyapp(), viewer = function(url) {
      .rs.enqueClientEvent("navigate_shiny_frame", list(
         "url" = .rs.scalar(url)
      ))
   });

   NULL
})

.rs.addJsonRpcHandler("launch_embedded_shiny_connection_ui", function(package)
{
   if (package == "sparklyr" & packageVersion("sparklyr") < "0.5") {
      return(.rs.error(
         "sparklyr ", packageVersion("sparklyr"), " does not support this functionality. ",
         "Please upgrade to sparklyr 0.5.1 or newer."
      ))
   }

   consoleCommand <- quote(.rs.launchEmbeddedShinyConnectionUI(package = package))

   .rs.enqueClientEvent("send_to_console", list(
      "code" = .rs.scalar(deparse(consoleCommand)),
      "execute" = .rs.scalar(TRUE),
      "focus" = .rs.scalar(FALSE),
      "animate" = .rs.scalar(FALSE)
   ))

   .rs.success()
})

.rs.addFunction("updateNewConnectionDialog", function(code)
{
   .rs.enqueClientEvent("update_new_connection_dialog", list(
      "code" = .rs.scalar(code)
   ))

   NULL
})

