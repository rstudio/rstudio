#
# SessionConnections.R
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
       c("disconnect", "listObjects", "listColumns", "previewObject"),
       "function")
})

# create an environment which will host the known active connections
assign(".rs.activeConnections", 
       value = new.env(parent = emptyenv()), 
       envir = .rs.toolsEnv())

# given a connection type and host, find a matching active connection name, or
# NULL if no connection was found
.rs.addFunction("findConnectionName", function(type, host) {
   connections <- ls(.rs.activeConnections)
   for (name in connections) {
      connection <- get(name, envir = .rs.activeConnections)
      if (identical(connection$type, type) && 
          identical(connection$host, host)) {
         return(name)
      }
   }
   # indicates no connection was found
   NULL
})

# given a connection type and host, find an active connection object, or NULL if
# no connection was found
.rs.addFunction("findActiveConnection", function(type, host) {
   name <- .rs.findConnectionName(type, host)
   if (is.null(name))
      return(NULL)
   return(get(name, envir = .rs.activeConnections))
})

options(connectionObserver = list(
   connectionOpened = function(type, host, displayName, icon = NULL, 
                               connectCode, disconnect, listObjectTypes,
                               listObjects, listColumns, previewObject, 
                               connectionObject, actions = NULL) {

      # execute the object types function once to get the list of known 
      # object types; this is presumed to be static over the lifetime of the
      # connection
      if (!inherits(listObjectTypes, "function")) {
         stop("listObjectTypes must be a function returning a list of object types", 
              call. = FALSE)
      }

      # function to flatten the tree of object types for more convenient storage
      promote <- function(name, l) {
        if (length(l) == 0)
          return(list())
        if (is.null(l$contains)) {
          # plain data
          return(list(list(name = name,
                      icon = l$icon,
                      contains = "data")))
        } else {
          # subtypes
          return(unlist(append(list(list(list(
            name = name, 
            icon = l$icon, 
            contains = names(l$contains)))),
            lapply(names(l$contains), function(name) {
              promote(name, l$contains[[name]])
            })), recursive = FALSE))
        }
        return(list())
      }
      
      # apply tree flattener to provided object tree
      objectTree <- listObjectTypes()
      objectTypes <- lapply(names(objectTree), function(name) {
         promote(name, objectTree[[name]])
      })[[1]]
      
      # manufacture and validate object representing this connection
      connection <- list(
         type             = type,            # the type of the connection
         host             = host,            # the host being connected to
         displayName      = displayName,     # the name to display 
         icon             = icon,            # an icon representing the connection
         connectCode      = connectCode,     # code to (re)establish connection
         disconnect       = disconnect,      # function that disconnects
         objectTypes      = objectTypes,     # list of object types known 
         listObjects      = listObjects,     # list objects (all or in container)
         listColumns      = listColumns,     # list columns of a data object
         previewObject    = previewObject,   # preview an object
         actions          = actions,         # list of actions possible on conn
         connectionObject = connectionObject # raw connection object
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

      # clean up reference in environment
      name <- .rs.findConnectionName(type, host)
      if (!is.null(name))
         rm(list = name, envir = .rs.activeConnections)

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

.rs.addFunction("connectionDisconnect", function(type, host) {
   connection <- .rs.findActiveConnection(type, host)
   if (!is.null(connection))
      connection$disconnect()
})

.rs.addFunction("connectionListObjects", function(type, host, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) 
      connection$listObjects(...)
   else
      character()
})

.rs.addFunction("connectionListColumns", function(type, host, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection))
      listColumnsCode <- connection$listColumns(...)
   else
      NULL
})

.rs.addFunction("connectionPreviewObject", function(type, host, limit, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) {
      df <- connection$previewObject(limit, ...)

      # use the last element of the specifier to caption the frame
      args <- list(...)
      .rs.viewDataFrame(df, args[[length(args)]], TRUE)
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

.rs.addFunction("connectionSupportedPackages", function() {
   list(
      list(
         name = "ODBC",
         package = "odbc",
         version = "1.0.1.9000"
      ),
      list(
         name = "Spark",
         package = "sparklyr",
         version = "0.5.3-9004"
      )
   )
})

.rs.addJsonRpcHandler("get_new_connection_context", function() {
   rawConnections <- .rs.fromJSON(.Call("rs_availableConnections"))

   snippets <- .rs.connectionReadSnippets()

   connectionList <- list()

   # add snippets to connections list
   connectionList <- c(connectionList, lapply(names(snippets), function(snippetName) {
      tryCatch({
         snippet <- snippets[[snippetName]]

         list(
            package = .rs.scalar(NULL),
            version = .rs.scalar(NULL),
            name = .rs.scalar(snippetName),
            type = .rs.scalar("Snippet"),
            snippet = .rs.scalar(snippet),
            help = .rs.scalar(NULL),
            iconData = .rs.scalar(.Call("rs_connectionIcon", snippetName)),
            licensed = .rs.scalar(FALSE)
         )
      }, error = function(e) {
         warning(e$message)
         NULL
      })
   }))

   # add packages to connections list
   connectionList <- c(connectionList, lapply(rawConnections, function(con) {
      tryCatch({
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

         iconData <- if (nchar(con$icon) > 0) {
            iconPath <- system.file(con$icon, package = con$package)
            if (file.exists(iconPath)) {
               paste0("data:image/png;base64,", .rs.base64encodeFile(iconPath));
            }
         }
         else {
            .Call("rs_connectionIcon", con$name)
         }

         list(
            package = .rs.scalar(con$package),
            version = .rs.scalar(NULL),
            name = .rs.scalar(con$name),
            type = .rs.scalar(connectionType),
            newConnection = .rs.scalar(paste(con$package, "::", con$shinyapp, "()", sep = "")),
            snippet = .rs.scalar(snippet),
            help = .rs.scalar(con$help),
            iconData = .rs.scalar(iconData),
            licensed = .rs.scalar(FALSE)
         )
      }, error = function(e) {
         warning(e$message)
         NULL
      })
   }))

   # add ODBC DSNs to connections list
   if (.rs.isPackageInstalled("odbc")) {
      dataSources <- data.frame()

      tryCatch({
         if (exists("list_data_sources", envir = asNamespace("odbc"))) {
            listSources <- get("list_data_sources", envir = asNamespace("odbc"))
         }
         else {
            listSources <- get("odbcListDataSources", envir = asNamespace("odbc"))
         }
         dataSources <- listSources()
      }, error = function(e) warning(e$message))

      dataSourcesNoSnippet <- unique(Filter(function(e) { !(e %in% names(snippets)) }, dataSources$name))

      connectionList <- c(connectionList, lapply(dataSourcesNoSnippet, function(dataSourceName) {
         tryCatch({

            dataSource <- dataSources[dataSources$name == dataSourceName, ]

            snippet <- paste(
               "library(DBI)\n",
               "con <- dbConnect(odbc::odbc(), \"${1:Data Source Name=", 
               dataSource$name,
               "}\")",
               sep = "")

            iconData <- .Call("rs_connectionIcon", dataSource$name)
            if (nchar(iconData) == 0)
               iconData <- .Call("rs_connectionIcon", "ODBC")

            list(
               package = .rs.scalar(NULL),
               version = .rs.scalar(NULL),
               name = .rs.scalar(dataSource$name),
               type = .rs.scalar("Snippet"),
               snippet = .rs.scalar(snippet),
               help = .rs.scalar(NULL),
               iconData = .rs.scalar(iconData),
               licensed = .rs.scalar(FALSE)
            )
         }, error = function(e) {
            warning(e$message)
            NULL
         })
      }))
   }

   # add ODBC drivers to connections list
   if (.rs.isPackageInstalled("odbc")) {
      drivers <- data.frame()

      tryCatch({
         if (exists("list_drivers", envir = asNamespace("odbc"))) {
            listDrivers <- get("list_drivers", envir = asNamespace("odbc"))
         }
         else {
            listDrivers <- get("odbcListDrivers", envir = asNamespace("odbc"))
         }
         drivers <- listDrivers()
      }, error = function(e) warning(e$message))

      uniqueDrivers <- drivers[drivers$attribute == "Driver", ]

      driversNoSnippet <- Filter(function(e) { !(e %in% names(snippets)) }, uniqueDrivers$name)

      connectionList <- c(connectionList, lapply(driversNoSnippet, function(driver) {
         tryCatch({
            currentDriver <- drivers[drivers$attribute == "Driver" & drivers$name == driver, ]

            basePath <- sub(paste(tolower(driver), ".*$", sep = ""), "", currentDriver$value)
            snippetsFile <- file.path(
               basePath,
               tolower(driver),
               "snippets",
               paste(tolower(driver), ".R", sep = "")
            )
            
            if (file.exists(snippetsFile)) {
               snippet <- paste(readLines(snippetsFile), collapse = "\n")
            }
            else {
               snippet <- paste(
                  "library(DBI)\n",
                  "con <- dbConnect(odbc::odbc(), .connection_string = \"", 
                  "Driver={", driver, "};${1:Parameters}\")",
                  sep = "")
            }

            licenseFile <- file.path(dirname(currentDriver$value), "license.lock")

            iconData <- .Call("rs_connectionIcon", driver)
            if (nchar(iconData) == 0)
               iconData <- .Call("rs_connectionIcon", "ODBC")

            list(
               package = .rs.scalar(NULL),
               version = .rs.scalar(NULL),
               name = .rs.scalar(driver),
               type = .rs.scalar("Snippet"),
               snippet = .rs.scalar(snippet),
               help = .rs.scalar(NULL),
               iconData = .rs.scalar(iconData),
               licensed = .rs.scalar(file.exists(licenseFile))
            )
         }, error = function(e) {
            warning(e$message)
            NULL
         })
      }))
   }

   connectionList <- Filter(function(e) !is.null(e), connectionList)

   connectionNames <- sapply(connectionList, function(e) e$name)
   supportedNotInstsalled <- Filter(function(e) {
      !e$name %in% connectionNames
   }, .rs.connectionSupportedPackages())

   connectionList <- c(connectionList, lapply(supportedNotInstsalled, function(supportedPackage) {
      iconData <- .Call("rs_connectionIcon", supportedPackage$name)
      list(
         package = .rs.scalar(supportedPackage$package),
         version = .rs.scalar(supportedPackage$version),
         name = .rs.scalar(supportedPackage$name),
         type = .rs.scalar("Install"),
         newConnection = .rs.scalar(NULL),
         snippet = .rs.scalar(NULL),
         help = .rs.scalar(NULL),
         iconData = .rs.scalar(iconData),
         licensed = .rs.scalar(FALSE)
      )
   }))

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

.rs.addJsonRpcHandler("connection_test", function(code) {
   error <- ""

   oldConnectionObserver <- getOption("connectionObserver")
   on.exit(options(connectionObserver = oldConnectionObserver))

   disconnectCalls <- list()

   options(connectionObserver = list(
      connectionOpened = function(type, host, displayName, icon = NULL, 
                                  connectCode, disconnect, listObjectTypes,
                                  listObjects, listColumns, previewObject, 
                                  connectionObject, actions = NULL) {
         disconnectCalls <<- c(disconnectCalls, disconnect)
      },
      connectionClosed = function(type, host, ...) {

      },
      connectionUpdated = function(type, host, hint, ...) {
      }
   ))

   .envir <- .rs.getActiveFrame()
   tryCatch({
      eval(parse(text = code), envir = .envir)
   }, error = function(e) {
      error <<- e$message
   })

   lapply(disconnectCalls, function(e) e())

   .rs.scalar(error)
})
