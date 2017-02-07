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

.rs.addFunction("validateParams", function(obj, params, type, optional = FALSE) {
   for (param in params) {
      value <- obj[[param]]
      if (optional && is.null(value))
         next
      if (!inherits(value, type) || length(value) != 1)
         stop(param, " must be a single element of type '", type, "'", 
              call. = FALSE)
   }
})

.rs.addFunction("validateCharacterParams", function(params, optional = FALSE) {
   .rs.validateParams(params, names(params), "character", optional)
})

.rs.addFunction("validateConnection", function(connection) {
   .rs.validateParams(connection, 
       c("type", "host", "displayName", "connectCode"),
       "character")
   .rs.validateParams(connection, "icon", "character", optional = TRUE)
   .rs.validateParams(connection, 
       c("disconnectCode", "listTables", "listColumns", "previewTable"),
       "function")
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
      if (identical(connection$type, type) && 
          identical(connection$host, host)) {
         return(connection)
      }
   }
   # indicates no connection was found
   NULL
})

options(connectionObserver = list(
   connectionOpened = function(type, host, displayName, icon = NULL, 
                               connectCode, disconnectCode, listTables, 
                               listColumns, previewTable, actions = NULL) {

      # manufacture and validate object representing this connection
      connection <- list(
         type           = type,
         host           = host,
         displayName    = displayName,
         icon           = icon,
         connectCode    = connectCode,
         disconnectCode = disconnectCode,
         listTables     = listTables,
         listColumns    = listColumns,
         previewTable   = previewTable,
         actions        = actions
      )
      class(connection) <- "rstudioConnection"
      .rs.validateConnection(connection)

      # generate an internal key for this connection in the local cache 
      cacheKey <- paste(connection$type, connection$host, 
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
      connection$disconnectCode()
   else
      ""
})

.rs.addFunction("connectionListTables", function(type, host) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) 
      connection$listTables()
   else
      character()
})

.rs.addFunction("connectionListColumns", function(type, host, table) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection))
      listColumnsCode <- connection$listColumns(table)
   else
      NULL
})

.rs.addFunction("connectionPreviewTable", function(type, host, table, limit) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) {
      df <- connection$previewTable(table, limit)
      .rs.viewDataFrame(df, table, TRUE)
   }

   NULL
})

.rs.addFunction("connectionExecuteAction", function(type, host, action) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection) && action %in% names(connection$actions)) {
      connection$actions[[action]]$callback()
   }

   NULL
})

.rs.addFunction("connectionReadSnippets", function() {
   snippetsPath <- getOption("connections-path", "/etc/rstudio/connections/")
   snippetsFiles <- list()

   if (!is.null(getOption("connections-path")) && !dir.exists(snippetsPath)) {
      warning(
         "Path '", snippetsPath, "' does not exist. ",
         "Configure the connections-path option appropriately.")
   }
   
   if (!is.null(snippetsPath)) {
      snippetsFiles <- list.files(snippetsPath)
   }

   snippets <- lapply(snippetsFiles, function(file) {
      fullPath <- file.path(snippetsPath, file)
      paste(readLines(fullPath), collapse = "\n")
   })

   names(snippets) <- tools::file_path_sans_ext(snippetsFiles)

   snippets
})

.rs.addJsonRpcHandler("get_new_connection_context", function() {
   rawConnections <- .rs.fromJSON(.Call("rs_availableConnections"))

   snippets <- .rs.connectionReadSnippets()

   connectionList <- lapply(rawConnections, function(con) {
      ns <- asNamespace(con$package)

      connectionType <- if (nchar(con$shinyapp) == 0) "Snippet" else "Shiny"
      snippetFile <- file.path("rstudio", "connections", paste(con$name, ".R", sep = ""))
      snippet <- ""

      if (nchar(con$shinyapp) == 0) {
         snippetPath <- system.file(snippetFile, package = con$package)
         if (!file.exists(snippetPath)) {
            warning(
               "The file \"", con$name, ".R\" does not exist under \"rstudio/connections\" for ",
               "package \"", con$package , "\".")
         }
         else {
            snippet <- paste(readLines(snippetPath), collapse = "\n")
         }
      }
      else {
         if (!exists(con$shinyapp, envir = ns, mode="function")) {
            warning(
               "The function \"", con$shinyapp, "\" does not exist. ",
               "Check the ShinyApp DCF field in the ", con$package, " package.")
         }
      }

      list(
         package = .rs.scalar(con$package),
         name = .rs.scalar(con$name),
         type = .rs.scalar(connectionType),
         newConnection = paste(con$package, "::", .rs.scalar(con$shinyapp), "()", sep = ""),
         snippet = .rs.scalar(snippet),
         help = .rs.scalar(con$help)
      )
   })

   connectionList <- c(connectionList, lapply(names(snippets), function(snippetName) {
      snippet <- snippets[[snippetName]]

      list(
         package = .rs.scalar(NULL),
         name = .rs.scalar(snippetName),
         type = .rs.scalar("Snippet"),
         snippet = .rs.scalar(snippet),
         help = .rs.scalar(NULL)
      )
   }))

   if (.rs.isPackageInstalled("odbc") &&
       exists("list_drivers", envir = asNamespace("odbc"))) {
      listDrivers <- get("list_drivers", envir = asNamespace("odbc"))

      driversNoSnippet <- Filter(function(e) { !(e %in% names(snippets)) }, listDrivers()$name)


      connectionList <- c(connectionList, lapply(driversNoSnippet, function(driver) {
         snippet <- paste(
            "library(DBI)\n",
            "con <- dbConnect(odbc::odbc(), .connection_string = \"", 
            "Driver={", driver, "};${1:Parameters}\")",
            sep = "")

         list(
            package = .rs.scalar(NULL),
            name = .rs.scalar(driver),
            type = .rs.scalar("Snippet"),
            snippet = .rs.scalar(snippet),
            help = .rs.scalar(NULL)
         )
      }))
   }

   context <- list(
      connectionsList = unname(connectionList)
   )

   context
})

.rs.addFunction("embeddedViewer", function(url)
{
   .rs.enqueClientEvent("navigate_shiny_frame", list(
      "url" = .rs.scalar(url)
   ))
})

.rs.addJsonRpcHandler("launch_embedded_shiny_connection_ui", function(package, name)
{
   if (package == "sparklyr" & packageVersion("sparklyr") <= "0.5.1") {
      return(.rs.error(
         "sparklyr ", packageVersion("sparklyr"), " does not support this functionality. ",
         "Please upgrade to sparklyr 0.5.2 or newer."
      ))
   }

   connectionContext <- .rs.rpc.get_new_connection_context()$connectionsList
   connectionInfo <- Filter(function(e) e$package == package & e$name == name, connectionContext)

   if (length(connectionInfo) != 1) {
      return(.rs.error(
         "Connection for package ", package, " and name ", name, " is not registered"
      ))
   }

   connectionInfo <- connectionInfo[[1]]

   consoleCommand <- paste(
      "shiny::runGadget(",
      connectionInfo$newConnection,
      ", viewer = .rs.embeddedViewer)",
      sep = ""
   )

   .rs.enqueClientEvent("send_to_console", list(
      "code" = .rs.scalar(consoleCommand),
      "execute" = .rs.scalar(TRUE),
      "focus" = .rs.scalar(FALSE),
      "animate" = .rs.scalar(FALSE)
   ))

   .rs.success()
})


